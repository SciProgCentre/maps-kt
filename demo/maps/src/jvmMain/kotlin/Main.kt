import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.sciprog.maps.compose.*
import center.sciprog.maps.coordinates.Distance
import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.MapViewPoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.math.PI
import kotlin.random.Random
import center.sciprog.maps.coordinates.kilometers

private fun GeodeticMapCoordinates.toShortString(): String =
    "${(latitude.degrees.value).toString().take(6)}:${(longitude.degrees.value).toString().take(6)}"


@Composable
@Preview
fun App() {
    MaterialTheme {
        //create a view point
        val viewPoint = remember {
            MapViewPoint(
                GeodeticMapCoordinates.ofDegrees(55.7558, 37.6173),
                8.0
            )
        }

        val scope = rememberCoroutineScope()
        val mapTileProvider = remember {
            OpenStreetMapTileProvider(
                client = HttpClient(CIO),
                cacheDirectory = Path.of("mapCache")
            )
        }

        var centerCoordinates by remember { mutableStateOf<GeodeticMapCoordinates?>(null) }


        val pointOne = 55.568548 to 37.568604
        val pointTwo = 55.929444 to 37.518434
        val pointThree = 60.929444 to 37.518434

        val dragPoint = 55.744 to 37.614

        MapView(
            mapTileProvider = mapTileProvider,
            initialViewPoint = null,// use null to infer view point from features
            config = MapViewConfig(
                onViewChange = { centerCoordinates = focus },
            )
        ) {

            image(pointOne, Icons.Filled.Home)

            rectangle(dragPoint, id = "dragMe", size = DpSize(10.dp, 10.dp)).draggable()

            points(
                points = listOf(
                    55.742465 to 37.615812,
                    55.742713 to 37.616370,
                    55.742815 to 37.616659,
                    55.742320 to 37.617132,
                    55.742086 to 37.616566,
                    55.741715 to 37.616716
                ),
                pointMode = PointMode.Polygon
            )

            //remember feature ID
            val circleId: FeatureId = circle(
                centerCoordinates = pointTwo,
            )

            draw(position = pointThree) {
                drawLine(start = Offset(-10f, -10f), end = Offset(10f, 10f), color = Color.Red)
                drawLine(start = Offset(-10f, 10f), end = Offset(10f, -10f), color = Color.Red)
            }

            arc(pointOne, 10.0.kilometers, 0f, PI)

            line(pointOne, pointTwo, id = "line")
            text(pointOne, "Home", font = { size = 32f })

            centerCoordinates?.let {
                group(id = "center") {
                    circle(center = it, color = Color.Blue, size = 1f)
                    text(position = it, it.toShortString(), color = Color.Blue)
                }
            }

            scope.launch {
                while (isActive) {
                    delay(200)
                    //Overwrite a feature with new color
                    circle(
                        pointTwo,
                        id = circleId,
                        color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
                    )
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
