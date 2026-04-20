RailGPS Tracker is a native Android application built using Java and Kotlin that records high-quality 1080p video while simultaneously overlaying live GPS telemetry — including Latitude, Longitude, Speed, Accuracy, and Frame Number — directly onto the camera feed in real time.
The GPS data is permanently burned into the saved video file using Canvas and SurfaceView, so every recorded frame displays the exact location data at that moment. Alongside the video, all GPS parameters are exported frame-by-frame into a structured CSV file for further analysis.
Key Features:

📹 1080p video recording via Camera2 API
🗺️ Live GPS overlay on video feed (Lat, Long, Speed, Accuracy, Frame No.)
💾 GPS data permanently embedded in saved video
📊 Real-time CSV export of all GPS parameters
⚡ Optimized MediaRecorder pipeline for low-latency concurrent processing

Tech Stack: Java | Kotlin | Android SDK | Camera2 API | GPS & Location Services | MediaRecorder | Canvas/SurfaceView
