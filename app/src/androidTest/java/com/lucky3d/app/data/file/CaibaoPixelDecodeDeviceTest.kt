package com.lucky3d.app.data.file

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaibaoPixelDecodeDeviceTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "caibao-pixel-${UUID.randomUUID()}").apply {
            check(mkdirs())
        }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun validAndroidBitmapCanBeStaged() = runTest {
        val output = ByteArrayOutputStream()
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            assertThat(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)).isTrue()
        } finally {
            bitmap.recycle()
        }

        val staged = CaibaoFileStore(root).stageAndValidate(
            issue = "2026201",
            bytes = output.toByteArray(),
            mimeType = "image/png",
        )

        assertThat(staged.width).isEqualTo(2)
        assertThat(staged.height).isEqualTo(2)
    }

    @Test
    fun structurallyCompletePngWithBrokenPixelStreamIsRejectedBeforeWrite() = runTest {
        val failure = runCatching {
            CaibaoFileStore(root).stageAndValidate(
                issue = "2026201",
                bytes = brokenPixelPng(),
                mimeType = "image/png",
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(CaibaoFileException::class.java)
        assertThat((failure as CaibaoFileException).failure)
            .isEqualTo(LiveContentFailure.INVALID_IMAGE)
        assertThat(root.listFiles().orEmpty()).isEmpty()
    }

    private fun brokenPixelPng(): ByteArray {
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
        return PNG_SIGNATURE +
            pngChunk("IHDR", ihdr) +
            pngChunk("IDAT", deflate(byteArrayOf(5, 0))) +
            pngChunk("IEND", byteArrayOf())
    }

    private fun deflate(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { output ->
            DeflaterOutputStream(output).use { compressed ->
                compressed.write(bytes)
            }
        }.toByteArray()

    private fun pngChunk(type: String, data: ByteArray): ByteArray {
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

    private companion object {
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
