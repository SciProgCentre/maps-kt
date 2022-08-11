// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.sciprog.maps.compose.*
import center.sciprog.maps.coordinates.Distance
import center.sciprog.maps.coordinates.GeodeticMapCoordinates
import center.sciprog.maps.coordinates.GmcBox
import center.sciprog.maps.coordinates.MapViewPoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.math.PI
import kotlin.random.Random

private fun GeodeticMapCoordinates.toShortString(): String =
    "${(latitude * 180.0 / PI).toString().take(6)}:${(longitude * 180.0 / PI).toString().take(6)}"


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

//        val markers = (1..1_000_000).map {
//            val position = GeodeticMapCoordinates.ofDegrees(
//                latitude = Random.nextDouble(-90.0, 90.0),
//                longitude = Random.nextDouble(0.0, 180.0)
//            )
//            MapDrawFeature(
//                position = position,
//                computeBoundingBox = {
//                    GmcBox.withCenter(
//                        center = position,
//                        width = Distance(0.001),
//                        height = Distance(0.001)
//                    )
//                }
//            ) {
//                drawRoundRect(
//                    color = Color.Yellow,
//                    size = Size(1f, 1f)
//                )
//            }
//        }
        val state = MapViewState(
            mapTileProvider = mapTileProvider,
            computeViewPoint = { viewPoint },
            config = MapViewConfig(
                inferViewBoxFromFeatures = true,
                onViewChange = { centerCoordinates = focus },
            )
        ) {
            val pointOne = 55.568548 to 37.568604
            val pointTwo = 55.929444 to 37.518434
            val pointThree = 60.929444 to 37.518434

            image(pointOne, Icons.Filled.Home)

            //remember feature Id
            val circleId: FeatureId = circle(
                centerCoordinates = pointTwo,
            )

            draw(
                position = pointThree,
                getBoundingBox = {
                    GmcBox.withCenter(
                        center = GeodeticMapCoordinates.ofDegrees(
                            pointThree.first,
                            pointThree.second
                        ),
                        height = Distance(0.001),
                        width = Distance(0.001)
                    )
                }
            ) {
                drawLine(start = Offset(-10f, -10f), end = Offset(10f, 10f), color = Color.Red)
                drawLine(start = Offset(-10f, 10f), end = Offset(10f, -10f), color = Color.Red)
            }

//            markers.forEach { feature ->
//                featureSelector {
//                    feature
//                }
//            }

            arc(pointOne, Distance(10.0), 0f, PI)

            line(pointOne, pointTwo)
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
        MapView(
            mapViewState = state
        )
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
