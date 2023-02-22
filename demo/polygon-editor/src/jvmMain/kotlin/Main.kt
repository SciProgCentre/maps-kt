// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.sciprog.maps.features.*
import center.sciprog.maps.scheme.SchemeView
import center.sciprog.maps.scheme.XY
import center.sciprog.maps.scheme.XYCoordinateSpace
import center.sciprog.maps.scheme.XYViewScope

@Composable
@Preview
fun App() {
    MaterialTheme {

        var clickPoint by remember { mutableStateOf<XY?>(null) }

        val myPolygon: SnapshotStateList<XY> = remember { mutableStateListOf<XY>() }

        val featureState: FeatureGroup<XY> = FeatureGroup.remember(XYCoordinateSpace) {
            multiLine(
                listOf(XY(0f, 0f), XY(0f, 1f), XY(1f, 1f), XY(1f, 0f), XY(0f, 0f)),
                id = "frame"
            )
        }

        val mapState: XYViewScope = XYViewScope.remember(
            config = ViewConfig<XY>(
                onClick = { event, point ->
                    if (event.buttons.isSecondaryPressed) {
                        clickPoint = point.focus
                    }
                }
            ),
            initialRectangle = featureState.getBoundingBox(1f),
        )

        CursorDropdownMenu(clickPoint != null, { clickPoint = null }) {
            clickPoint?.let { point ->
                TextButton({
                    myPolygon.add(point)
                    if (myPolygon.isNotEmpty()) {
                        featureState.group(id = "polygon") {
                            val pointRefs = myPolygon.mapIndexed { index, xy ->
                                circle(xy, id = "point[$index]").draggable { _, to ->
                                    myPolygon[index] = to.focus
                                }
                            }
                            draggableMultiLine(
                                pointRefs + pointRefs.first(),
                            )
                        }
                    }
                    clickPoint = null
                }) {
                    Text("Create node")
                }
            }
        }

        SchemeView(
            mapState,
            featureState,
        )

    }
}

fun main() = application {
    Window(title = "Polygon editor demo", onCloseRequest = ::exitApplication) {
        App()
    }
}
