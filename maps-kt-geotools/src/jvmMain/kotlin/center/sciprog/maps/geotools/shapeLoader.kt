package center.sciprog.maps.geotools

import io.github.oshai.kotlinlogging.KotlinLogging
import org.geotools.api.data.FileDataStore
import org.geotools.api.data.FileDataStoreFinder
import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import space.kscience.kmath.geometry.degrees
import space.kscience.maps.coordinates.GeoEllipsoid
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.coordinates.GmcCurve
import space.kscience.maps.coordinates.curveBetween
import java.net.URL


internal fun SimpleFeatureCollection.asSequence(): Sequence<SimpleFeature> = sequence {
    features().use { iterator ->
        while (iterator.hasNext()) {
            yield(iterator.next())
        }
    }
}

internal val MultiLineString.lines: Sequence<LineString>
    get() = sequence {
        for (n in 0 until numGeometries) {
            yield(getGeometryN(n) as LineString)
        }
    }

internal fun readCrs(url: URL): CoordinateReferenceSystem {
    val store: FileDataStore = FileDataStoreFinder.getDataStore(url)
    val featureSource = store.featureSource
    val schema = featureSource.schema
    return schema.coordinateReferenceSystem
}

private fun Double.checkFinite(): Double {
    if (!isFinite()) error("Not finite!")
    return this
}

/**
 * Interpret this [Coordinate] as geodetic coordinates
 */
public fun Coordinate.toGmc(): Gmc = Gmc(x.checkFinite().degrees, y.checkFinite().degrees)

//TODO add other shapes

/**
 * Loads and processes shape lines from a shapefile, transforming them into geodetic curves
 * compatible with the EPSG:4326 coordinate reference system.
 *
 * @param url the URL pointing to the shapefile to be processed
 * @param crsOverride an optional parameter to override the shapefile's coordinate reference system (CRS),
 *        if null, the CRS from the shapefile schema will be used
 * @return a list of geodetic curves (GmcCurve) representing the transformed lines from the shapefile
 */
public fun GeoEllipsoid.loadShapeLines(
    url: URL,
    crsOverride: CoordinateReferenceSystem? = null
): List<GmcCurve> {
    val store: FileDataStore = FileDataStoreFinder.getDataStore(url)
    val featureSource = store.featureSource
    val schema = featureSource.schema
    //https://gis.stackexchange.com/questions/359967/how-to-parse-crs-from-shapefile-using-geotools
    val crs: CoordinateReferenceSystem = crsOverride ?: schema.coordinateReferenceSystem
    val transform: MathTransform = CRS.findMathTransform(crs, GeoToolsMapProjection.EPSG4326.crs, true)

    return featureSource.features.asSequence().mapNotNull {
        it.defaultGeometry as? Geometry
    }.filterIsInstance<MultiLineString>().flatMap { it.lines }.mapNotNull {
        val transformed: Geometry = JTS.transform(it, transform)
        val begin = transformed.coordinates[0].toGmc()
        val end = transformed.coordinates[1].toGmc()
        if (begin == end) {
            KotlinLogging.logger("PathPlanner")
                .error { "One of the lines has zero length: $begin == $end. Skipping it." }
            null
        } else {
            curveBetween(begin, end)
        }
    }.toList()
}