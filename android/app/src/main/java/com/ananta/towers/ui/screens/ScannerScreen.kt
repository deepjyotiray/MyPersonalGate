package com.ananta.towers.ui.screens

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ananta.towers.ui.scanner.PlateParser
import com.ananta.towers.ui.scanner.TextAnalyzer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun ScannerScreen(
    onVehicleDetected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var detectedPlate by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var stablePlate by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(detectedPlate) {
        if (detectedPlate != null) {
            stablePlate = detectedPlate
        }
    }

    val analyzer = remember {
        TextAnalyzer { plate ->
            if (!isAnalyzing) {
                detectedPlate = plate
            }
        }
    }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Number Plate") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        flashEnabled = !flashEnabled
                        cameraControl?.enableTorch(flashEnabled)
                    }) {
                        Icon(if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(executor, analyzer)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture
                            )
                            cameraControl = camera.cameraControl
                        } catch (exc: Exception) {
                            Log.e("ScannerScreen", "Use case binding failed", exc)
                        }
                    }, mainExecutor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanner Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val rectWidth = size.width * 0.8f
                val rectHeight = size.height * 0.2f
                val left = (size.width - rectWidth) / 2
                val top = (size.height - rectHeight) / 2

                drawRoundRect(
                    color = if (stablePlate != null) Color.Green else Color.White,
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = stablePlate != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = stablePlate ?: "",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isAnalyzing) return@IconButton
                            
                            if (stablePlate != null) {
                                onVehicleDetected(stablePlate!!)
                            } else {
                                isAnalyzing = true
                                errorMessage = null
                                
                                imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val rotation = imageProxy.imageInfo.rotationDegrees
                                            val image = InputImage.fromMediaImage(mediaImage, rotation)
                                            
                                            val imgWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                                            val imgHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

                                            recognizer.process(image)
                                                .addOnSuccessListener { visionText ->
                                                    val plate = PlateParser.parse(visionText, imgWidth, imgHeight, ignoreRect = true)
                                                    mainExecutor.execute {
                                                        isAnalyzing = false
                                                        if (plate != null) {
                                                            detectedPlate = plate
                                                            stablePlate = plate
                                                        } else {
                                                            errorMessage = "No plate detected. Try again."
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    mainExecutor.execute {
                                                        isAnalyzing = false
                                                        errorMessage = "Processing failed."
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                            mainExecutor.execute { isAnalyzing = false }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        mainExecutor.execute {
                                            isAnalyzing = false
                                            errorMessage = "Capture failed."
                                        }
                                    }
                                })
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (stablePlate != null) MaterialTheme.colorScheme.primary else Color.White, CircleShape),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                        } else {
                            if (stablePlate != null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Confirm",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                                        .border(2.dp, Color.Gray, CircleShape)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    if (stablePlate != null) "Verify and tap to confirm" else "Align number plate within the box",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall).padding(horizontal = 8.dp)
                )
            }
        }
    }
}
