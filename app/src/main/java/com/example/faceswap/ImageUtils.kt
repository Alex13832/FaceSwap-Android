package com.example.faceswap

import android.graphics.*
import android.util.Log


/**
 * Utilities for working with images.
 *
 * @author alex011235
 */
class ImageUtils {

    companion object {

        private const val TAG = "ImageUtils"
        private const val MAX_IM_SIZE = 1200

        /**
         * Changes size of the input image.
         *
         * @param bm The bitmap to resize.
         * @return Scaled bitmap.
         */
        fun resizeBitmap(bm: Bitmap): Bitmap {
            Log.d(TAG, "resizeBitmap: input w:${bm.width} h:${bm.height}")

            val h = bm.height
            val w = bm.width

            val ratioWh = w.toDouble() / h.toDouble()
            val ratioHw = h.toDouble() / w.toDouble()

            val height = when {
                h < w -> (MAX_IM_SIZE * ratioHw).toInt()
                else -> MAX_IM_SIZE
            }
            val width = when {
                h > w -> (MAX_IM_SIZE * ratioWh).toInt()
                else -> MAX_IM_SIZE
            }

            Log.d(TAG, "resizeBitmap: after resize w:${width} h:${height}")
            return Bitmap.createScaledBitmap(bm, width, height, true)
        }

        /**
         * Draws landmarks on a bitmap.
         *
         * @param bitmap The image to draw landmarks on.
         * @param landmarks Landmarks to draw.
         * @return Bitmap with landmarks as circles.
         */
        fun drawLandmarksOnBitmap(
            bitmap: Bitmap, landmarks: ArrayList<ArrayList<PointF>>
        ): Bitmap? {
            val bitmapWithLandmarks = bitmap.copy(bitmap.config, true)
            val canvas = Canvas(bitmapWithLandmarks)

            for (i in 0 until landmarks.size) {
                for (j in 0 until landmarks[i].size) {
                    val cx = landmarks[i][j].x
                    val cy = landmarks[i][j].y
                    val paint = Paint()
                    paint.style = Paint.Style.FILL_AND_STROKE
                    paint.color = Color.GREEN
                    canvas.drawCircle(cx, cy, 10F, paint)
                }
            }
            return bitmapWithLandmarks
        }
    }

}