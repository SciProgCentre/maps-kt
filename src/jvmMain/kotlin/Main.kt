// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import centre.sciprog.maps.GeodeticMapCoordinates
import centre.sciprog.maps.MapViewPoint
import centre.sciprog.maps.compose.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.random.Random

/**
 *  initial set of features
 */
@Composable
private fun initialFeatures() = buildList {
    val pointOne = 55.568548 to 37.568604
    val pointTwo = 55.929444 to 37.518434
    add(MapVectorImageFeature(pointOne.toCoordinates(), Icons.Filled.Home))
//            add(MapCircleFeature(pointOne))
    add(MapCircleFeature(pointTwo))
    add(MapLineFeature(pointOne, pointTwo))
    add(MapTextFeature(pointOne.toCoordinates(), "Home"))
}


@Composable
@Preview
fun App() {
    MaterialTheme {
        //create a view point
        val viewPoint = remember {
            MapViewPoint(
                GeodeticMapCoordinates.ofDegrees(55.7558, 37.6173),
                6.0
            )
        }

        // observable list of features
        val features = mutableStateListOf<MapFeature>().apply {
            addAll(initialFeatures())
        }

//        // test dynamic rendering
//        LaunchedEffect(features) {
//            repeat(10000) {
//                delay(10)
//                val randomPoint = Random.nextDouble(55.568548, 55.929444) to Random.nextDouble(37.518434, 37.568604)
//                features.add(MapCircleFeature(randomPoint))
//            }
//        }

        val scope = rememberCoroutineScope()
        val mapTileProvider = remember { OpenStreetMapTileProvider(scope, HttpClient(CIO), Path.of("mapCache")) }

        var coordinates by remember { mutableStateOf<GeodeticMapCoordinates?>(null) }

        Column {
            //display click coordinates
            Text(coordinates?.toString() ?: "")
            MapView(viewPoint, mapTileProvider, features = features) {
                coordinates = it
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
