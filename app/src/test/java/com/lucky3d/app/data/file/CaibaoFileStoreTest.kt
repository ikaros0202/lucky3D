package com.lucky3d.app.data.file

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class CaibaoFileStoreTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("caibao-file-store").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `jpeg and png are staged under injected root with safe deterministic names`() = runTest {
        val store = store(bounds = ImageBounds(640, 480))
        val jpeg = jpegBytes("jpeg-content")
        val png = pngBytes("png-content")

        val stagedJpeg = store.stageAndValidate("2026201", jpeg, "image/jpeg")
        val stagedPng = store.stageAndValidate("2026202", png, "image/png")

        assertThat(stagedJpeg.file.parentFile!!.canonicalFile).isEqualTo(root.canonicalFile)
        assertThat(stagedJpeg.file.name).endsWith(".tmp")
        assertThat(stagedJpeg.finalFileName)
            .isEqualTo("2026201-A11-${sha256(jpeg).take(12)}.jpg")
        assertThat(stagedPng.finalFileName)
            .isEqualTo("2026202-A11-${sha256(png).take(12)}.png")
        assertThat(stagedJpeg.width).isEqualTo(640)
        assertThat(stagedJpeg.height).isEqualTo(480)
        assertThat(stagedJpeg.sha256).isEqualTo(sha256(jpeg))
    }

    @Test
    fun `invalid issue mime empty signature mismatch and failed bounds are typed validation failures`() = runTest {
        assertFailure(LiveContentFailure.INVALID_ISSUE) {
            store().stageAndValidate("../2026201", jpegBytes(), "image/jpeg")
        }
        assertFailure(LiveContentFailure.INVALID_IMAGE) {
            store().stageAndValidate("2026201", jpegBytes(), "image/gif")
        }
        assertFailure(LiveContentFailure.INVALID_IMAGE) {
            store().stageAndValidate("2026201", byteArrayOf(), "image/jpeg")
        }
        assertFailure(LiveContentFailure.INVALID_IMAGE) {
            store().stageAndValidate("2026201", pngBytes(), "image/jpeg")
        }
        assertFailure(LiveContentFailure.INVALID_IMAGE) {
            store(bounds = null).stageAndValidate("2026201", jpegBytes(), "image/jpeg")
        }
        assertFailure(LiveContentFailure.INVALID_IMAGE) {
            store(bounds = ImageBounds(0, 480))
                .stageAndValidate("2026201", jpegBytes(), "image/jpeg")
        }
        assertThat(root.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `default store integrity accepts generated valid png and jpeg`() = runTest {
        val store = CaibaoFileStore(
            rootDirectory = root,
            imageBoundsReader = ImageBoundsReader { ImageBounds(1, 1) },
            ioDispatcher = Dispatchers.Unconfined,
        )

        val stagedPng = store.stageAndValidate("2026201", pngBytes(), "image/png")
        val stagedJpeg = store.stageAndValidate("2026202", jpegBytes(), "image/jpeg")

        assertThat(stagedPng.width).isEqualTo(1)
        assertThat(stagedPng.height).isEqualTo(1)
        assertThat(stagedJpeg.width).isEqualTo(1)
        assertThat(stagedJpeg.height).isEqualTo(1)
    }

    @Test
    fun `default store integrity rejects truncated corrupt and incomplete png before writing temp`() = runTest {
        val valid = pngBytes()
        val corruptCrc = valid.copyOf().also { bytes ->
            bytes[PNG_IHDR_CRC_OFFSET] = (bytes[PNG_IHDR_CRC_OFFSET].toInt() xor 0x01).toByte()
        }
        val invalidImages = listOf(
            valid.copyOf(valid.size - 1),
            valid.copyOf(valid.size / 2),
            corruptCrc,
            pngBytes(includeIdat = false),
            valid.copyOf(valid.size - PNG_IEND_CHUNK_SIZE),
        )

        invalidImages.forEach { bytes ->
            assertFailure(LiveContentFailure.INVALID_IMAGE) {
                store(bounds = ImageBounds(1, 1))
                    .stageAndValidate("2026201", bytes, "image/png")
            }
        }

        assertThat(root.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `default store integrity rejects truncated overflowing and trailing jpeg before writing temp`() = runTest {
        val valid = jpegBytes()
        val segmentLengthOverflow = valid.copyOf().also { bytes ->
            bytes[JPEG_FIRST_SEGMENT_LENGTH_OFFSET] = 0x7F
            bytes[JPEG_FIRST_SEGMENT_LENGTH_OFFSET + 1] = 0xFF.toByte()
        }
        val invalidImages = listOf(
            valid.copyOf(valid.size - 1),
            valid.copyOf(valid.size / 2),
            segmentLengthOverflow,
            valid.copyOf(valid.size - JPEG_EOI_SIZE),
            valid + 0,
        )

        invalidImages.forEach { bytes ->
            assertFailure(LiveContentFailure.INVALID_IMAGE) {
                store(bounds = ImageBounds(1, 1))
                    .stageAndValidate("2026201", bytes, "image/jpeg")
            }
        }

        assertThat(root.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `jpeg entropy accepts stuffed bytes restart markers and fill bytes`() = runTest {
        val valid = jpegBytes()
        val eoiOffset = valid.size - JPEG_EOI_SIZE
        val entropyMarkers = byteArrayOf(
            0x12,
            0xFF.toByte(),
            0x00,
            0x34,
            0xFF.toByte(),
            0xD0.toByte(),
            0x56,
            0xFF.toByte(),
            0xFF.toByte(),
            0xD1.toByte(),
        )
        val withEntropyMarkers =
            valid.copyOfRange(0, eoiOffset) +
                entropyMarkers +
                valid.copyOfRange(eoiOffset, valid.size)

        val staged = store(bounds = ImageBounds(1, 1))
            .stageAndValidate("2026201", withEntropyMarkers, "image/jpeg")

        assertThat(staged.file.exists()).isTrue()
    }

    @Test
    fun `eight MiB is accepted and one extra byte is rejected`() = runTest {
        val exact = paddedJpeg(8 * 1024 * 1024)
        val over = exact + 0

        val staged = store().stageAndValidate("2026201", exact, "image/jpeg")

        assertThat(staged.file.length()).isEqualTo(exact.size.toLong())
        assertFailure(LiveContentFailure.IMAGE_TOO_LARGE) {
            store().stageAndValidate("2026202", over, "image/jpeg")
        }
    }

    @Test
    fun `commit delegates one atomic move and returns the final file`() = runTest {
        val mover = RecordingAtomicMover()
        val store = store(mover = mover)
        val staged = store.stageAndValidate("2026201", jpegBytes(), "image/jpeg")

        val stored = store.commit(staged)

        assertThat(mover.moves).containsExactly(staged.file to stored.file)
        assertThat(stored.file.parentFile!!.canonicalFile).isEqualTo(root.canonicalFile)
        assertThat(stored.file.name).isEqualTo(staged.finalFileName)
        assertThat(stored.file.exists()).isTrue()
        assertThat(staged.file.exists()).isFalse()
    }

    @Test
    fun `failed atomic commit removes temporary file without creating a final file`() = runTest {
        val store = store(mover = AtomicFileMover { _, _ -> throw java.io.IOException("unsupported") })
        val staged = store.stageAndValidate("2026201", jpegBytes(), "image/jpeg")

        assertFailure(LiveContentFailure.FILE_IO) {
            store.commit(staged)
        }

        assertThat(staged.file.exists()).isFalse()
        assertThat(File(root, staged.finalFileName).exists()).isFalse()
    }

    @Test
    fun `stage writer cancellation propagates unchanged and removes its temporary file`() = runTest {
        val cancellation = CancellationException("sync cancelled")
        val store = store(
            writer = CaibaoFileWriter { temporary, bytes ->
                temporary.writeBytes(bytes)
                throw cancellation
            },
        )

        assertCancellation(cancellation) {
            store.stageAndValidate("2026201", jpegBytes(), "image/jpeg")
        }

        assertThat(root.listFiles().orEmpty().filter { it.name.endsWith(".tmp") }).isEmpty()
    }

    @Test
    fun `commit mover cancellation propagates unchanged without mapping it to file IO`() = runTest {
        val cancellation = CancellationException("commit cancelled")
        val staged = store().stageAndValidate("2026201", jpegBytes(), "image/jpeg")
        val store = store(mover = AtomicFileMover { _, _ -> throw cancellation })

        assertCancellation(cancellation) { store.commit(staged) }

        assertThat(staged.file.exists()).isTrue()
    }

    @Test
    fun `rollback deletes staged or stored files in the root`() = runTest {
        val store = store()
        val staged = store.stageAndValidate("2026201", jpegBytes(), "image/jpeg")
        store.rollback(staged.file)
        assertThat(staged.file.exists()).isFalse()

        val stored = store.commit(
            store.stageAndValidate("2026202", jpegBytes("other"), "image/jpeg"),
        )
        store.rollback(stored.file)
        assertThat(stored.file.exists()).isFalse()
    }

    @Test
    fun `validated read returns only the committed image matching stored metadata`() = runTest {
        val bytes = jpegBytes("cached-reader")
        val store = store(bounds = ImageBounds(720, 1280))
        val stored = store.commit(
            store.stageAndValidate("2026201", bytes, "image/jpeg"),
        )

        val loaded = store.readValidated(
            fileName = stored.fileName,
            expectedSha256 = stored.sha256,
            expectedMimeType = stored.mimeType,
            expectedWidth = stored.width,
            expectedHeight = stored.height,
        )

        assertThat(loaded).isEqualTo(bytes)
    }

    @Test
    fun `validated read rejects missing unsafe or metadata mismatched cache files`() = runTest {
        val bytes = pngBytes("cached-reader")
        val store = store(bounds = ImageBounds(720, 1280))
        val stored = store.commit(
            store.stageAndValidate("2026201", bytes, "image/png"),
        )

        assertFailure(LiveContentFailure.INVALID_IMAGE) {
            store.readValidated(
                fileName = stored.fileName,
                expectedSha256 = "0".repeat(64),
                expectedMimeType = stored.mimeType,
                expectedWidth = stored.width,
                expectedHeight = stored.height,
            )
        }
        assertFailure(LiveContentFailure.FILE_IO) {
            store.readValidated(
                fileName = "2026202-A11-0123456789ab.png",
                expectedSha256 = sha256(bytes),
                expectedMimeType = "image/png",
                expectedWidth = 720,
                expectedHeight = 1280,
            )
        }
        assertThrowsIllegalArgument {
            store.readValidated(
                fileName = "../outside.png",
                expectedSha256 = sha256(bytes),
                expectedMimeType = "image/png",
                expectedWidth = 720,
                expectedHeight = 1280,
            )
        }
    }

    @Test
    fun `staged delete atomically hides a cache file and rollback restores its bytes`() = runTest {
        val mover = RecordingAtomicMover()
        val store = store(mover = mover)
        val original = File(root, "2026201-A11-0123456789ab.jpg").apply {
            writeText("cached")
        }

        val staged = store.stageDelete(original.name)

        assertThat(original.exists()).isFalse()
        assertThat(staged.tombstoneFile?.exists()).isTrue()
        assertThat(staged.tombstoneFile?.readText()).isEqualTo("cached")

        store.rollbackDelete(staged)

        assertThat(original.readText()).isEqualTo("cached")
        assertThat(staged.tombstoneFile?.exists()).isFalse()
        assertThat(mover.moves).hasSize(2)
    }

    @Test
    fun `committed staged delete permanently removes its tombstone`() = runTest {
        val store = store()
        val original = File(root, "2026201-A11-0123456789ab.jpg").apply {
            writeText("cached")
        }

        val staged = store.stageDelete(original.name)
        store.commitDelete(staged)

        assertThat(original.exists()).isFalse()
        assertThat(staged.tombstoneFile?.exists()).isFalse()
        assertThat(root.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `staged delete mover cancellation propagates unchanged without mapping it to file IO`() = runTest {
        val cancellation = CancellationException("stage delete cancelled")
        val original = File(root, "2026201-A11-0123456789ab.jpg").apply { writeText("cached") }
        val store = store(mover = AtomicFileMover { _, _ -> throw cancellation })

        assertCancellation(cancellation) { store.stageDelete(original.name) }

        assertThat(original.exists()).isTrue()
    }

    @Test
    fun `rollback delete mover cancellation propagates unchanged without mapping it to file IO`() = runTest {
        val original = File(root, "2026201-A11-0123456789ab.jpg").apply { writeText("cached") }
        val staged = store().stageDelete(original.name)
        val cancellation = CancellationException("rollback delete cancelled")
        val store = store(mover = AtomicFileMover { _, _ -> throw cancellation })

        assertCancellation(cancellation) { store.rollbackDelete(staged) }

        assertThat(original.exists()).isFalse()
        assertThat(staged.tombstoneFile?.exists()).isTrue()
    }

    @Test
    fun `cleanup restores a referenced tombstone after an interrupted staged delete`() = runTest {
        val mover = RecordingAtomicMover()
        val store = store(mover = mover)
        val original = File(root, "2026201-A11-0123456789ab.jpg").apply {
            writeText("cached")
        }
        val staged = store.stageDelete(original.name)

        store.removeTemporaryAndOrphanFiles(setOf(original.name))

        assertThat(original.readText()).isEqualTo("cached")
        assertThat(staged.tombstoneFile?.exists()).isFalse()
        assertThat(mover.moves).hasSize(2)
    }

    @Test
    fun `cleanup deletes tombstones that are unreferenced shadowed or malformed`() = runTest {
        val store = store()
        val unreferenced = File(root, "2026200-A11-0123456789ab.jpg").apply {
            writeText("orphan")
        }
        val unreferencedDeletion = store.stageDelete(unreferenced.name)
        val shadowed = File(root, "2026201-A11-abcdef012345.png").apply {
            writeText("old")
        }
        val shadowedDeletion = store.stageDelete(shadowed.name)
        shadowed.writeText("current")
        val malformedOriginalName = "2026202-A11-999999999999.jpg"
        val malformed = File(root, "$malformedOriginalName.delete.not-a-uuid.tmp").apply {
            writeText("unsafe tombstone")
        }

        store.removeTemporaryAndOrphanFiles(setOf(shadowed.name, malformedOriginalName))

        assertThat(unreferenced.exists()).isFalse()
        assertThat(unreferencedDeletion.tombstoneFile?.exists()).isFalse()
        assertThat(shadowed.readText()).isEqualTo("current")
        assertThat(shadowedDeletion.tombstoneFile?.exists()).isFalse()
        assertThat(File(root, malformedOriginalName).exists()).isFalse()
        assertThat(malformed.exists()).isFalse()
    }

    @Test
    fun `delete and rollback reject absolute traversal and root escaping paths`() = runTest {
        val store = store()
        val outside = File(root.parentFile, "outside.jpg").apply { writeText("keep") }
        try {
            assertThrowsIllegalArgument { store.delete("../${outside.name}") }
            assertThrowsIllegalArgument { store.delete(outside.absolutePath) }
            assertThrowsIllegalArgument { store.delete("nested/file.jpg") }
            assertThrowsIllegalArgument { store.stageDelete("../${outside.name}") }
            assertThrowsIllegalArgument { store.rollback(outside) }
            assertThat(outside.readText()).isEqualTo("keep")
        } finally {
            outside.delete()
        }
    }

    @Test
    fun `cleanup removes temporary and orphan cache files but preserves references and unrelated files`() =
        runTest {
            val referenced = "2026201-A11-0123456789ab.jpg"
            val orphan = "2026200-A11-abcdef012345.png"
            val temp = "2026202-A11-999999999999.jpg.download.tmp"
            val unrelated = "notes.txt"
            listOf(referenced, orphan, temp, unrelated).forEach { File(root, it).writeText(it) }

            store().removeTemporaryAndOrphanFiles(setOf(referenced))

            assertThat(File(root, referenced).exists()).isTrue()
            assertThat(File(root, orphan).exists()).isFalse()
            assertThat(File(root, temp).exists()).isFalse()
            assertThat(File(root, unrelated).exists()).isTrue()
        }

    private fun store(
        bounds: ImageBounds? = ImageBounds(10, 20),
        mover: AtomicFileMover = RecordingAtomicMover(),
        writer: CaibaoFileWriter = CaibaoFileWriter { target, bytes ->
            target.outputStream().use { output ->
                output.write(bytes)
                output.flush()
            }
        },
    ) = CaibaoFileStore(
        rootDirectory = root,
        imageBoundsReader = ImageBoundsReader { bounds },
        atomicFileMover = mover,
        caibaoFileWriter = writer,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private suspend fun assertFailure(
        expected: LiveContentFailure,
        block: suspend () -> Unit,
    ) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CaibaoFileException::class.java)
        assertThat((failure as CaibaoFileException).failure).isEqualTo(expected)
    }

    private suspend fun assertThrowsIllegalArgument(block: suspend () -> Unit) {
        assertThat(runCatching { block() }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private suspend fun assertCancellation(
        expected: CancellationException,
        block: suspend () -> Unit,
    ) {
        assertThat(runCatching { block() }.exceptionOrNull()).isSameInstanceAs(expected)
    }

    private fun jpegBytes(content: String = "valid"): ByteArray {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).apply {
            setRGB(0, 0, content.hashCode())
        }
        val output = ByteArrayOutputStream()
        check(ImageIO.write(image, "jpeg", output))
        return output.toByteArray()
    }

    private fun paddedJpeg(size: Int): ByteArray {
        val jpeg = jpegBytes()
        require(size >= jpeg.size)
        return ByteArray(size).also { padded ->
            jpeg.copyInto(
                destination = padded,
                startIndex = 0,
                endIndex = jpeg.size - JPEG_EOI_SIZE,
            )
            padded[padded.lastIndex - 1] = 0xFF.toByte()
            padded[padded.lastIndex] = 0xD9.toByte()
        }
    }

    private fun pngBytes(
        content: String = "valid",
        includeIdat: Boolean = true,
    ): ByteArray {
        val ihdr = ByteArrayOutputStream().also { output ->
            DataOutputStream(output).use { data ->
                data.writeInt(1)
                data.writeInt(1)
                data.writeByte(8)
                data.writeByte(0)
                data.writeByte(0)
                data.writeByte(0)
                data.writeByte(0)
            }
        }.toByteArray()
        val compressed = ByteArrayOutputStream().also { output ->
            DeflaterOutputStream(output).use { compressedOutput ->
                compressedOutput.write(byteArrayOf(0, content.hashCode().toByte()))
            }
        }.toByteArray()
        return PNG_SIGNATURE +
            pngChunk("IHDR", ihdr) +
            (if (includeIdat) pngChunk("IDAT", compressed) else byteArrayOf()) +
            pngChunk("IEND", byteArrayOf())
    }

    private fun pngChunk(
        type: String,
        data: ByteArray,
    ): ByteArray {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply {
            update(typeBytes)
            update(data)
        }
        return ByteArrayOutputStream().also { output ->
            DataOutputStream(output).use { chunk ->
                chunk.writeInt(data.size)
                chunk.write(typeBytes)
                chunk.write(data)
                chunk.writeInt(crc.value.toInt())
            }
        }.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class RecordingAtomicMover : AtomicFileMover {
        val moves = mutableListOf<Pair<File, File>>()

        override fun move(source: File, target: File) {
            moves += source to target
            Files.move(source.toPath(), target.toPath())
        }
    }

    private companion object {
        const val PNG_IHDR_CRC_OFFSET = 8 + 4 + 4 + 13
        const val PNG_IEND_CHUNK_SIZE = 12
        const val JPEG_FIRST_SEGMENT_LENGTH_OFFSET = 4
        const val JPEG_EOI_SIZE = 2
        val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val PNG_SIGNATURE = byteArrayOf(
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
