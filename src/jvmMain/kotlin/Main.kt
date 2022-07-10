// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import centre.sciprog.maps.compose.*
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
            add(MapCircleFeature(pointOne))
            add(MapCircleFeature(pointTwo))
            add(MapLineFeature(pointOne, pointTwo))
        }
        MapView(viewPoint, features = features, cacheDirectory = Path.of("mapCache"))
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
