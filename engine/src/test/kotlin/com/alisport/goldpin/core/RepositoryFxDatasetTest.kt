package com.alisport.goldpin.core

import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Integrity and volume checks on the actual release payload, without loading it into RAM. */
class RepositoryFxDatasetTest {
    private val expected = listOf(
        Triple(1, 369390, "c66b3d01614f42508978c6061a9a29a6a452ff25f5aa868489075d94f28a3abd"),
        Triple(2, 738328, "547d84e1faf31bc5930460ac8c07f52f4b5c065b7e6107fc1ffe12dd7189b56e"),
        Triple(3, 1107619, "d227be7d372e44fd1133b22fd9818cc8dcfae25f6f33e976aa615db2a9b89e31"),
        Triple(10, 3580052, "081bb7af6e8dfe966ef4c17d03f9ddba4788d02914aa3e6daf698193b24394fe"),
        Triple(20, 7101926, "fc1ccef3b080e00ea049624684d20cc5cbfb35d3f53942ce42a2fa52487aec2e")
    )

    @Test fun `available GBPUSD archives have expected hash`() {
        for ((year, _, sha) in expected) {
            val file = File("datasets/goldpin_gbpusd_m1_${year}y.csv.gz")
            if (!file.isFile) continue
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { stream ->
                val buf = ByteArray(65536)
                while (true) { val n = stream.read(buf); if (n < 0) break; digest.update(buf, 0, n) }
            }
            assertEquals(sha, digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }, file.name)
        }
    }

    @Test fun `twenty year archive has ordered OHLCV rows and strictly positive volume`() {
        val file = File("datasets/goldpin_gbpusd_m1_20y.csv.gz")
        assumeTrue(file.isFile, "20-year GBPUSD archive unavailable")
        var count = 0; var prior = Long.MIN_VALUE
        GZIPInputStream(file.inputStream().buffered(65536)).bufferedReader().use { reader ->
            assertEquals("timestamp,open,high,low,close,volume", reader.readLine())
            while (true) {
                val line = reader.readLine() ?: break
                val f = line.split(',')
                assertEquals(6, f.size)
                val t = f[0].toLong(); val o = f[1].toDouble(); val h = f[2].toDouble()
                val l = f[3].toDouble(); val c = f[4].toDouble(); val v = f[5].toLong()
                assertTrue(t > prior && v > 0 && l <= minOf(o, c) && h >= maxOf(o, c), "row $count")
                prior = t; count++
            }
        }
        assertEquals(7101926, count)
    }
}
