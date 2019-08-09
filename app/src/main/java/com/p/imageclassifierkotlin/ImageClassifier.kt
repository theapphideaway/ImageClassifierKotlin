package com.p.imageclassifierkotlin

import android.content.res.AssetManager
import android.graphics.Bitmap
import com.p.imageclassifierkotlin.Keys.DIM_BATCH_SIZE
import com.p.imageclassifierkotlin.Keys.DIM_IMG_SIZE_X
import com.p.imageclassifierkotlin.Keys.DIM_IMG_SIZE_Y
import com.p.imageclassifierkotlin.Keys.DIM_PIXEL_SIZE
import com.p.imageclassifierkotlin.Keys.INPUT_SIZE
import com.p.imageclassifierkotlin.Keys.LABEL_PATH
import com.p.imageclassifierkotlin.Keys.MAX_RESULTS
import com.p.imageclassifierkotlin.Keys.MODEL_PATH
import java.io.BufferedReader
import java.io.FileInputStream
import org.tensorflow.lite.Interpreter
import io.reactivex.Single
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class ImageClassifier constructor(private val assetManager: AssetManager) {

    private var interpreter: Interpreter? = null
    private var labelProb: Array<ByteArray>
    private val labels = Vector<String>()
    private val intValues by lazy { IntArray(INPUT_SIZE * INPUT_SIZE) }
    private var imgData: ByteBuffer

    init {
        try {
            val br = BufferedReader(InputStreamReader(assetManager.open(LABEL_PATH)))
            while (true) {
                val line = br.readLine() ?: break
                labels.add(line)
            }
            br.close()
        } catch (e: IOException) {
            throw RuntimeException("Problem reading label file!", e)
        }
        labelProb = Array(1) { ByteArray(labels.size) }
        imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())
        try {
            interpreter = Interpreter(loadModelFile(assetManager, MODEL_PATH))
//            var byteArray = assetManager.open(MODEL_PATH).readBytes()
//            var finalModel = ByteBuffer.wrap(byteArray)
//            interpreter = Interpreter(finalModel)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) return
        imgData!!.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues!![pixel++]
                imgData!!.put((value shr 16 and 0xFF).toByte())
                imgData!!.put((value shr 8 and 0xFF).toByte())
                imgData!!.put((value and 0xFF).toByte())

            }
        }
    }

    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun recognizeImage(bitmap: Bitmap): Single<List<Result>> {
        return Single.just(bitmap).flatMap {
            convertBitmapToByteBuffer(it)
            interpreter!!.run(imgData, labelProb)
            val pq = PriorityQueue<Result>(3,
                Comparator<Result> { lhs, rhs ->
                    // Intentionally reversed to put high confidence at the head of the queue.
                    (rhs.confidence!!).compareTo(lhs.confidence!!)
                })
            for (i in labels.indices) {
                pq.add(Result("" + i, if (labels.size > i) labels[i] else "unknown", labelProb[0][i].toFloat(), null))
            }
            val recognitions = ArrayList<Result>()
            val recognitionsSize = Math.min(pq.size, MAX_RESULTS)
            for (i in 0 until recognitionsSize) recognitions.add(pq.poll())
            return@flatMap Single.just(recognitions)
        }
    }

    fun close() {
        interpreter?.close()
    }
}