package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.data.LocationData
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val locationState: StateFlow<LocationData?>
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val frameCounter = AtomicLong(0)
    private val isCurrentlyRecording = AtomicBoolean(false)
    private var csvOutputStream: OutputStream? = null

    // Fallback values
    private var lastLat: String = "---"
    private var lastLon: String = "---"
    private var rawLat: Double = 0.0
    private var rawLon: Double = 0.0

    // Dedicated thread for overlay rendering
    private val overlayThread = HandlerThread("CameraOverlay").apply { start() }
    private val overlayHandler = Handler(overlayThread.looper)

    @SuppressLint("MissingPermission")
    fun startCamera(previewView: androidx.camera.view.PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val textPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                style = Paint.Style.FILL
                setShadowLayer(8f, 2f, 2f, Color.BLACK)
                textAlign = Paint.Align.CENTER
            }
            
            val bgPaint = Paint().apply {
                color = Color.BLACK
                alpha = 80
                style = Paint.Style.FILL
            }

            val overlayEffect = OverlayEffect(
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                2,
                overlayHandler
            ) { exc: Throwable -> Log.e("CameraManager", "Overlay error", exc) }

            overlayEffect.setOnDrawListener { frame ->
                // DIRECTLY read the latest location from StateFlow for every frame
                val currentLocation = locationState.value
                if (currentLocation != null && currentLocation.latitude != 0.0) {
                    rawLat = currentLocation.latitude
                    rawLon = currentLocation.longitude
                    lastLat = "%.6f".format(currentLocation.latitude)
                    lastLon = "%.6f".format(currentLocation.longitude)
                }

                val canvas = frame.overlayCanvas
                val rotation = frame.rotationDegrees
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                
                val isRec = isCurrentlyRecording.get()
                val currentFrame = if (isRec) frameCounter.getAndIncrement() else 0L

                if (isRec) {
                    try {
                        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                        // Added a leading space inside quotes to force Excel to show HH:mm:ss.SSS properly
                        val csvLine = "$currentFrame,\" $timestamp\",$rawLat,$rawLon\n"
                        csvOutputStream?.write(csvLine.toByteArray())
                    } catch (e: Exception) {
                        Log.e("CameraManager", "CSV Write Error", e)
                    }
                }

                val line1 = "GPS: $lastLat, $lastLon"
                val line2 = "${if (isRec) "● REC" else "STBY"} | FRM: $currentFrame"

                canvas.save()
                
                val vW: Float
                val vH: Float

                when (rotation) {
                    90 -> {
                        canvas.translate(0f, h)
                        canvas.rotate(270f)
                        vW = h
                        vH = w
                    }
                    180 -> {
                        canvas.translate(w, h)
                        canvas.rotate(180f)
                        vW = w
                        vH = h
                    }
                    270 -> {
                        canvas.translate(w, 0f)
                        canvas.rotate(90f)
                        vW = h
                        vH = w
                    }
                    else -> {
                        vW = w
                        vH = h
                    }
                }

                val barWidth = vW * 0.90f
                val barHeight = vH * 0.15f
                val left = (vW - barWidth) / 2f
                canvas.drawRect(left, 0f, left + barWidth, barHeight, bgPaint)
                
                textPaint.textSize = vH * 0.048f
                val centerX = vW / 2f
                canvas.drawText(line1, centerX, barHeight * 0.45f, textPaint)
                canvas.drawText(line2, centerX, barHeight * 0.85f, textPaint)
                
                canvas.restore()
                true
            }

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(videoCapture!!)
                .addEffect(overlayEffect)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onRecordingStarted: (String) -> Unit, onRecordingFinished: (File) -> Unit) {
        val videoCapture = this.videoCapture ?: return
        frameCounter.set(0)

        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())
        val fileNo = getNextFileNo(datePart)
        val name = "GPS_LOG_${datePart}_${String.format(Locale.US, "%03d", fileNo)}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GPS_Logs")
            }
        }

        val csvContentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GPS_Logs")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        try {
            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }
            val csvUri = context.contentResolver.insert(contentUri, csvContentValues)
            csvOutputStream = csvUri?.let { context.contentResolver.openOutputStream(it) }
            csvOutputStream?.write("Frame,Time,Latitude,Longitude\n".toByteArray())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && csvUri != null) {
                context.contentResolver.update(csvUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Failed to create CSV", e)
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isCurrentlyRecording.set(true)
                        onRecordingStarted(name)
                    }
                    is VideoRecordEvent.Finalize -> {
                        isCurrentlyRecording.set(false)
                        closeCsv()
                    }
                }
            }
    }

    private fun getNextFileNo(datePart: String): Int {
        val prefs = context.getSharedPreferences("gps_logger_prefs", Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_date", "")
        var count = if (lastDate == datePart) prefs.getInt("last_fileno", 0) + 1 else 1
        prefs.edit().putString("last_date", datePart).putInt("last_fileno", count).apply()
        return count
    }

    private fun closeCsv() {
        try {
            csvOutputStream?.flush()
            csvOutputStream?.close()
        } catch (e: Exception) {
            Log.e("CameraManager", "CSV close error", e)
        } finally {
            csvOutputStream = null
        }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun release() {
        cameraExecutor.shutdown()
        overlayThread.quitSafely()
    }
}
