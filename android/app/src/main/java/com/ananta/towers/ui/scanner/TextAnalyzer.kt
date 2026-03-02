package com.ananta.towers.ui.scanner

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextAnalyzer(
    private val onTextDetected: (String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Stability mechanism: reduce flickering by requiring consistent frames
    private var lastDetectedText: String? = null
    private var frameCount = 0
    private val requiredFrames = 3 // Reduced to 3 for better responsiveness while still being stable
    private var nullFrameCount = 0
    private val maxNullFrames = 15 // Increased grace period to 15 frames for a more "static" feel

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            
            val imgWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
            val imgHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detected = PlateParser.parse(visionText, imgWidth, imgHeight)
                    
                    if (detected != null) {
                        nullFrameCount = 0
                        if (detected == lastDetectedText) {
                            frameCount++
                            if (frameCount >= requiredFrames) {
                                onTextDetected(detected)
                            }
                        } else {
                            lastDetectedText = detected
                            frameCount = 1
                        }
                    } else {
                        nullFrameCount++
                        if (nullFrameCount >= maxNullFrames) {
                            frameCount = 0
                            lastDetectedText = null
                            onTextDetected(null)
                        }
                    }
                }
                .addOnFailureListener {
                    onTextDetected(null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

object PlateParser {
    // Standard Indian number plate format (e.g. MH12AB1234, DL3C1234)
    private val plateRegex = Regex("[A-Z]{2}[0-9]{1,2}[A-Z]{1,2}[0-9]{4}")

    fun parse(visionText: Text, imgWidth: Int, imgHeight: Int, ignoreRect: Boolean = false): String? {
        // Area of interest: center rectangle
        val interestRect = Rect(
            (imgWidth * 0.15f).toInt(),
            (imgHeight * 0.35f).toInt(),
            (imgWidth * 0.85f).toInt(),
            (imgHeight * 0.65f).toInt()
        )
        
        for (block in visionText.textBlocks) {
            val boundingBox = block.boundingBox
            if (ignoreRect || (boundingBox != null && Rect.intersects(interestRect, boundingBox))) {
                val cleanedText = block.text.uppercase().replace("\\s".toRegex(), "")
                val match = plateRegex.find(cleanedText)
                if (match != null) {
                    return match.value
                }
            }
        }
        return null
    }
}
