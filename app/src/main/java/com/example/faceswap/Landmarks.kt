package com.example.faceswap

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.cos
import kotlin.math.sin

/**
 * Landmarks class for handling landmarks.
 *
 * @author alex011235
 */
class Landmarks {

    companion object {

        private const val TAG = "Landmarks"
        private const val FACE_CONST = 0.4f
        private const val FACE_CONST_MOUTH = 0.15f
        private const val FACE_CONST_EYE = 0.5f
        private const val PI: Float = Math.PI.toFloat()
        private const val PI_FIFTH: Float = (Math.PI / 5.0f).toFloat()
        private const val PI_EIGHTH: Float = (Math.PI / 8.0f).toFloat()

        /**
         * Arranges the landmarks for each face in the input list.
         *
         * @param faces Faces found be FaceDetectorEngine
         */
        fun arrangeLandmarksForFaces(faces: List<Face>): ArrayList<ArrayList<PointF>> {

            Log.d(TAG, "Nbr of faces to arrange: ${faces.size}")

            val faceLandmarks = ArrayList<ArrayList<PointF>>()

            for (face in faces) {
                val landmarks = face.allLandmarks
                val lnTemp = ArrayList<PointF>()

                val faceW: Float = face.boundingBox.width().toFloat()
                val theta: Float = -1.0f * (face.headEulerAngleZ * PI / 180.0f)
                val theta90: Float = (theta + PI / 2.0f)

                Log.d(TAG, "Nbr of landmarks extracted for face ${landmarks.size}")

                var xRightEye = 0.0f
                var yRightEye = 0.0f
                var xLeftEye = 0.0f
                var yLeftEye = 0.0f
                var xMouthRight = 0.0f
                var yMouthRight = 0.0f
                var xMouthLeft = 0.0f
                var yMouthLeft = 0.0f
                var xForeHeadMid = 0.0f
                var yForeHeadMid = 0.0f
                var xForeHeadRight = 0.0f
                var yForeHeadRight = 0.0f
                var xForeHeadLeft = 0.0f
                var yForeHeadLeft = 0.0f

                for (landmark in landmarks) {

                    var x1: Float = landmark.position.x
                    var y1: Float = landmark.position.y

                    when (landmark.landmarkType) {

                        FaceLandmark.RIGHT_CHEEK -> {
                            x1 += 1.45f * FACE_CONST * faceW * cos(PI + theta)
                            y1 += FACE_CONST * faceW * sin(PI + theta)

                            xRightEye += x1
                            yRightEye += y1
                        }
                        FaceLandmark.LEFT_CHEEK -> {
                            x1 += 1.45f * FACE_CONST * faceW * cos(theta)
                            y1 += FACE_CONST * faceW * sin(theta)

                            xLeftEye += x1
                            yLeftEye += y1
                        }
                        FaceLandmark.MOUTH_RIGHT -> {
                            x1 += FACE_CONST * faceW * cos((-PI_EIGHTH) + PI + theta)
                            y1 += 0.8f * FACE_CONST * faceW * sin((-PI_EIGHTH) + PI + theta)

                            xMouthRight += x1
                            yMouthRight += y1
                        }
                        FaceLandmark.MOUTH_LEFT -> {
                            x1 += FACE_CONST * faceW * cos(PI_EIGHTH + theta)
                            y1 += 0.8f * FACE_CONST * faceW * sin(PI_EIGHTH + theta)

                            xMouthLeft += x1
                            yMouthLeft += y1
                        }
                        FaceLandmark.MOUTH_BOTTOM -> {
                            x1 += FACE_CONST_MOUTH * faceW * cos(theta90)
                            y1 += 0.8f * FACE_CONST_MOUTH * faceW * sin(theta90)

                            xMouthLeft += x1
                            xMouthRight += x1

                            yMouthLeft += y1
                            yMouthRight += y1
                        }
                        FaceLandmark.RIGHT_EYE -> {
                            x1 += 1.1f * FACE_CONST_EYE * faceW * cos(PI + PI_FIFTH + theta)
                            y1 += 0.7f * FACE_CONST_EYE * faceW * sin(PI + PI_FIFTH + theta)

                            xRightEye += x1
                            yRightEye += y1

                            xForeHeadMid += x1
                            yForeHeadMid += y1

                            xForeHeadRight += x1
                            yForeHeadRight += y1
                        }
                        FaceLandmark.LEFT_EYE -> {
                            x1 += 1.1f * FACE_CONST_EYE * faceW * cos(-PI_FIFTH + theta)
                            y1 += 0.7f * FACE_CONST_EYE * faceW * sin(-PI_FIFTH + theta)
                            xLeftEye += x1
                            yLeftEye += y1

                            xForeHeadMid += x1
                            yForeHeadMid += y1

                            xForeHeadLeft += x1
                            yForeHeadLeft += y1
                        }
                        else -> continue
                    }
                    lnTemp.add(PointF(x1, y1)) // Add landmark
                }

                // Adjusting extra points
                xForeHeadMid /= 2.0f
                yForeHeadMid = (yForeHeadMid / 2.0f - faceW * 0.07f)
                // Extra left fore head point
                yForeHeadLeft += yForeHeadMid
                xForeHeadLeft += xForeHeadMid
                // Extra fore head right point
                yForeHeadRight += yForeHeadMid
                xForeHeadRight += xForeHeadMid

                val leftDownMouth = PointF(0.51f * xMouthLeft, 0.51f * yMouthLeft)
                val rightDownMouth = PointF(0.49f * xMouthRight, 0.51f * yMouthRight)
                val foreHeadMid = PointF(xForeHeadMid, yForeHeadMid)
                val foreHeadLeft = PointF(0.51f * xForeHeadLeft, 0.48f * yForeHeadLeft)
                val foreHeadRight = PointF(0.49f * xForeHeadRight, 0.48f * yForeHeadRight)
                val rightEye = PointF(0.45f * xRightEye, 0.5f * yRightEye)
                val leftEye = PointF(0.55f * xLeftEye, 0.5f * yLeftEye)

                lnTemp.add(leftDownMouth)
                lnTemp.add(rightDownMouth)
                lnTemp.add(foreHeadMid)
                lnTemp.add(foreHeadLeft)
                lnTemp.add(foreHeadRight)
                lnTemp.add(rightEye)
                lnTemp.add(leftEye)

                Log.d(TAG, "Total landmarks for face: ${lnTemp.size}")
                faceLandmarks.add(lnTemp)
            }
            return faceLandmarks
        }
    }
}