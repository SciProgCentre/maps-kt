// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import centre.sciprog.maps.compose.GeodeticMapCoordinates
import centre.sciprog.maps.compose.MapRectangle
import centre.sciprog.maps.compose.MapView
import java.nio.file.Path

@Composable
@Preview
fun App() {
    MaterialTheme {
        val map = MapRectangle.of(
            GeodeticMapCoordinates.ofDegrees(66.513260, 0.0),
            GeodeticMapCoordinates.ofDegrees(40.979897, 44.999999),
        )
        MapView(map,  modifier = Modifier.fillMaxSize(), initialZoom = 4.0, cacheDirectory = Path.of("mapCache"))
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
