package centre.sciprog.maps.compose

import androidx.compose.ui.graphics.ImageBitmap
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
    suspend fun loadTile(id: TileId): MapTile
    fun toIndex(d: Double): Int = floor(d / DEFAULT_TILE_SIZE).toInt()
    fun toCoordinate(i: Int): Double = (i * DEFAULT_TILE_SIZE).toDouble()

    companion object{
        const val DEFAULT_TILE_SIZE = 256
    }
}