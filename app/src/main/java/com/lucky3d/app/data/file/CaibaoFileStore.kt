package com.lucky3d.app.data.file

import android.graphics.BitmapFactory
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.CancellationException
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

fun interface ImageIntegrityValidator {
    fun isValid(bytes: ByteArray, mimeType: String): Boolean
}

fun interface ImagePixelValidator {
    fun isValid(bytes: ByteArray, bounds: ImageBounds): Boolean
}

fun interface AtomicFileMover {
    fun move(source: File, target: File)
}

fun interface CaibaoFileWriter {
    fun writeAndSync(target: File, bytes: ByteArray)
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

data class StagedCaibaoDeletion(
    val originalFileName: String,
    val tombstoneFile: File?,
)

class CaibaoFileException(
    val failure: LiveContentFailure,
    cause: Throwable? = null,
) : Exception(failure.name, cause)

class CaibaoFileStore(
    private val rootDirectory: File,
    private val imageBoundsReader: ImageBoundsReader = BitmapFactoryImageBoundsReader,
    private val imageIntegrityValidator: ImageIntegrityValidator = DefaultImageIntegrityValidator,
    private val imagePixelValidator: ImagePixelValidator = BitmapFactoryImagePixelValidator,
    private val atomicFileMover: AtomicFileMover = JavaAtomicFileMover,
    private val caibaoFileWriter: CaibaoFileWriter = FileOutputStreamCaibaoFileWriter,
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
        if (!imageIntegrityValidator.isValid(bytes, mimeType)) fail(LiveContentFailure.INVALID_IMAGE)
        val bounds = imageBoundsReader.read(bytes)
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            fail(LiveContentFailure.INVALID_IMAGE)
        }
        if (!imagePixelValidator.isValid(bytes, bounds)) fail(LiveContentFailure.INVALID_IMAGE)
        ensureRootDirectory()
        val sha256 = bytes.sha256()
        val extension = if (mimeType == JPEG_MIME_TYPE) "jpg" else "png"
        val finalFileName = "$issue-A11-${sha256.take(SHA_PREFIX_LENGTH)}.$extension"
        val temporary = File(rootDirectory, "$finalFileName.${UUID.randomUUID()}.tmp")
        try {
            caibaoFileWriter.writeAndSync(temporary, bytes)
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                temporary.delete()
                throw exception
            }
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
            exception.rethrowCancellation()
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

    suspend fun readValidated(
        fileName: String,
        expectedSha256: String,
        expectedMimeType: String,
        expectedWidth: Int,
        expectedHeight: Int,
    ): ByteArray = withContext(ioDispatcher) {
        val file = safeFile(fileName)
        if (!file.exists() || !file.isFile) fail(LiveContentFailure.FILE_IO)
        if (file.length() <= 0L || file.length() > MAX_IMAGE_BYTES) {
            fail(LiveContentFailure.INVALID_IMAGE)
        }
        val bytes = try {
            file.readBytes()
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            fail(LiveContentFailure.FILE_IO, exception)
        }
        val bounds = imageBoundsReader.read(bytes)
        if (
            bytes.sha256() != expectedSha256.lowercase() ||
            expectedMimeType !in SUPPORTED_MIME_TYPES ||
            !signatureMatches(bytes, expectedMimeType) ||
            !imageIntegrityValidator.isValid(bytes, expectedMimeType) ||
            bounds?.width != expectedWidth ||
            bounds.height != expectedHeight ||
            !imagePixelValidator.isValid(bytes, bounds)
        ) {
            fail(LiveContentFailure.INVALID_IMAGE)
        }
        bytes
    }

    suspend fun rollback(stagedOrStored: File) = withContext(ioDispatcher) {
        requireRootFile(stagedOrStored)
        deleteFile(stagedOrStored)
    }

    suspend fun delete(fileName: String) = withContext(ioDispatcher) {
        deleteFile(safeFile(fileName))
    }

    suspend fun stageDelete(fileName: String): StagedCaibaoDeletion = withContext(ioDispatcher) {
        val original = safeFile(fileName)
        if (!original.exists()) {
            return@withContext StagedCaibaoDeletion(fileName, tombstoneFile = null)
        }
        if (!original.isFile) fail(LiveContentFailure.FILE_IO)
        val tombstone = safeFile("$fileName.delete.${UUID.randomUUID()}$TEMP_SUFFIX")
        try {
            atomicFileMover.move(original, tombstone)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            fail(LiveContentFailure.FILE_IO, exception)
        }
        StagedCaibaoDeletion(fileName, tombstone)
    }

    suspend fun commitDelete(staged: StagedCaibaoDeletion) = withContext(ioDispatcher) {
        staged.tombstoneFile?.let {
            requireRootFile(it)
            deleteFile(it)
        }
    }

    suspend fun rollbackDelete(staged: StagedCaibaoDeletion) = withContext(ioDispatcher) {
        val tombstone = staged.tombstoneFile ?: return@withContext
        requireRootFile(tombstone)
        if (!tombstone.exists()) return@withContext
        val original = safeFile(staged.originalFileName)
        try {
            atomicFileMover.move(tombstone, original)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            fail(LiveContentFailure.FILE_IO, exception)
        }
    }

    suspend fun removeTemporaryAndOrphanFiles(referencedFileNames: Set<String>) =
        withContext(ioDispatcher) {
            if (!rootDirectory.exists()) return@withContext
            val safeReferences = referencedFileNames.filterTo(mutableSetOf()) { isSafeBasename(it) }
            val files = rootDirectory.listFiles() ?: fail(LiveContentFailure.FILE_IO)
            files.filter(File::isFile).forEach { file ->
                val tombstoneOriginalName =
                    TOMBSTONE_FILE_PATTERN.matchEntire(file.name)?.groupValues?.get(1)
                when {
                    tombstoneOriginalName != null -> {
                        val original = safeFile(tombstoneOriginalName)
                        if (tombstoneOriginalName in safeReferences && !original.exists()) {
                            try {
                                atomicFileMover.move(file, original)
                            } catch (exception: Exception) {
                                exception.rethrowCancellation()
                                fail(LiveContentFailure.FILE_IO, exception)
                            }
                        } else {
                            deleteFile(file)
                        }
                    }

                    file.name.endsWith(TEMP_SUFFIX) -> deleteFile(file)
                    CACHE_FILE_PATTERN.matches(file.name) && file.name !in safeReferences ->
                        deleteFile(file)
                }
            }
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
        private val TOMBSTONE_FILE_PATTERN = Regex(
            """(20\d{5}-A11-[0-9a-f]{12}\.(?:jpg|png))""" +
                """\.delete\.[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}""" +
                """-[89ab][0-9a-f]{3}-[0-9a-f]{12}\.tmp""",
        )
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

object DefaultImageIntegrityValidator : ImageIntegrityValidator {
    override fun isValid(
        bytes: ByteArray,
        mimeType: String,
    ): Boolean = when (mimeType) {
        JPEG_MIME_TYPE -> isValidJpeg(bytes)
        PNG_MIME_TYPE -> isValidPng(bytes)
        else -> false
    }

    private fun isValidPng(bytes: ByteArray): Boolean {
        if (!bytes.startsWith(PNG_SIGNATURE)) return false
        var offset = PNG_SIGNATURE.size
        var chunkIndex = 0
        var hasIhdr = false
        var hasIdat = false
        var width = 0
        var height = 0
        var bitDepth = 0
        var colorType = 0
        var interlace = 0
        val compressedPixels = ByteArrayOutputStream()
        while (offset < bytes.size) {
            if (bytes.size - offset < PNG_CHUNK_OVERHEAD_BYTES) return false
            val dataLength = bytes.readUnsignedInt(offset)
            val chunkEnd = offset.toLong() + PNG_CHUNK_OVERHEAD_BYTES + dataLength
            if (chunkEnd > bytes.size.toLong()) return false

            val typeOffset = offset + PNG_LENGTH_BYTES
            val dataOffset = typeOffset + PNG_TYPE_BYTES
            val crcOffset = dataOffset + dataLength.toInt()
            val expectedCrc = bytes.readUnsignedInt(crcOffset)
            val actualCrc = CRC32().apply {
                update(bytes, typeOffset, PNG_TYPE_BYTES + dataLength.toInt())
            }.value
            if (expectedCrc != actualCrc) return false

            val isIhdr = bytes.matchesAscii(typeOffset, "IHDR")
            val isPlte = bytes.matchesAscii(typeOffset, "PLTE")
            val isIdat = bytes.matchesAscii(typeOffset, "IDAT")
            val isIend = bytes.matchesAscii(typeOffset, "IEND")
            val isUnknownCritical =
                bytes[typeOffset].unsigned() and PNG_ANCILLARY_BIT == 0 &&
                    !isIhdr &&
                    !isPlte &&
                    !isIdat &&
                    !isIend
            if (chunkIndex == 0 && !isIhdr) return false
            if (isUnknownCritical) return false
            when {
                isIhdr -> {
                    if (hasIhdr || chunkIndex != 0 || dataLength != PNG_IHDR_DATA_BYTES.toLong()) {
                        return false
                    }
                    val parsedWidth = bytes.readUnsignedInt(dataOffset)
                    val parsedHeight = bytes.readUnsignedInt(dataOffset + PNG_DIMENSION_BYTES)
                    if (
                        parsedWidth == 0L ||
                        parsedHeight == 0L ||
                        parsedWidth > Int.MAX_VALUE ||
                        parsedHeight > Int.MAX_VALUE
                    ) {
                        return false
                    }
                    width = parsedWidth.toInt()
                    height = parsedHeight.toInt()
                    bitDepth = bytes[dataOffset + PNG_BIT_DEPTH_OFFSET].unsigned()
                    colorType = bytes[dataOffset + PNG_COLOR_TYPE_OFFSET].unsigned()
                    val compression = bytes[dataOffset + PNG_COMPRESSION_OFFSET].unsigned()
                    val filter = bytes[dataOffset + PNG_FILTER_OFFSET].unsigned()
                    interlace = bytes[dataOffset + PNG_INTERLACE_OFFSET].unsigned()
                    if (
                        !validPngBitDepth(bitDepth, colorType) ||
                        compression != 0 ||
                        filter != 0 ||
                        interlace !in 0..1
                    ) {
                        return false
                    }
                    hasIhdr = true
                }

                isIdat -> {
                    if (!hasIhdr) return false
                    compressedPixels.write(bytes, dataOffset, dataLength.toInt())
                    hasIdat = true
                }

                isIend -> {
                    return hasIhdr &&
                        hasIdat &&
                        dataLength == 0L &&
                        chunkEnd == bytes.size.toLong() &&
                        validPngPixelStream(
                            compressed = compressedPixels.toByteArray(),
                            width = width,
                            height = height,
                            bitDepth = bitDepth,
                            colorType = colorType,
                            interlace = interlace,
                        )
                }
            }
            offset = chunkEnd.toInt()
            chunkIndex += 1
        }
        return false
    }

    private fun validPngBitDepth(bitDepth: Int, colorType: Int): Boolean = when (colorType) {
        0 -> bitDepth in setOf(1, 2, 4, 8, 16)
        2 -> bitDepth == 8 || bitDepth == 16
        3 -> bitDepth in setOf(1, 2, 4, 8)
        4, 6 -> bitDepth == 8 || bitDepth == 16
        else -> false
    }

    private fun validPngPixelStream(
        compressed: ByteArray,
        width: Int,
        height: Int,
        bitDepth: Int,
        colorType: Int,
        interlace: Int,
    ): Boolean {
        val samplesPerPixel = when (colorType) {
            0, 3 -> 1
            2 -> 3
            4 -> 2
            6 -> 4
            else -> return false
        }
        val bitsPerPixel = bitDepth * samplesPerPixel
        val passes = if (interlace == 0) {
            listOf(PngPass(0, 0, 1, 1))
        } else {
            ADAM7_PASSES
        }
        var expectedBytes = 0L
        passes.forEach { pass ->
            val passWidth = passSize(width, pass.xStart, pass.xStep)
            val passHeight = passSize(height, pass.yStart, pass.yStep)
            if (passWidth > 0 && passHeight > 0) {
                val rowBytes = (passWidth.toLong() * bitsPerPixel + 7L) / 8L
                expectedBytes += passHeight.toLong() * (rowBytes + PNG_FILTER_BYTE)
                if (expectedBytes > MAX_PNG_DECOMPRESSED_BYTES) return false
            }
        }
        val pixels = inflateExactly(compressed, expectedBytes.toInt()) ?: return false
        var pixelOffset = 0
        passes.forEach { pass ->
            val passWidth = passSize(width, pass.xStart, pass.xStep)
            val passHeight = passSize(height, pass.yStart, pass.yStep)
            if (passWidth > 0 && passHeight > 0) {
                val rowBytes = ((passWidth.toLong() * bitsPerPixel + 7L) / 8L).toInt()
                repeat(passHeight) {
                    if (pixels[pixelOffset].unsigned() !in PNG_FILTER_TYPES) return false
                    pixelOffset += PNG_FILTER_BYTE + rowBytes
                }
            }
        }
        return pixelOffset == pixels.size
    }

    private fun inflateExactly(compressed: ByteArray, expectedBytes: Int): ByteArray? {
        val inflated = ByteArray(expectedBytes)
        return try {
            InflaterInputStream(ByteArrayInputStream(compressed)).use { input ->
                var offset = 0
                while (offset < inflated.size) {
                    val read = input.read(inflated, offset, inflated.size - offset)
                    if (read < 0) return null
                    offset += read
                }
                if (input.read() != -1) return null
            }
            inflated
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun passSize(fullSize: Int, start: Int, step: Int): Int =
        if (fullSize <= start) 0 else (fullSize - start + step - 1) / step

    private data class PngPass(
        val xStart: Int,
        val yStart: Int,
        val xStep: Int,
        val yStep: Int,
    )

    private fun isValidJpeg(bytes: ByteArray): Boolean {
        if (bytes.size < JPEG_MINIMUM_BYTES ||
            bytes[0].unsigned() != JPEG_MARKER_PREFIX ||
            bytes[1].unsigned() != JPEG_SOI
        ) {
            return false
        }
        var offset = JPEG_SOI_BYTES
        var inEntropyData = false
        var hasSof = false
        var hasSos = false
        while (offset < bytes.size) {
            val marker: Int
            if (inEntropyData) {
                while (offset < bytes.size && bytes[offset].unsigned() != JPEG_MARKER_PREFIX) {
                    offset += 1
                }
                if (offset >= bytes.size) return false
                while (offset < bytes.size && bytes[offset].unsigned() == JPEG_MARKER_PREFIX) {
                    offset += 1
                }
                if (offset >= bytes.size) return false
                marker = bytes[offset].unsigned()
                offset += 1
                when {
                    marker == JPEG_STUFFED_BYTE -> continue
                    marker in JPEG_RESTART_MARKERS -> continue
                    else -> inEntropyData = false
                }
            } else {
                if (bytes[offset].unsigned() != JPEG_MARKER_PREFIX) return false
                while (offset < bytes.size && bytes[offset].unsigned() == JPEG_MARKER_PREFIX) {
                    offset += 1
                }
                if (offset >= bytes.size) return false
                marker = bytes[offset].unsigned()
                offset += 1
                if (marker == JPEG_STUFFED_BYTE) return false
            }

            when {
                marker == JPEG_EOI -> return hasSof && hasSos && offset == bytes.size
                marker == JPEG_SOI -> return false
                marker == JPEG_TEMPORARY || marker in JPEG_RESTART_MARKERS -> continue
            }

            if (bytes.size - offset < JPEG_SEGMENT_LENGTH_BYTES) return false
            val segmentLength = bytes.readUnsignedShort(offset)
            if (segmentLength < JPEG_SEGMENT_LENGTH_BYTES) return false
            val segmentEnd = offset.toLong() + segmentLength.toLong()
            if (segmentEnd > bytes.size.toLong()) return false

            when {
                marker in JPEG_SOF_MARKERS -> {
                    if (segmentLength < JPEG_MINIMUM_SOF_LENGTH) return false
                    val height = bytes.readUnsignedShort(offset + JPEG_SOF_HEIGHT_OFFSET)
                    val width = bytes.readUnsignedShort(offset + JPEG_SOF_WIDTH_OFFSET)
                    val componentCount = bytes[offset + JPEG_SOF_COMPONENT_COUNT_OFFSET].unsigned()
                    val expectedLength =
                        JPEG_SOF_BASE_LENGTH + JPEG_SOF_COMPONENT_BYTES * componentCount
                    if (height == 0 ||
                        width == 0 ||
                        componentCount == 0 ||
                        segmentLength != expectedLength
                    ) {
                        return false
                    }
                    hasSof = true
                }

                marker == JPEG_SOS -> {
                    if (!hasSof || segmentLength < JPEG_MINIMUM_SOS_LENGTH) return false
                    val componentCount = bytes[offset + JPEG_SOS_COMPONENT_COUNT_OFFSET].unsigned()
                    val expectedLength =
                        JPEG_SOS_BASE_LENGTH + JPEG_SOS_COMPONENT_BYTES * componentCount
                    if (componentCount == 0 || segmentLength != expectedLength) return false
                    hasSos = true
                    inEntropyData = true
                }
            }
            offset = segmentEnd.toInt()
        }
        return false
    }

    private const val JPEG_MIME_TYPE = "image/jpeg"
    private const val PNG_MIME_TYPE = "image/png"
    private const val PNG_LENGTH_BYTES = 4
    private const val PNG_TYPE_BYTES = 4
    private const val PNG_DIMENSION_BYTES = 4
    private const val PNG_IHDR_DATA_BYTES = 13
    private const val PNG_CHUNK_OVERHEAD_BYTES = 12
    private const val PNG_BIT_DEPTH_OFFSET = 8
    private const val PNG_COLOR_TYPE_OFFSET = 9
    private const val PNG_COMPRESSION_OFFSET = 10
    private const val PNG_FILTER_OFFSET = 11
    private const val PNG_INTERLACE_OFFSET = 12
    private const val PNG_ANCILLARY_BIT = 0x20
    private const val PNG_FILTER_BYTE = 1
    private const val MAX_PNG_DECOMPRESSED_BYTES = 32 * 1024 * 1024L
    private const val JPEG_MARKER_PREFIX = 0xFF
    private const val JPEG_STUFFED_BYTE = 0x00
    private const val JPEG_TEMPORARY = 0x01
    private const val JPEG_SOI = 0xD8
    private const val JPEG_EOI = 0xD9
    private const val JPEG_SOS = 0xDA
    private const val JPEG_SOI_BYTES = 2
    private const val JPEG_MINIMUM_BYTES = 4
    private const val JPEG_SEGMENT_LENGTH_BYTES = 2
    private const val JPEG_MINIMUM_SOF_LENGTH = 11
    private const val JPEG_SOF_HEIGHT_OFFSET = 3
    private const val JPEG_SOF_WIDTH_OFFSET = 5
    private const val JPEG_SOF_COMPONENT_COUNT_OFFSET = 7
    private const val JPEG_SOF_BASE_LENGTH = 8
    private const val JPEG_SOF_COMPONENT_BYTES = 3
    private const val JPEG_MINIMUM_SOS_LENGTH = 8
    private const val JPEG_SOS_COMPONENT_COUNT_OFFSET = 2
    private const val JPEG_SOS_BASE_LENGTH = 6
    private const val JPEG_SOS_COMPONENT_BYTES = 2
    private val JPEG_RESTART_MARKERS = 0xD0..0xD7
    private val PNG_FILTER_TYPES = 0..4
    private val ADAM7_PASSES = listOf(
        PngPass(0, 0, 8, 8),
        PngPass(4, 0, 8, 8),
        PngPass(0, 4, 4, 8),
        PngPass(2, 0, 4, 4),
        PngPass(0, 2, 2, 4),
        PngPass(1, 0, 2, 2),
        PngPass(0, 1, 1, 2),
    )
    private val JPEG_SOF_MARKERS = setOf(
        0xC0,
        0xC1,
        0xC2,
        0xC3,
        0xC5,
        0xC6,
        0xC7,
        0xC9,
        0xCA,
        0xCB,
        0xCD,
        0xCE,
        0xCF,
    )
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

private object BitmapFactoryImagePixelValidator : ImagePixelValidator {
    override fun isValid(bytes: ByteArray, bounds: ImageBounds): Boolean {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds)
        }
        val bitmap = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: RuntimeException) {
            null
        } ?: return false
        bitmap.recycle()
        return true
    }

    private fun sampleSize(bounds: ImageBounds): Int {
        var sampleSize = 1
        while (
            bounds.width / sampleSize > MAX_PIXEL_DECODE_DIMENSION ||
            bounds.height / sampleSize > MAX_PIXEL_DECODE_DIMENSION
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private const val MAX_PIXEL_DECODE_DIMENSION = 2_048
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

private object FileOutputStreamCaibaoFileWriter : CaibaoFileWriter {
    override fun writeAndSync(target: File, bytes: ByteArray) {
        FileOutputStream(target).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
    }
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

private fun ByteArray.readUnsignedInt(offset: Int): Long =
    (this[offset].unsigned().toLong() shl 24) or
        (this[offset + 1].unsigned().toLong() shl 16) or
        (this[offset + 2].unsigned().toLong() shl 8) or
        this[offset + 3].unsigned().toLong()

private fun ByteArray.readUnsignedShort(offset: Int): Int =
    (this[offset].unsigned() shl 8) or this[offset + 1].unsigned()

private fun ByteArray.matchesAscii(
    offset: Int,
    value: String,
): Boolean = value.indices.all { index -> this[offset + index].unsigned() == value[index].code }

private fun Byte.unsigned(): Int = toInt() and 0xFF

private fun Exception.rethrowCancellation() {
    if (this is CancellationException) throw this
}
