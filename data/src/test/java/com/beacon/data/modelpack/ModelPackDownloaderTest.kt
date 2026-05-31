package com.beacon.data.modelpack

import com.beacon.core.concurrency.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ModelPackDownloaderTest {

    private lateinit var server: MockWebServer
    private lateinit var downloader: ModelPackDownloader

    private val dispatchers = object : DispatcherProvider {
        override val main get() = Dispatchers.Unconfined
        override val default get() = Dispatchers.Unconfined
        override val io get() = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        downloader = ModelPackDownloader(dispatchers)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun download_writesFullFile() = runTest {
        val body = "beacon-pack-bytes"
        server.enqueue(MockResponse().setBody(body).setResponseCode(200))

        val dest = File.createTempFile("dest", ".bin")
        val tempParent = dest.parentFile!!
        dest.delete()

        downloader.download(server.url("/pack").toString(), dest) { _, _ -> }

        assertTrue(dest.isFile)
        assertEquals(body.length.toLong(), dest.length())
        dest.delete()
        tempParent.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun download_resumesFromPartialPartFile() = runTest {
        val full = ByteArray(256) { it.toByte() }
        val firstHalf = full.copyOfRange(0, 128)
        val secondHalf = full.copyOfRange(128, 256)

        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 128-255/256")
                .setBody(Buffer().write(secondHalf)),
        )

        val dest = File.createTempFile("dest", ".bin")
        val part = File(dest.parentFile, "${dest.name}.part")
        dest.delete()
        part.writeBytes(firstHalf)

        downloader.download(server.url("/pack").toString(), dest) { _, _ -> }

        assertEquals(256L, dest.length())
        assertTrue(full.contentEquals(dest.readBytes()))
        dest.delete()
        part.delete()
    }
}
