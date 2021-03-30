package com.example.faceswap

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectorEngine() {

    private val detector: FaceDetector

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()


    init {
        val options = highAccuracyOpts
        detector = FaceDetection.getClient(options)
    }

    fun stop() {
        detector.close()
    }

    fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

}