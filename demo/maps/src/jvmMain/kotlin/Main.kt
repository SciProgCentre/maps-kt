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
import center.sciprog.maps.coordinates.*
import center.sciprog.maps.features.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.math.PI
import kotlin.random.Random

private fun GeodeticMapCoordinates.toShortString(): String =
    "${(latitude.degrees.value).toString().take(6)}:${(longitude.degrees.value).toString().take(6)}"


@Composable
@Preview
fun App() {
    MaterialTheme {

        val scope = rememberCoroutineScope()
        val mapTileProvider = remember {
            OpenStreetMapTileProvider(
                client = HttpClient(CIO),
                cacheDirectory = Path.of("mapCache")
            )
        }

        var centerCoordinates by remember { mutableStateOf<Gmc?>(null) }


        val pointOne = 55.568548 to 37.568604
        val pointTwo = 55.929444 to 37.518434
        val pointThree = 60.929444 to 37.518434

        val dragPoint = 55.744 to 37.614

        MapView(
            mapTileProvider = mapTileProvider,
//            initialViewPoint = MapViewPoint(
//                GeodeticMapCoordinates.ofDegrees(55.7558, 37.6173),
//                8.0
//            ),
//            initialRectangle = GmcRectangle.square(
//                GeodeticMapCoordinates.ofDegrees(55.7558, 37.6173),
//                50.kilometers,
//                50.kilometers
//            ),
            config = ViewConfig(
                onViewChange = { centerCoordinates = focus },
            )
        ) {

            image(pointOne, Icons.Filled.Home)

            val marker1 = rectangle(55.744 to 37.614, size = DpSize(10.dp, 10.dp), color = Color.Magenta)
            val marker2 = rectangle(55.8 to 37.5, size = DpSize(10.dp, 10.dp), color = Color.Magenta)
            val marker3 = rectangle(56.0 to 37.5, size = DpSize(10.dp, 10.dp), color = Color.Magenta)

            draggableLine(marker1, marker2, color = Color.Blue)
            draggableLine(marker2, marker3, color = Color.Blue)
            draggableLine(marker3, marker1, color = Color.Blue)

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
            circle(
                centerCoordinates = pointTwo,
            ).updated(scope) {
                delay(200)
                //Overwrite a feature with new color
                it.copy(color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()))
            }

            draw(position = pointThree) {
                drawLine(start = Offset(-10f, -10f), end = Offset(10f, 10f), color = Color.Red)
                drawLine(start = Offset(-10f, 10f), end = Offset(10f, -10f), color = Color.Red)
            }

            arc(pointOne, 10.0.kilometers, (PI / 4).radians, -Angle.pi / 2)

            line(pointOne, pointTwo, id = "line")
            text(pointOne, "Home", font = { size = 32f })

            centerCoordinates?.let {
                group(id = "center") {
                    circle(center = it, color = Color.Blue, id = "circle", size = 1.dp)
                    text(position = it, it.toShortString(), id = "text", color = Color.Blue)
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
