// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.sciprog.maps.scheme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        //create a view point
        val viewPoint = remember {
            SchemeViewPoint(
                SchemeCoordinates(0f, 0f),
                1f
            )
        }
        val scope = rememberCoroutineScope()


        SchemeView(
            viewPoint,
            config = SchemeViewConfig(
                inferViewBoxFromFeatures = true,
                onClick = {
                    println("${focus.x}, ${focus.y}")
                }
            )
        ) {
            background(painterResource("middle-earth.jpg"))
            circle(410.52737 to 868.7676, color = Color.Blue)
            text(410.52737 to 868.7676, "Shire", color = Color.Blue)
            circle(1132.0881 to 394.99127, color = Color.Red)
            text(1132.0881 to 394.99127, "Ordruin", color = Color.Red)

            val hobbitId = circle(410.52737 to 868.7676)

            scope.launch {
                var t = 0.0
                while (isActive) {
                    val x = 410.52737 + t * (1132.0881 - 410.52737)
                    val y = 868.7676 + t * (394.99127 - 868.7676)
                    circle(x to y, color = Color.Green, id = hobbitId)
                    delay(100)
                    t += 0.005
                    if (t >= 1.0) t = 0.0
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
