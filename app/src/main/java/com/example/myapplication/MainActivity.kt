package com.example.myapplication

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.data.CsvLogger
import com.example.myapplication.data.LocationManager
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.*

import android.view.WindowManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prevent screen from sleeping while app is open
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        
        val locationManager = LocationManager(applicationContext)
        val csvLogger = CsvLogger(applicationContext)
        val viewModel = MainViewModel(locationManager, csvLogger)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
    )

    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    
    val cameraManager = remember {
        CameraManager(context, lifecycleOwner, viewModel.locationState)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (permissionState.allPermissionsGranted) {
                LaunchedEffect(Unit) {
                    viewModel.startLocationUpdates()
                }

                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            cameraManager.startCamera(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient for Status Bar readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )

                // Minimalist GPS Info Overlay (Transparent)
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        locationState?.let { location ->
                            Text(
                                text = "GPS: ${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                        if (isRecording) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "REC | LIVE LOGGING",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black,
                                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                // Bottom Controls
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                        .padding(bottom = 48.dp, top = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Recording Toggle
                        LargeFloatingActionButton(
                            onClick = {
                                if (isRecording) {
                                    cameraManager.stopRecording()
                                    viewModel.stopRecording()
                                } else {
                                    cameraManager.startRecording(
                                        onRecordingStarted = { fileName ->
                                            viewModel.startRecording(fileName)
                                        },
                                        onRecordingFinished = { file ->
                                            viewModel.setRecordedFile(file)
                                            Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Rounded.Videocam,
                                contentDescription = if (isRecording) "Stop" else "Record",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    
                    if (isRecording) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-40).dp)
                                .background(Color.Red, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REC", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

            } else {
                // Permission Request Screen
                PermissionRequestScreen(permissionState)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(permissionState: MultiplePermissionsState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Camera & Location",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This app records video with real-time GPS overlays. We need your permission to access the camera and location services.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { permissionState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Grant Access")
        }
    }
}
