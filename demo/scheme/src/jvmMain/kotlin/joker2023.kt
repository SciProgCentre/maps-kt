import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import space.kscience.maps.features.*
import space.kscience.maps.scheme.*
import space.kscience.maps.scheme.XYCoordinateSpace.Rectangle


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Joker2023 demo", icon = painterResource("SPC-logo.png")) {
        MaterialTheme {

            SchemeView(
                initialRectangle = Rectangle(XY(0f, 0f), XY(1734f, 724f)),
                config = ViewConfig(
                    onClick = { _, pointer ->
                        println("(${pointer.focus.x}, ${pointer.focus.y})")
                    }
                )
            ) {
                background(1734f, 724f, id = "background") { painterResource("joker2023.png") }
                group(id = "hall_1") {
                    polygon(
                        listOf(
                            XY(1582.0042, 210.29636),
                            XY(1433.7021, 127.79796),
                            XY(1370.7639, 127.79796),
                            XY(1315.293, 222.73865),
                            XY(1314.2262, 476.625),
                            XY(1364.3635, 570.4984),
                            XY(1434.7689, 570.4984),
                            XY(1579.8469, 493.69244),
                        )
                    ).modifyAttributes {
                        ColorAttribute(Color.Blue)
                        AlphaAttribute(0.4f)
                    }.onClick {
                        println("hall_1")
                    }
                }

                group(id = "hall_2") {
                    rectanglePolygon(
                        left = 893, right = 1103,
                        bottom = 223, top = 406,
                    ).modifyAttributes {
                        ColorAttribute(Color.Blue)
                        AlphaAttribute(0.4f)
                    }
                }

                group(id = "hall_3") {
                    rectanglePolygon(
                        Rectangle(XY(460f, 374f), width = 140f, height = 122f),
                    ).modifyAttributes {
                        ColorAttribute(Color.Blue)
                        AlphaAttribute(0.4f)
                    }
                }

                group(id = "people") {
                    icon(XY(815.60535, 342.71313), Icons.Default.Face).color(Color.Red)
                    icon(XY(743.751, 381.09064), Icons.Default.Face).color(Color.Red)
                    icon(XY(1349.6648, 417.36014), Icons.Default.Face).color(Color.Red)
                    icon(XY (1362.4658, 287.21667), Icons.Default.Face).color(Color.Red)
                    icon(XY(208.24274, 317.08566), Icons.Default.Face).color(Color.Red)
                    icon(XY (293.5827, 319.21915), Icons.Default.Face).color(Color.Red)
                }
            }
        }
    }
}
