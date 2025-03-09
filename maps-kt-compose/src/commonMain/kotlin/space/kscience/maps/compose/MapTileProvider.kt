package space.kscience.maps.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import org.jetbrains.skia.Image
import kotlin.math.floor

public data class TileId(
    val zoom: Int,
    val i: Int,
    val j: Int,
)

public data class MapTile(
    val id: TileId,
    val image: Image,
)

public interface MapTileProvider {
    public fun CoroutineScope.loadTileAsync(tileId: TileId): Deferred<MapTile>

    public val tileSize: Int get() = DEFAULT_TILE_SIZE

    public fun toIndex(d: Float): Int = floor(d / tileSize).toInt()

    public fun toCoordinate(i: Int): Float = (i * tileSize).toFloat()

    public companion object {
        public const val DEFAULT_TILE_SIZE: Int = 256
    }
}
