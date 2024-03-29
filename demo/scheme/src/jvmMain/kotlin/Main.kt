// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.sciprog.maps.features.FeatureGroup
import center.sciprog.maps.features.ViewConfig
import center.sciprog.maps.features.ViewPoint
import center.sciprog.maps.features.color
import center.sciprog.maps.scheme.*
import center.sciprog.maps.svg.FeatureStateSnapshot
import center.sciprog.maps.svg.exportToSvg
import center.sciprog.maps.svg.snapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import space.kscience.kmath.geometry.Angle
import java.awt.Desktop
import java.nio.file.Files

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()

        val schemeFeaturesState: FeatureGroup<XY> = FeatureGroup.remember(XYCoordinateSpace) {
            background(1600f, 1200f) { painterResource("middle-earth.jpg") }
            circle(410.52737 to 868.7676).color(Color.Blue)
            text(410.52737 to 868.7676, "Shire").color(Color.Blue)
            circle(1132.0881 to 394.99127).color(Color.Red)
            text(1132.0881 to 394.99127, "Ordruin").color(Color.Red)
            arc(center = 1132.0881 to 394.99127, radius = 20f, startAngle = Angle.zero, Angle.piTimes2)

            //circle(410.52737 to 868.7676, id = "hobbit")

            scope.launch {
                var t = 0.0
                while (isActive) {
                    val x = 410.52737 + t * (1132.0881 - 410.52737)
                    val y = 868.7676 + t * (394.99127 - 868.7676)
                    circle(x to y, id = "hobbit").color(Color.Green)
                    delay(100)
                    t += 0.005
                    if (t >= 1.0) t = 0.0
                }
            }
        }

        val initialViewPoint: ViewPoint<XY> = remember {
            schemeFeaturesState.getBoundingBox(1f)?.computeViewPoint() ?: XYViewPoint(XY(0f, 0f))
        }

        var viewPoint: ViewPoint<XY> by remember { mutableStateOf(initialViewPoint) }

        var snapshot: FeatureStateSnapshot<XY>? by remember { mutableStateOf(null) }

        if (snapshot == null) {
            snapshot = schemeFeaturesState.snapshot()
        }

        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem("Export to SVG") {
                        snapshot?.let {
                            val path = Files.createTempFile("scheme-kt-", ".svg")
                            it.exportToSvg(viewPoint, 800.0, 800.0, path)
                            println(path.toFile())
                            Desktop.getDesktop().browse(path.toFile().toURI())
                        }
                    },
                )
            }
        ) {
            val mapState: XYViewScope = XYViewScope.remember(
                ViewConfig(
                    onClick = { _, click ->
                        println("${click.focus.x}, ${click.focus.y}")
                    },
                    onViewChange = { viewPoint = this }
                ),
                initialViewPoint = initialViewPoint,
            )

            SchemeView(
                mapState,
                schemeFeaturesState,
            )
        }

    }
}

fun main() = application {
    Window(title = "Scheme demo", onCloseRequest = ::exitApplication) {
        App()
    }
}
