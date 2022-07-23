package center.sciprog.maps.coordinates

public class Ellipsoid(public val equatorRadius: Distance, public val polarRadius: Distance) {
    public companion object {
        public val WGS84: Ellipsoid = Ellipsoid(
            equatorRadius = Distance(6378.137),
            polarRadius = Distance(6356.7523142)
        )
    }
}

public val Ellipsoid.f: Double get() = (equatorRadius.kilometers - polarRadius.kilometers) / equatorRadius.kilometers

public val Ellipsoid.inverseF: Double get() = equatorRadius.kilometers / (equatorRadius.kilometers - polarRadius.kilometers)