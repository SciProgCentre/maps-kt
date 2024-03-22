package center.sciprog.maps.features

import android.graphics.Paint


public actual class FeatureFont(
    private val paint: Paint,
) {
    public actual var size: Float = paint.textSize
        get() = paint.textSize
        set(value) {
            field = value
            paint.textSize = value
        }

}