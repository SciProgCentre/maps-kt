package center.sciprog.maps.geotools

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.Position2D
import org.geotools.referencing.CRS
import space.kscience.kmath.geometry.degrees
import space.kscience.maps.coordinates.GeodeticMapCoordinates
import space.kscience.maps.coordinates.MapProjection
import space.kscience.maps.coordinates.ProjectionCoordinates
import space.kscience.maps.coordinates.meters
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/**
 * Represents a map projection using the GeoTools library with a specified Coordinate Reference System (CRS).
 *
 * This class provides methods to convert between projection coordinates and geodetic map coordinates using the
 * specified CRS. The transformation is handled using `MathTransform` instances.
 *
 * @param crs The coordinate reference system used for the projection.
 */
@Serializable(with = GeoToolsMapProjection.Serializer::class)
public class GeoToolsMapProjection(
    public val crs: CoordinateReferenceSystem
) : MapProjection<ProjectionCoordinates> {

    private val transform: MathTransform = CRS.findMathTransform(crs, crsEPSG4326, true)
    private val inverted: MathTransform = transform.inverse()
    override fun toGeodetic(pc: ProjectionCoordinates): GeodeticMapCoordinates {
        val input = Position2D(pc.x.meters, pc.y.meters)
        val output = Position2D()
        transform.transform(input, output)
        return GeodeticMapCoordinates(output.x.degrees, output.y.degrees)
    }

    override fun toProjection(gmc: GeodeticMapCoordinates): ProjectionCoordinates {
        val input = Position2D(gmc.latitude.toDegrees().value, gmc.longitude.toDegrees().value)
        val output = Position2D()
        inverted.transform(input, output)
        return ProjectionCoordinates(output.x.meters, output.y.meters)
    }

    override fun toString(): String = crs.name.toString()

    public companion object {

        private val crsEPSG4326 by lazy { CRS.decode("EPSG:4326") }

        public val EPSG4326: GeoToolsMapProjection = GeoToolsMapProjection(crsEPSG4326)

        public fun decode(string: String): GeoToolsMapProjection {
            val crs = if (string.startsWith("file")) {
                val crsFile = Path.of(string)
                if (crsFile.extension == "wkt") {
                    CRS.parseWKT(crsFile.readText())
                } else {
                    error("Unknown CRS file: $crsFile")
                }
            } else {
                CRS.decode(string)
            }
            return GeoToolsMapProjection(crs)
        }
    }

    public object Serializer : KSerializer<GeoToolsMapProjection> {

        @Serializable
        @SerialName("geoTools")
        private class Proxy(val wkt: String)

        private val serializer = Proxy.serializer()
        override val descriptor: SerialDescriptor get() = serializer.descriptor

        override fun deserialize(
            decoder: Decoder,
        ): GeoToolsMapProjection {
            val proxy = decoder.decodeSerializableValue(serializer)
            return GeoToolsMapProjection(CRS.parseWKT(proxy.wkt))
        }

        override fun serialize(encoder: Encoder, value: GeoToolsMapProjection) {
            val proxy = Proxy(value.crs.toWKT())
            encoder.encodeSerializableValue(serializer, proxy)
        }
    }
}


