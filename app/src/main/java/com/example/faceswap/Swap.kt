package com.example.faceswap

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Subdiv2D
import org.opencv.photo.Photo
import kotlin.math.abs

/**
 * Class for swapping faces in images.
 *
 * @author alex011235
 */
class Swap {

    companion object {

        private const val TAG = "Swap"
        private var openCvInitialized = true

        init {
            if (!OpenCVLoader.initDebug()) {
                Log.i(TAG, "Could not load OpenCV library.")
                openCvInitialized = false
            }
        }

        fun faceSwapAll(
            bmp1: Bitmap, bmp2: Bitmap, landmarksForFaces1: ArrayList<ArrayList<PointF>>,
            landmarksForFaces2: ArrayList<ArrayList<PointF>>
        ): Bitmap {

            if (openCvInitialized) {
                Log.d(TAG, "OpenCV was loaded successfully!")

                val img1 = Mat()
                val img2 = Mat()

                Utils.bitmapToMat(bmp1, img1)
                Utils.bitmapToMat(bmp2, img2)

                Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGRA2BGR)
                Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGRA2BGR)

                val clonedMat = swapSingle(img1, img2, landmarksForFaces1[0], landmarksForFaces2[0])
                val bmResult = Bitmap.createBitmap(bmp2)
                Utils.matToBitmap(clonedMat, bmResult)
                return bmResult
            }
            return bmp2
        }

        private fun swapSingle(
            img1: Mat,
            img2: Mat,
            points1: ArrayList<PointF>,
            points2: ArrayList<PointF>
        ): Mat {
            Log.d(TAG, "Swapping a single face.")

            val img1Warped = img2.clone()

            // Convert Mat to float data type
            img1.convertTo(img1, CvType.CV_32F)
            img1Warped.convertTo(img1Warped, CvType.CV_32F)

            val img11 = Mat()
            val img22 = Mat()
            img1.convertTo(img11, CvType.CV_8UC3)
            img2.convertTo(img22, CvType.CV_8UC3)

            // Convert to OpenCv points
            val srcPoints2 = fromArrayList(points2)
            val matOfPoint2 = MatOfPoint()
            matOfPoint2.fromArray(*srcPoints2)
            val hullIndex = MatOfInt()

            // Find convex hull
            Imgproc.convexHull(matOfPoint2, hullIndex, false)

            val hull1 = ArrayList<Point>()
            val hull2 = ArrayList<Point>()
            val hullInd = hullIndex.toArray()

            for (value in hullInd) {
                val ptf1 = points1[value]
                val ptf2 = points2[value]
                hull1.add(Point(ptf1.x.toDouble(), ptf1.y.toDouble()))
                hull2.add(Point(ptf2.x.toDouble(), ptf2.y.toDouble()))
            }

            // Find Delaunay triangulation for points in the convex hull
            val rect = Rect(0, 0, img1Warped.cols(), img1Warped.rows())
            val dt = calculateDelaunayTriangles(rect, hull2)

            // Apply affine transformation to Delaunay triangles
            for (i in 0 until dt.size) {
                val t1 = ArrayList<Point>()
                val t2 = ArrayList<Point>()

                for (j in 0 until 3) {
                    t1.add(hull1[dt[i][j]])
                    t2.add(hull2[dt[i][j]])
                }
                // Warp triangles
                warpTriangle(img1, img1Warped, t1, t2)
            }

            // Calculate mask
            val mask = Mat.zeros(img2.rows(), img2.cols(), img2.depth())
            val hull8u = MatOfPoint()
            hull8u.fromList(hull2)
            Imgproc.fillConvexPoly(mask, hull8u, Scalar(255.0, 255.0, 255.0))

            // Clone seamlessly
            val r = Imgproc.boundingRect(hull8u)
            img1Warped.convertTo(img1Warped, CvType.CV_8UC3)
            val img1WarpedSub = img1Warped.submat(r)
            val img2Sub = img2.submat(r)
            val masksub = mask.submat(r)

            val center = Point(r.width / 2.0, r.height / 2.0)
            val output = Mat()
            Photo.seamlessClone(img1WarpedSub, img2Sub, masksub, center, output, Photo.NORMAL_CLONE)
            output.copyTo(img2.submat(r))

            img2.convertTo(img2, CvType.CV_8UC3)
            return img2
        }

        /**
         * Calculates and returns the Delaunay triangulation.
         */
        private fun calculateDelaunayTriangles(
            rect: Rect,
            hull: ArrayList<Point>
        ): ArrayList<ArrayList<Int>> {
            Log.d(TAG, "Calculate Delaunay triangles.")

            val delaunayTri = ArrayList<ArrayList<Int>>()
            val subdiv = Subdiv2D(rect)

            for (pt in hull) {
                subdiv.insert(pt)
            }

            val triangleList = MatOfFloat6()
            subdiv.getTriangleList(triangleList)


            val cnt: Int = triangleList.rows()
            val buff = FloatArray(cnt * 6)
            triangleList.get(0, 0, buff)


            for (i in 0 until cnt) {
                val pt = arrayListOf(Point(0.0, 0.0), Point(0.0, 0.0), Point(0.0, 0.0))
                val ind = arrayListOf(0, 0, 0)
                pt[0] = Point(buff[6 * i + 0].toDouble(), buff[6 * i + 1].toDouble())
                pt[1] = Point(buff[6 * i + 2].toDouble(), buff[6 * i + 3].toDouble())
                pt[2] = Point(buff[6 * i + 4].toDouble(), buff[6 * i + 5].toDouble())

                if (rect.contains(pt[0]) && rect.contains(pt[1]) && rect.contains(pt[2])) {

                    for (j in 0 until 3) {
                        for (k in 0 until hull.size) {
                            if (abs(pt[j].x - hull[k].x) < 1.0 && abs(pt[j].y - hull[k].y) < 1.0) {
                                ind[j] = k
                            }
                        }
                    }
                    delaunayTri.add(ind)
                }
            }

            return delaunayTri
        }

        private fun fromArrayList(alist: ArrayList<PointF>): Array<Point?> {
            val arr = arrayOfNulls<Point>(alist.size)
            for ((index, value) in alist.withIndex()) {
                arr[index] = Point(value.x.toDouble(), value.y.toDouble())
            }
            return arr
        }


        /**
         * Warps triangle t1 piece of image in image img1 onto triangle t2 in img2 using an
         * affine transform.
         *
         * @param img1 Contains the piece of image that will be warped with t1.
         * @param img2 Contains the piece/patch that the piece in img will be fitted in.
         * @param t1 triangle for img1.
         * @param t2 triangle for img2.
         */
        private fun warpTriangle(
            img1: Mat, img2: Mat, t1: ArrayList<Point>, t2: ArrayList<Point>
        ) {
            Log.d(TAG, "Warping triangles.")

            val m1 = MatOfPoint(t1[0], t1[1], t1[2])
            val m2 = MatOfPoint(t2[0], t2[1], t2[2])

            val r1 = Imgproc.boundingRect(m1)
            val r2 = Imgproc.boundingRect(m2)

            val t1Rect = ArrayList<Point>()
            val t2Rect = ArrayList<Point>()
            val t2RectInt = ArrayList<Point>()

            for (i in 0 until 3) {
                t1Rect.add(Point(t1[i].x - r1.x, t1[i].y - r1.y))
                t2Rect.add(Point(t2[i].x - r2.x, t2[i].y - r2.y))
                t2RectInt.add(Point(t2[i].x - r2.x, t2[i].y - r2.y))
            }

            val mask = Mat.zeros(r2.height, r2.width, img1.type())
            val t2RectIntMatofPt = MatOfPoint(t2RectInt[0], t2RectInt[1], t2RectInt[2])
            Imgproc.fillConvexPoly(mask, t2RectIntMatofPt, Scalar(1.0, 1.0, 1.0), 16, 0)

            // Apply warpImage to small rectangular patches
            val img1Rect = Mat()
            img1.submat(r1).copyTo(img1Rect)

            var img2Rect = Mat.zeros(r2.height, r2.width, img1Rect.type())

            // Apply affine transform
            img2Rect = applyAffineTransform(img2Rect, img1Rect, t1Rect, t2Rect)

            img2Rect.convertTo(img2Rect, CvType.CV_32FC3)
            Core.multiply(img2Rect, mask, img2Rect)
            val dst = Mat()
            Core.subtract(mask, Scalar(1.0, 1.0, 1.0), dst)
            Core.multiply(img2.submat(r2), dst, img2.submat(r2))
            Core.absdiff(img2.submat(r2), img2Rect, img2.submat(r2))
        }

        /**
         * Applies transform using the affine transform from t1Rect to t2Rect. Basically cuts
         * sub-image in warpImage and makes it fit in src. Does this by making a transform
         * of the coordinates in t1Rect to t2Rect.
         *
         * https://en.wikipedia.org/wiki/Affine_transformation">Affine transform
         *
         * @param warpImage the image that cointains the piece that will be transformed.
         * @param src onto this image.
         * @param t1Rect src coordinates.
         * @param t2Rect dts coordinates.
         */
        private fun applyAffineTransform(
            warpImage: Mat,
            src: Mat,
            t1Rect: ArrayList<Point>,
            t2Rect: ArrayList<Point>
        ): Mat {
            Log.d(TAG, "Applying affine transform.")

            val srcTri = MatOfPoint2f(t1Rect[0], t1Rect[1], t1Rect[2])
            val dstTri = MatOfPoint2f(t2Rect[0], t2Rect[1], t2Rect[2])

            // Given a pair of triangles, find the affine transform.
            val warpMat = Imgproc.getAffineTransform(srcTri, dstTri)

            Imgproc.warpAffine(
                src,
                warpImage,
                warpMat,
                warpImage.size(),
                Imgproc.INTER_LINEAR,
                Core.BORDER_REFLECT_101
            )
            return warpImage
        }
    }
}
