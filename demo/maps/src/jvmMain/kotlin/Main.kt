import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.kscience.attributes.Attributes
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.degrees
import space.kscience.kmath.geometry.radians
import space.kscience.maps.compose.*
import space.kscience.maps.coordinates.GeodeticMapCoordinates
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.coordinates.kilometers
import space.kscience.maps.features.*
import space.kscience.maps.geojson.geoJson
import java.nio.file.Path
import kotlin.math.PI
import kotlin.random.Random

public fun GeodeticMapCoordinates.toShortString(): String =
    "${(latitude.toDegrees().value).toString().take(6)}:${(longitude.toDegrees().value).toString().take(6)}"


@OptIn(ExperimentalFoundationApi::class)
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

        val centerCoordinates = MutableStateFlow<Gmc?>(null)

        val pointOne = 55.568548 to 37.568604
        val pointTwo = 55.929444 to 37.518434
//        val pointThree = 60.929444 to 37.518434

        MapView(
            mapTileProvider = mapTileProvider,
            config = ViewConfig(
                onViewChange = { centerCoordinates.value = focus },
                onClick = { _, viewPoint ->
                    println(viewPoint)
                }
            )
        ) {

            geoJson(javaClass.getResource("/moscow.geo.json")!!)
                .color(Color.Blue)
                .alpha(0.4f)

            icon(pointOne, Icons.Filled.Home)

            val marker1 = rectangle(55.744 to 38.614, size = DpSize(10.dp, 10.dp))
                .color(Color.Magenta)
            val marker2 = rectangle(55.8 to 38.5, size = DpSize(10.dp, 10.dp))
                .color(Color.Magenta)
            val marker3 = rectangle(56.0 to 38.5, size = DpSize(10.dp, 10.dp))
                .color(Color.Magenta)

            draggableLine(marker1, marker2, id = "line 1").color(Color.Red).onClick {
                println("line 1 clicked")
            }
            draggableLine(marker2, marker3, id = "line 2").color(Color.DarkGray).onClick {
                println("line 2 clicked")
            }
            draggableLine(marker3, marker1, id = "line 3").color(Color.Blue).onClick {
                println("line 3 clicked")
            }


            multiLine(
                points = listOf(
                    55.742465 to 37.615812,
                    55.742713 to 37.616370,
                    55.742815 to 37.616659,
                    55.742320 to 37.617132,
                    55.742086 to 37.616566,
                    55.741715 to 37.616716
                ),
            )

//            points(
//                points = listOf(
//                    55.744 to 38.614,
//                    55.8 to 38.5,
//                    56.0 to 38.5,
//                )
//            ).pointSize(5f)

//            geodeticLine(Gmc.ofDegrees(40.7128, -74.0060), Gmc.ofDegrees(55.742465, 37.615812)).color(Color.Blue)
//            line(Gmc.ofDegrees(40.7128, -74.0060), Gmc.ofDegrees(55.742465, 37.615812))


            //remember feature ref
            val circleId = circle(
                centerCoordinates = pointTwo,
            )
            scope.launch {
                while (isActive) {
                    delay(200)
                    circleId.color(Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()))
                }
            }


//            draw(position = pointThree) {
//                drawLine(start = Offset(-10f, -10f), end = Offset(10f, 10f), color = Color.Red)
//                drawLine(start = Offset(-10f, 10f), end = Offset(10f, -10f), color = Color.Red)
//            }

            arc(pointOne, 10.0.kilometers, (PI / 4).radians, -Angle.pi / 2)


            line(pointOne, pointTwo, id = "line")
            text(pointOne, "Home", font = { size = 32f })


            pixelMap(
                space.Rectangle(
                    Gmc(latitude = 55.58461879539754.degrees, longitude = 37.8746197303493.degrees),
                    Gmc(latitude = 55.442792937592415.degrees, longitude = 38.132240805463844.degrees)
                ),
                0.005.degrees,
                0.005.degrees
            ) { gmc ->
                Color(
                    red = ((gmc.latitude + Angle.piDiv2).toDegrees().value * 10 % 1f).toFloat(),
                    green = ((gmc.longitude + Angle.pi).toDegrees().value * 10 % 1f).toFloat(),
                    blue = 0f,
                    alpha = 0.3f
                )
            }

            centerCoordinates.filterNotNull().onEach {
                group(id = "center") {
                    circle(center = it, id = "circle", size = 1.dp).color(Color.Blue)
                    text(position = it, it.toShortString(), id = "text").color(Color.Blue)
                }
            }.launchIn(scope)

            //Add click listeners for all polygons
            forEachWithType<Gmc, PolygonFeature<Gmc>> { ref, polygon: PolygonFeature<Gmc> ->
                ref.onClick(PointerMatcher.Primary) {
                    println("Click on $ref")
                    //draw in top-level scope
                    with(this@MapView) {
                        multiLine(
                            polygon.points,
                            attributes = Attributes(ZAttribute, 10f),
                            id = "selected",
                        ).modifyAttribute(StrokeAttribute, 4f).color(Color.Magenta)
                    }
                }
            }
//            println(toPrettyString())
        }
    }
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Maps-kt demo", icon = painterResource("SPC-logo.png")) {
        App()
    }
}
