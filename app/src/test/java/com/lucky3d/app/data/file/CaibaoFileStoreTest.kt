package com.lucky3d.app.data.file

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
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
    fun `eight MiB is accepted and one extra byte is rejected`() = runTest {
        val exact = ByteArray(8 * 1024 * 1024).also {
            JPEG_SIGNATURE.copyInto(it)
        }
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
    fun `delete and rollback reject absolute traversal and root escaping paths`() = runTest {
        val store = store()
        val outside = File(root.parentFile, "outside.jpg").apply { writeText("keep") }
        try {
            assertThrowsIllegalArgument { store.delete("../${outside.name}") }
            assertThrowsIllegalArgument { store.delete(outside.absolutePath) }
            assertThrowsIllegalArgument { store.delete("nested/file.jpg") }
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
    ) = CaibaoFileStore(
        rootDirectory = root,
        imageBoundsReader = ImageBoundsReader { bounds },
        atomicFileMover = mover,
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

    private fun jpegBytes(content: String = "valid") =
        JPEG_SIGNATURE + content.toByteArray()

    private fun pngBytes(content: String = "valid") =
        PNG_SIGNATURE + content.toByteArray()

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
