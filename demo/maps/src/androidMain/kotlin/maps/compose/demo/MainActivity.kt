package maps.compose.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import center.sciprog.maps.compose.MapView
import center.sciprog.maps.compose.OpenStreetMapTileProviderAndroid
import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.Gmc
import center.sciprog.maps.features.ViewConfig
import center.sciprog.maps.features.circle
import center.sciprog.maps.features.color
import center.sciprog.maps.features.group
import center.sciprog.maps.features.text
import center.sciprog.maps.geojson.geoJson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import space.kscience.kmath.geometry.degrees


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }

    private fun GeodeticMapCoordinates.toShortString(): String =
        "${(latitude.degrees).toString().take(6)}:${(longitude.degrees).toString().take(6)}"


    @Composable
    @Preview
    private fun App() {
        val directory = filesDir
        val scope = rememberCoroutineScope()

        val mapTileProvider = remember {
            OpenStreetMapTileProviderAndroid(
                client = HttpClient(CIO),
                cacheDirectory = directory
            )
        }

        val centerCoordinates = MutableStateFlow<Gmc?>(null)

        MapView(
            mapTileProvider = mapTileProvider,
            config = ViewConfig(
                onViewChange = { centerCoordinates.value = focus },
                onClick = { _, viewPoint ->
                    println(viewPoint)
                },
                zoomSpeed = 0.1f,

            )
        ) {
            geoJson(javaClass.getResource("/moscow.geo.json")!!)

            centerCoordinates.filterNotNull().onEach {
                group(id = "center") {
                    circle(center = it, id = "circle", size = 1.dp).color(Color.Blue)
                    text(position = it, text = it.toShortString(), id = "text").color(Color.Blue)
                }
            }.launchIn(scope)
        }
    }
}