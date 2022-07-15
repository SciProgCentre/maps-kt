package centre.sciprog.maps.compose

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlin.math.floor

data class TileId(
    val zoom: Int,
    val i: Int,
    val j: Int,
)

data class MapTile(
    val id: TileId,
    val image: ImageBitmap,
)

interface MapTileProvider {
    suspend fun loadTileAsync(tileIds: List<TileId>, scope: CoroutineScope, onTileLoad: (mapTile: MapTile) -> Unit)

    val tileSize: Int get() = DEFAULT_TILE_SIZE

    fun toIndex(d: Double): Int = floor(d / tileSize).toInt()

    fun toCoordinate(i: Int): Double = (i * tileSize).toDouble()

    companion object {
        const val DEFAULT_TILE_SIZE = 256
    }
}
