package com.warabi1062.burehan

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import kotlin.math.exp

class SharpnessAnalyzer(private val contentResolver: ContentResolver) {

    fun analyze(uri: Uri): Int {
        val bitmap = decodeBitmap(uri) ?: return 0
        val score = computeSharpnessScore(bitmap)
        bitmap.recycle()
        return score
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        val maxSide = maxOf(options.outWidth, options.outHeight)
        val targetSize = 800
        val sampleSize = maxOf(1, maxSide / targetSize)

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val scale = targetSize.toFloat() / maxOf(sampled.width, sampled.height)
        if (scale >= 1f) return sampled

        val w = (sampled.width * scale).toInt()
        val h = (sampled.height * scale).toInt()
        val resized = Bitmap.createScaledBitmap(sampled, w, h, true)
        if (resized !== sampled) sampled.recycle()
        return resized
    }

    private fun computeSharpnessScore(bitmap: Bitmap): Int {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        mat.release()

        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
        gray.release()

        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        org.opencv.core.Core.meanStdDev(laplacian, mean, stddev)
        laplacian.release()

        val variance = stddev.get(0, 0)[0].let { it * it }
        mean.release()
        stddev.release()

        return varianceToScore(variance)
    }

    private fun varianceToScore(variance: Double): Int {
        // Sigmoid mapping: midpoint=500, steepness=0.008
        val score = 100.0 / (1.0 + exp(-0.008 * (variance - 500.0)))
        return score.toInt().coerceIn(0, 100)
    }
}
