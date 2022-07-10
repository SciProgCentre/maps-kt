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
import centre.sciprog.maps.compose.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.nio.file.Path

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewPoint = MapViewPoint(
            GeodeticMapCoordinates.ofDegrees(55.7558, 37.6173),
            6.0
        )
        val pointOne = 55.568548 to 37.568604
        val pointTwo = 55.929444 to 37.518434
        val features = buildList<MapFeature> {
//            add(MapCircleFeature(pointOne))
            add(MapCircleFeature(pointTwo))
            add(MapLineFeature(pointOne, pointTwo))
            add(MapTextFeature(pointOne.toCoordinates(), "Home"))
            add(MapVectorImageFeature(pointOne.toCoordinates(), Icons.Filled.Home))
        }

        val scope = rememberCoroutineScope()
        val mapTileProvider = remember { OpenStreetMapTileProvider(scope, HttpClient(CIO), Path.of("mapCache")) }

        var coordinates by remember { mutableStateOf<GeodeticMapCoordinates?>(null) }

        Column {
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
