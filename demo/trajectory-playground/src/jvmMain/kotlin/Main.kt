import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.Vector2D
import space.kscience.kmath.geometry.euclidean2d.Circle2D
import space.kscience.kmath.geometry.euclidean2d.Float64Space2D
import space.kscience.maps.features.*
import space.kscience.maps.scheme.SchemeView
import space.kscience.maps.scheme.XY
import space.kscience.trajectory.*
import kotlin.random.Random

private fun Vector2D<out Number>.toXY() = XY(x.toFloat(), y.toFloat())

private val random = Random(123)

fun FeatureBuilder<XY>.trajectory(
    trajectory: Trajectory2D,
    colorPicker: (Trajectory2D) -> Color = { Color.Blue },
): FeatureRef<XY, FeatureGroup<XY>> = group {
    when (trajectory) {
        is StraightTrajectory2D -> line(
            aCoordinates = trajectory.begin.toXY(),
            bCoordinates = trajectory.end.toXY(),
        ).color(colorPicker(trajectory))

        is CircleTrajectory2D -> with(Float64Space2D) {
            val topLeft = trajectory.circle.center + vector(-trajectory.circle.radius, trajectory.circle.radius)
            val bottomRight = trajectory.circle.center + vector(trajectory.circle.radius, -trajectory.circle.radius)

            val rectangle = Rectangle(
                topLeft.toXY(),
                bottomRight.toXY()
            )

            arc(
                oval = rectangle,
                startAngle = trajectory.arcStart - Angle.piDiv2,
                arcLength = trajectory.arcAngle,
            ).color(colorPicker(trajectory))
        }

        is CompositeTrajectory2D -> trajectory.segments.forEach {
            trajectory(it, colorPicker)
        }
    }
}

fun FeatureBuilder<XY>.obstacle(obstacle: Obstacle, colorPicker: (Trajectory2D) -> Color = { Color.Red }) {
    trajectory(obstacle.circumvention, colorPicker)
    polygon(obstacle.arcs.map { it.center.toXY() }).color(Color.Gray)
}

fun FeatureBuilder<XY>.pose(pose2D: Pose2D) = with(Float64Space2D) {
    line(pose2D.toXY(), (pose2D + Pose2D.bearingToVector(pose2D.bearing)).toXY())
}

@Composable
@Preview
fun closePoints()  = with(Float64Space2D){
    SchemeView {

        val obstacle = Obstacle(
            Circle2D(vector(0.0, 0.0), 1.0),
            Circle2D(vector(0.0, 1.0), 1.0),
            Circle2D(vector(1.0, 1.0), 1.0),
            Circle2D(vector(1.0, 0.0), 1.0)
        )
        val enter = Pose2D(-0.8, -0.8, Angle.pi)
        val exit = Pose2D(-0.8, -0.8, Angle.piDiv2)

        pose(enter)
        pose(exit)

        val paths: List<Trajectory2D> = Obstacles.avoidObstacles(
            enter,
            exit,
            1.0,
            obstacle
        )

        obstacle(obstacle)

        paths.forEach {
            val color = Color(random.nextInt())
            trajectory(it) { color }
        }

    }
}

@Composable
@Preview
fun singleObstacle() {
    SchemeView {
        val obstacle = Obstacle(Circle2D(Float64Space2D.vector(7.0, 1.0), 5.0))
        val enter = Pose2D(-5, -1, Angle.pi / 4)
        val exit = Pose2D(20, 4, Angle.pi * 3 / 4)

        pose(enter)
        pose(exit)
        obstacle(obstacle)

        Obstacles.avoidObstacles(
            enter,
            exit,
            0.5,
            obstacle
        ).forEach {
            val color = Color(random.nextInt())
            trajectory(it) { color }
        }
    }
}

@Composable
@Preview
fun doubleObstacle() = with(Float64Space2D){
    SchemeView {
        val obstacles = arrayOf(
            Obstacle(
                Circle2D(vector(1.0, 6.5), 0.5),
                Circle2D(vector(2.0, 1.0), 0.5),
                Circle2D(vector(6.0, 0.0), 0.5),
                Circle2D(vector(5.0, 5.0), 0.5)
            ), Obstacle(
                Circle2D(vector(10.0, 1.0), 0.5),
                Circle2D(vector(16.0, 0.0), 0.5),
                Circle2D(vector(14.0, 6.0), 0.5),
                Circle2D(vector(9.0, 4.0), 0.5)
            )
        )

        obstacles.forEach { obstacle(it) }
        val enter = Pose2D(-5, -1, Angle.pi / 4)
        val exit = Pose2D(20, 4, Angle.pi * 3 / 4)
        pose(enter)
        pose(exit)

        Obstacles.avoidObstacles(
            enter,
            exit,
            0.5,
            *obstacles
        ).forEach {
            val color = Color(random.nextInt())
            trajectory(it) { color }
        }
    }
}

@Composable
@Preview
fun singleElement() {
    SchemeView {
        points(listOf(XY(1f,1f)))
    }
}


@Composable
@Preview
fun playground() {
    val examples = listOf(
        "Close starting points",
        "Single obstacle",
        "Two obstacles",
        "Single element"
    )

    var currentExample by remember { mutableStateOf(examples.first()) }

    Scaffold(floatingActionButton = {
        Column {
            examples.forEach {
                Button(onClick = { currentExample = it }) {
                    Text(it)
                }
            }
        }
    }) {
        when (currentExample) {
            examples[0] -> closePoints()
            examples[1] -> singleObstacle()
            examples[2] -> doubleObstacle()
            examples[3] -> singleElement()
        }
    }
}

fun main() = application {
    Window(title = "Trajectory-playground", onCloseRequest = ::exitApplication) {
        MaterialTheme {
            playground()
        }
    }
}
