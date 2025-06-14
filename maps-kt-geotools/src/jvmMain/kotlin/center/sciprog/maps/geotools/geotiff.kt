package center.sciprog.maps.geotools

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.geotools.api.geometry.Position
import org.geotools.gce.geotiff.GeoTiffReader
import org.geotools.util.factory.Hints
import space.kscience.kmath.geometry.degrees
import space.kscience.maps.coordinates.Gmc
import space.kscience.maps.features.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.InputStream
import java.lang.Boolean
import javax.media.jai.PlanarImage
import kotlin.String


private fun RenderedImage.toImageBitmap(transform: AffineTransform = AffineTransform()): ImageBitmap {
    val bufferedImage = when (this) {
        is BufferedImage -> this

        is PlanarImage -> this.asBufferedImage

        else -> {
            val bufferedImage = BufferedImage(
                this.width,
                this.height,
                BufferedImage.TYPE_INT_ARGB
            )
            val graphics = bufferedImage.createGraphics()
            graphics.drawRenderedImage(this, transform)
            graphics.dispose()
            bufferedImage
        }
    }

    // Convert BufferedImage to Compose ImageBitmap
    return bufferedImage.toComposeImageBitmap()
}

/**
 * Transform position to geodetic coordinates assuming the position already uses geodetic cooridnates
 */
private fun Position.toGmc(): Gmc = Gmc(getOrdinate(0).degrees, getOrdinate(1).degrees)

public fun FeatureBuilder<Gmc>.geoTiff(
    geoTiffStream: () -> InputStream,
    hints: Hints = Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE),
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> {
    geoTiffStream().use { stream ->
        val reader = GeoTiffReader(stream, hints)
        val coverage = reader.read(null)

        val image = coverage.renderedImage.toImageBitmap()
        val envelope = coverage.envelope2D.transform(GeoToolsMapProjection.crsEPSG4326, true)

        val rectangle: Rectangle<Gmc> = space.Rectangle(envelope.lowerCorner.toGmc(), envelope.upperCorner.toGmc())

        return feature(
            id,
            ScalableImageFeature<Gmc>(space, rectangle) {
                BitmapPainter(image)
            }
        )
    }
}

