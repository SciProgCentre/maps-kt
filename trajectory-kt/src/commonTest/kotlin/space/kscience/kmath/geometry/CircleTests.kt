/*
 * Copyright 2018-2022 KMath contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package space.kscience.kmath.geometry

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircleTests {

    @Test
    fun circle() {
        val center = Euclidean2DSpace.vector(0.0, 0.0)
        val radius = 2.0
        val expectedCircumference = 12.56637
        val circle = Circle2D(center, radius)
        assertEquals(expectedCircumference, circle.circumference, 1e-4)
    }

    @Test
    fun circleIntersection() = with(Euclidean2DSpace) {
        assertTrue {
            intersectsOrInside(
                circle(0.0, 0.0, 1.0),
                circle(0.0, 0.0, 2.0)
            )
        }
        assertTrue {
            intersectsOrInside(
                circle(0.0, 1.0, 1.0),
                circle(0.0, 0.0, 1.0)
            )
        }
        assertFalse {
            intersectsOrInside(
                circle(0.0, 1.0, 1.0),
                circle(0.0, -1.1, 1.0)
            )
        }
    }

    @Test
    fun circleLineIntersection() = with(Euclidean2DSpace) {
        assertTrue {
            intersects(circle(0, 0, 1.0), segment(1, 1, -1, 1))
        }

        assertFalse {
            intersects(circle(0, 0, 1.0), segment(1, 1, 0.5, 1))
        }

        assertFalse {
            intersects(circle(0, 0, 1.0), segment(0, 0.5, 0, -0.5))
        }

        assertTrue {
            intersects(circle(1, 1, sqrt(2.0)/2), segment(1, 0, 0, 1))
        }

        assertTrue {
            intersects(circle(1, 1, 1), segment(2, 2, 0, 2))
        }

        assertTrue {
            intersects(circle(0, 0, 1), segment(1, -1, 1, 1))
        }

        assertTrue {
            intersects(circle(0, 0, 1), segment(1, 0, -1, 0))
        }

        assertFalse {
            intersects(circle(0, 0, 1), segment(1, 2, -1, 2))
        }

        assertFalse {
            intersects(circle(-1, 0, 1), segment(0, 1.05, -2, 1.0))
        }
    }

    private fun Euclidean2DSpace.oldIntersect(circle: Circle2D, segment: LineSegment2D): Boolean{
        val begin = segment.begin
        val end = segment.end
        val lengthSquared = (begin.x - end.x).pow(2) + (begin.y - end.y).pow(2)
        val b = 2 * ((begin.x - end.x) * (end.x - circle.center.x) +
                (begin.y - end.y) * (end.y - circle.center.y))
        val c = (end.x - circle.center.x).pow(2) + (end.y - circle.center.y).pow(2) -
                circle.radius.pow(2)

        val aNormalized = lengthSquared / (lengthSquared * lengthSquared + b * b + c * c)
        val bNormalized = b / (lengthSquared * lengthSquared + b * b + c * c)
        val cNormalized = c / (lengthSquared * lengthSquared + b * b + c * c)

        val d = bNormalized.pow(2.0) - 4 * aNormalized * cNormalized
        if (d < 1e-6) {
            return false
        } else {
            val t1 = (-bNormalized - d.pow(0.5)) * 0.5 / aNormalized
            val t2 = (-bNormalized + d.pow(0.5)) * 0.5 / aNormalized
            if (((0 < t1) and (t1 < 1)) or ((0 < t2) and (t2 < 1))) {
                return true
            }
        }
        return false
    }

    @Test
    fun oldCircleLineIntersection() = with(Euclidean2DSpace){
        assertTrue {
            oldIntersect(circle(0, 0, 1.1), segment(1, 1, -1, 1))
        }

        assertTrue {
            oldIntersect(circle(1, 1, sqrt(2.0)/2+0.01), segment(1, 0, 0, 1))
        }

        assertTrue {
            oldIntersect(circle(1, 1, 1.01), segment(2, 2, 0, 2))
        }

        assertTrue {
            oldIntersect(circle(0, 0, 1.01), segment(1, -1, 1, 1))
        }

        assertTrue {
            oldIntersect(circle(0, 0, 1.0), segment(2, 0, -2, 0))
        }

        assertFalse {
            oldIntersect(circle(0, 0, 1), segment(1, 2, -1, 2))
        }

        assertFalse {
            oldIntersect(circle(-1, 0, 1), segment(0, 1.05, -2, 1.0))
        }
    }
}
