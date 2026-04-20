# Project Plan

An Android app that records live video with a real-time GPS (latitude and longitude) overlay. The overlay must be burned into the final video. Additionally, it logs GPS coordinates and timestamps into a CSV file. Both files are saved in the same directory upon stopping the recording.

## Project Brief

# GPS Video Logger

A specialized utility for recording synchronized video and location data, designed for professional surveys, sports analysis, and documentation.

## Features

- **Real-time Camera Preview & GPS Tracking**: A high-performance viewfinder displaying live camera feed with a dynamic overlay of current latitude, longitude, and accuracy.
- **Video Recording with Burned-in Metadata**: Captures video with GPS coordinates rendered directly onto the frames, ensuring the location data is inseparable from the visual record.
- **Synchronized CSV Data Logging**: Simultaneously generates a structured CSV file containing high-frequency GPS coordinates and precise timestamps, perfectly synced with the video duration.
- **Unified File Export**: Automatically saves both the MP4 video and the corresponding CSV log to a dedicated local directory upon stopping the recording for easy retrieval.

## High-Level Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Camera Engine**: CameraX (utilizing `VideoCapture` and `SurfaceProcessor` for real-time frame manipulation)
- **Location Services**: Google Play Services Location (Fused Location Provider API)
- **Concurrency**: Kotlin Coroutines & Flow for asynchronous data logging and UI updates
- **Serialization**: Moshi (via KSP) for any internal data handling
- **Code Generation**: KSP (Kotlin Symbol Processing)

## Implementation Steps

### Task_1_Location_And_Permissions: Implement runtime permissions for Camera and Location, and create a LocationManager to stream GPS data using FusedLocationProviderClient.
- **Status:** COMPLETED
- **Updates:** Implemented runtime permissions for Camera and Location. Created a LocationManager using FusedLocationProviderClient to stream GPS data as a Flow. Updated MainActivity and MainViewModel to handle permissions and display real-time coordinates. Verified with a successful build.
- **Acceptance Criteria:**
  - Camera and Location permissions are requested and handled.
  - GPS coordinates (Lat, Lon, Accuracy) are available as a Flow.

### Task_2_Video_Recording_With_Overlay: Configure CameraX VideoCapture and implement a SurfaceProcessor (CameraEffect) to burn the live GPS data into the video frames.
- **Status:** COMPLETED
- **Updates:** Configured CameraX VideoCapture and implemented a SurfaceProcessor (OverlayEffect) to burn live GPS data into the video frames. Integrated Camera preview and recording controls into the Compose UI. Added video capture functionality with timestamped filenames. Verified with a successful build.
- **Acceptance Criteria:**
  - CameraX preview is functional.
  - Video recording produces an MP4 with GPS text burned into the frames.

### Task_3_UI_And_CSV_Logging: Implement a CSV logger for synchronized data capture, and build the Material 3 UI with Edge-to-Edge display, custom theme, and adaptive icon.
- **Status:** COMPLETED
- **Updates:** Implemented a CSV logger for synchronized data capture. Built the Material 3 UI with Edge-to-Edge display, custom theme, and adaptive icon. Integrated CSV logging with the recording state. Verified with a successful build.
- **Acceptance Criteria:**
  - CSV file is saved alongside the video on stop.
  - Material 3 UI includes viewfinder and recording controls.
  - Edge-to-Edge display and adaptive icon are implemented.
  - Vibrant color scheme used.
- **Duration:** N/A

### Task_4_Run_And_Verify: Perform final integration testing, verify the output files, and ensure application stability.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - App builds successfully and does not crash.
  - Recorded video and CSV are synchronized.
  - All existing tests pass.
  - Critic agent verifies stability and requirement alignment.
- **StartTime:** 2026-04-08 13:52:54 IST

