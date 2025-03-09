package space.kscience.maps.compose

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertFails

class OsmTileProviderTest {
//    @get:Rule
//    val rule = createComposeRule()


    @Test
    fun testCorrectOsm() = runTest {
        val provider = OpenStreetMapTileProvider(HttpClient(CIO), Files.createTempDirectory("mapCache"))
        val tileId = TileId(3, 1, 1)
        with(provider) {
            loadTileAsync(tileId).await()
        }
    }

    @Test
    fun testFailedOsm() = runTest {
        val provider = OpenStreetMapTileProvider(
            HttpClient(CIO),
            Files.createTempDirectory("mapCache"),
            osmBaseUrl = "https://tile.openstreetmap1.org"
        )
        val tileId = TileId(3, 1, 1)
        supervisorScope {
            with(provider) {
                val deferred = loadTileAsync(tileId)
                assertFails {
                    deferred.await()
                }
            }
        }
    }

}