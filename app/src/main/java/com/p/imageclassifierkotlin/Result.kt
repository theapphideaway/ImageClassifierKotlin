package com.p.imageclassifierkotlin

import android.graphics.RectF

class Result(val id: String?, val title: String?, val confidence: Float?, private var location: RectF?) {
    override fun toString(): String {
        var resultString = ""
        if (id != null) resultString += "[$id] "
        if (title != null) resultString += title + " "
        if (confidence != null) resultString += String.format("(%.1f%%) ", confidence * 1)
        if (location != null) resultString += location!!.toString() + " "
        return resultString.trim { it <= ' ' }
    }
}

object Keys {
    const val MODEL_PATH = "fuh_yeah_boi.tflite"
    const val LABEL_PATH = "labels.txt"
    const val INPUT_SIZE = 224
    const val MAX_RESULTS = 1
    const val DIM_BATCH_SIZE = 1
    const val DIM_PIXEL_SIZE = 3
    const val DIM_IMG_SIZE_X = 224
    const val DIM_IMG_SIZE_Y = 224

    const val IMAGE_MEAN = 128
    const val IMAGE_STD = 128.0f
}