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
import center.sciprog.maps.features.*
import center.sciprog.maps.scheme.SchemeView
import center.sciprog.maps.scheme.XY
import space.kscience.kmath.geometry.Angle
import space.kscience.kmath.geometry.Circle2D
import space.kscience.kmath.geometry.DoubleVector2D
import space.kscience.kmath.geometry.Euclidean2DSpace
import space.kscience.trajectory.*
import kotlin.random.Random

private fun DoubleVector2D.toXY() = XY(x.toFloat(), y.toFloat())

fun FeatureGroup<XY>.trajectory(
    trajectory: Trajectory2D,
    colorPicker: (Trajectory2D) -> Color = { Color.Blue },
): FeatureRef<XY, FeatureGroup<XY>> = group {
    when (trajectory) {
        is StraightTrajectory2D -> line(
            aCoordinates = trajectory.begin.toXY(),
            bCoordinates = trajectory.end.toXY(),
        ).color(colorPicker(trajectory))

        is CircleTrajectory2D -> with(Euclidean2DSpace) {
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

fun FeatureGroup<XY>.obstacle(obstacle: Obstacle, colorPicker: (Trajectory2D) -> Color = { Color.Red }) {
    trajectory(obstacle.circumvention, colorPicker)
    polygon(obstacle.arcs.map { it.center.toXY() }).color(Color.Gray)
}

fun FeatureGroup<XY>.pose(pose2D: Pose2D) = with(Euclidean2DSpace){
    line(pose2D.toXY(), (pose2D + Pose2D.bearingToVector(pose2D.bearing)).toXY() )
}

@Composable
@Preview
fun closePoints() {
    SchemeView {

        val obstacle = Obstacle(
            Circle2D(Euclidean2DSpace.vector(0.0, 0.0), 1.0),
            Circle2D(Euclidean2DSpace.vector(0.0, 1.0), 1.0),
            Circle2D(Euclidean2DSpace.vector(1.0, 1.0), 1.0),
            Circle2D(Euclidean2DSpace.vector(1.0, 0.0), 1.0)
        )

        val paths: List<Trajectory2D> = Obstacles.avoidObstacles(
            Pose2D(-1, -1, Angle.pi),
            Pose2D(-1, -1, Angle.piDiv2),
            1.0,
            obstacle
        )

        obstacle(obstacle)

        trajectory(paths.first()) { Color.Green }
        trajectory(paths.last()) { Color.Magenta }

    }
}

@Composable
@Preview
fun singleObstacle() {
    SchemeView {
        val obstacle = Obstacle(Circle2D(Euclidean2DSpace.vector(7.0, 1.0), 5.0))
        obstacle(obstacle)
        Obstacles.avoidObstacles(
            Pose2D(-5, -1, Angle.pi / 4),
            Pose2D(20, 4, Angle.pi * 3 / 4),
            0.5,
            obstacle
        ).forEach {
            trajectory(it).color(Color(Random.nextInt()))
        }
    }
}

@Composable
@Preview
fun doubleObstacle() {
    SchemeView {
        val obstacles = arrayOf(
            Obstacle(
                Circle2D(Euclidean2DSpace.vector(1.0, 6.5), 0.5),
                Circle2D(Euclidean2DSpace.vector(2.0, 1.0), 0.5),
                Circle2D(Euclidean2DSpace.vector(6.0, 0.0), 0.5),
                Circle2D(Euclidean2DSpace.vector(5.0, 5.0), 0.5)
            ), Obstacle(
                Circle2D(Euclidean2DSpace.vector(10.0, 1.0), 0.5),
                Circle2D(Euclidean2DSpace.vector(16.0, 0.0), 0.5),
                Circle2D(Euclidean2DSpace.vector(14.0, 6.0), 0.5),
                Circle2D(Euclidean2DSpace.vector(9.0, 4.0), 0.5)
            )
        )

        obstacles.forEach { obstacle(it)  }
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
            trajectory(it).color(Color(Random.nextInt()))
        }
    }
}


@Composable
@Preview
fun playground() {
    val examples = listOf(
        "Close starting points",
        "Single obstacle",
        "Two obstacles",
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
