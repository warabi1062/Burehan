package com.warabi1062.burehan

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.exp

class SharpnessAnalyzer(private val contentResolver: ContentResolver) {

    data class Result(val score: Int, val variance: Double, val mse: Double)

    fun analyze(uri: Uri): Result {
        val bitmap = decodeBitmap(uri) ?: return Result(0, 0.0, 0.0)
        val result = computeSharpnessScore(bitmap)
        bitmap.recycle()
        return result
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

    private fun computeSharpnessScore(bitmap: Bitmap): Result {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        mat.release()

        val variance = computeLaplacianVariance(gray)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(7.0, 7.0), 0.0)

        val grayF = Mat()
        val blurredF = Mat()
        gray.convertTo(grayF, CvType.CV_64F)
        blurred.convertTo(blurredF, CvType.CV_64F)
        gray.release()
        blurred.release()

        val diff = Mat()
        Core.subtract(grayF, blurredF, diff)
        grayF.release()
        blurredF.release()

        val diffSq = Mat()
        Core.multiply(diff, diff, diffSq)
        diff.release()

        val mse = Core.mean(diffSq).`val`[0]
        diffSq.release()

        // midpoint=175, steepness=0.03
        val score = 100.0 / (1.0 + exp(-0.03 * (mse - 175.0)))
        return Result(score.toInt().coerceIn(0, 100), variance, mse)
    }

    private fun computeLaplacianVariance(gray: Mat): Double {
        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)

        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stddev)
        laplacian.release()

        val variance = stddev.get(0, 0)[0].let { it * it }
        mean.release()
        stddev.release()

        return variance
    }
}
