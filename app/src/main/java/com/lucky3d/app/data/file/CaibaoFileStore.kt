package com.lucky3d.app.data.file

import android.graphics.BitmapFactory
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageBounds(
    val width: Int,
    val height: Int,
)

fun interface ImageBoundsReader {
    fun read(bytes: ByteArray): ImageBounds?
}

fun interface AtomicFileMover {
    fun move(source: File, target: File)
}

data class StagedCaibaoFile(
    val issue: String,
    val file: File,
    val finalFileName: String,
    val sha256: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
)

data class StoredCaibaoFile(
    val issue: String,
    val file: File,
    val fileName: String,
    val sha256: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
)

class CaibaoFileException(
    val failure: LiveContentFailure,
    cause: Throwable? = null,
) : Exception(failure.name, cause)

class CaibaoFileStore(
    private val rootDirectory: File,
    private val imageBoundsReader: ImageBoundsReader = BitmapFactoryImageBoundsReader,
    private val atomicFileMover: AtomicFileMover = JavaAtomicFileMover,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun stageAndValidate(
        issue: String,
        bytes: ByteArray,
        mimeType: String,
    ): StagedCaibaoFile = withContext(ioDispatcher) {
        if (!ISSUE_PATTERN.matches(issue)) fail(LiveContentFailure.INVALID_ISSUE)
        if (bytes.size > MAX_IMAGE_BYTES) fail(LiveContentFailure.IMAGE_TOO_LARGE)
        if (bytes.isEmpty() || mimeType !in SUPPORTED_MIME_TYPES || !signatureMatches(bytes, mimeType)) {
            fail(LiveContentFailure.INVALID_IMAGE)
        }
        val bounds = imageBoundsReader.read(bytes)
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            fail(LiveContentFailure.INVALID_IMAGE)
        }
        ensureRootDirectory()
        val sha256 = bytes.sha256()
        val extension = if (mimeType == JPEG_MIME_TYPE) "jpg" else "png"
        val finalFileName = "$issue-A11-${sha256.take(SHA_PREFIX_LENGTH)}.$extension"
        val temporary = File(rootDirectory, "$finalFileName.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.flush()
                output.fd.sync()
            }
        } catch (exception: Exception) {
            temporary.delete()
            fail(LiveContentFailure.FILE_IO, exception)
        }
        StagedCaibaoFile(
            issue = issue,
            file = temporary,
            finalFileName = finalFileName,
            sha256 = sha256,
            mimeType = mimeType,
            width = bounds.width,
            height = bounds.height,
        )
    }

    suspend fun commit(staged: StagedCaibaoFile): StoredCaibaoFile = withContext(ioDispatcher) {
        requireRootFile(staged.file)
        val target = safeFile(staged.finalFileName)
        try {
            atomicFileMover.move(staged.file, target)
        } catch (exception: Exception) {
            staged.file.delete()
            fail(LiveContentFailure.FILE_IO, exception)
        }
        StoredCaibaoFile(
            issue = staged.issue,
            file = target,
            fileName = target.name,
            sha256 = staged.sha256,
            mimeType = staged.mimeType,
            width = staged.width,
            height = staged.height,
        )
    }

    suspend fun rollback(stagedOrStored: File) = withContext(ioDispatcher) {
        requireRootFile(stagedOrStored)
        deleteFile(stagedOrStored)
    }

    suspend fun delete(fileName: String) = withContext(ioDispatcher) {
        deleteFile(safeFile(fileName))
    }

    suspend fun removeTemporaryAndOrphanFiles(referencedFileNames: Set<String>) =
        withContext(ioDispatcher) {
            if (!rootDirectory.exists()) return@withContext
            val safeReferences = referencedFileNames.filterTo(mutableSetOf()) { isSafeBasename(it) }
            val files = rootDirectory.listFiles() ?: fail(LiveContentFailure.FILE_IO)
            files
                .filter { file ->
                    file.isFile && (
                        file.name.endsWith(TEMP_SUFFIX) ||
                            (CACHE_FILE_PATTERN.matches(file.name) && file.name !in safeReferences)
                        )
                }
                .forEach(::deleteFile)
        }

    private fun ensureRootDirectory() {
        if (rootDirectory.exists() && !rootDirectory.isDirectory) {
            fail(LiveContentFailure.FILE_IO)
        }
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            fail(LiveContentFailure.FILE_IO)
        }
    }

    private fun safeFile(fileName: String): File {
        require(isSafeBasename(fileName)) { "Only a safe cache basename is accepted" }
        val file = File(rootDirectory, fileName)
        require(file.canonicalFile.parentFile == rootDirectory.canonicalFile) {
            "File must remain inside the cache root"
        }
        return file
    }

    private fun requireRootFile(file: File) {
        require(isSafeBasename(file.name) && file.canonicalFile.parentFile == rootDirectory.canonicalFile) {
            "File must be a safe direct child of the cache root"
        }
    }

    private fun deleteFile(file: File) {
        if (file.exists() && !file.delete()) fail(LiveContentFailure.FILE_IO)
    }

    private fun isSafeBasename(value: String): Boolean =
        value.isNotBlank() &&
            !File(value).isAbsolute &&
            !value.contains("..") &&
            !value.contains('/') &&
            !value.contains('\\') &&
            File(value).name == value

    private fun signatureMatches(bytes: ByteArray, mimeType: String): Boolean = when (mimeType) {
        JPEG_MIME_TYPE -> bytes.startsWith(JPEG_SIGNATURE)
        PNG_MIME_TYPE -> bytes.startsWith(PNG_SIGNATURE)
        else -> false
    }

    private fun fail(
        failure: LiveContentFailure,
        cause: Throwable? = null,
    ): Nothing = throw CaibaoFileException(failure, cause)

    companion object {
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private const val JPEG_MIME_TYPE = "image/jpeg"
        private const val PNG_MIME_TYPE = "image/png"
        private const val SHA_PREFIX_LENGTH = 12
        private const val TEMP_SUFFIX = ".tmp"
        private val SUPPORTED_MIME_TYPES = setOf(JPEG_MIME_TYPE, PNG_MIME_TYPE)
        private val ISSUE_PATTERN = Regex("""20\d{5}""")
        private val CACHE_FILE_PATTERN = Regex("""20\d{5}-A11-[0-9a-f]{12}\.(jpg|png)""")
        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
        )
    }
}

private object BitmapFactoryImageBoundsReader : ImageBoundsReader {
    override fun read(bytes: ByteArray): ImageBounds? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            ImageBounds(options.outWidth, options.outHeight)
        } else {
            null
        }
    }
}

private object JavaAtomicFileMover : AtomicFileMover {
    override fun move(source: File, target: File) {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
