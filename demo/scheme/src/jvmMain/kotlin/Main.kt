// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import center.sciprog.scheme.*

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
            text(410.52737 to 868.7676,"Shire", color = Color.Blue)
            circle(1132.0881 to 394.99127, color = Color.Red)
            text(1132.0881 to 394.99127, "Ordruin",color = Color.Red)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
