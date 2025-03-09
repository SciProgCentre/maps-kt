@file:OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import space.kscience.kmath.geometry.Angle
import space.kscience.maps.features.FeatureStore
import space.kscience.maps.features.ViewConfig
import space.kscience.maps.features.ViewPoint
import space.kscience.maps.features.color
import space.kscience.maps.scheme.*
import space.kscience.maps_wasm.generated.resources.Res
import space.kscience.maps_wasm.generated.resources.middle_earth


@Composable
fun App() {

    val scope = rememberCoroutineScope()


    val features = FeatureStore.remember(XYCoordinateSpace) {
        background(1600f, 1200f) {
            painterResource(Res.drawable.middle_earth)
        }
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
        features.getBoundingBox(1f)?.computeViewPoint() ?: XYViewPoint(XY(0f, 0f))
    }

    var viewPoint: ViewPoint<XY> by remember { mutableStateOf(initialViewPoint) }

    val mapState: XYCanvasState = XYCanvasState.remember(
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
        features,
    )

}


fun main() {
//    renderComposable(rootElementId = "root") {
    CanvasBasedWindow("Maps demo", canvasElementId = "ComposeTarget") {
        App()
    }
}
