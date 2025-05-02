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
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.lang.Boolean
import java.net.URL
import kotlin.String


private fun RenderedImage.toImageBitmap(): ImageBitmap {
    // Convert RenderedImage to BufferedImage if needed
    val bufferedImage = if (this is BufferedImage) {
        this
    } else {
        val bi = BufferedImage(
            this.width,
            this.height,
            BufferedImage.TYPE_INT_ARGB
        )
        val graphics = bi.createGraphics()
        graphics.drawRenderedImage(this, null)
        graphics.dispose()
        bi
    }

    // Convert BufferedImage to Compose ImageBitmap
    return bufferedImage.toComposeImageBitmap()
}

private fun Position.toGmc(): Gmc {
    return Gmc(getOrdinate(0).degrees, getOrdinate(1).degrees)
}

public fun FeatureGroup<Gmc>.geoTiff(
    geoTiffUrl: URL,
    hints: Hints = Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE),
    id: String? = null,
): FeatureRef<Gmc, Feature<Gmc>> {
    val reader = GeoTiffReader(geoTiffUrl, hints)
    val coverage = reader.read(null)
    val crs = coverage.coordinateReferenceSystem


    val image = coverage.renderedImage.toImageBitmap()
    val envelope = coverage.envelope2D
    val rectangle: Rectangle<Gmc> = Rectangle(envelope.lowerCorner.toGmc(), envelope.upperCorner.toGmc())

    return feature(
        id,
        ScalableImageFeature<Gmc>(space, rectangle) {
            BitmapPainter(image)
        }
    )
}

