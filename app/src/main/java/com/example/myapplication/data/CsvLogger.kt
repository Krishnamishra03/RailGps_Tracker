package com.example.myapplication.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CsvLogger(private val context: Context) {
    private var fileWriter: FileWriter? = null
    private var csvFile: File? = null

    fun startLogging(videoFileName: String): File? {
        try {
            val directory = File(context.getExternalFilesDir(null), "Recordings")
            if (!directory.exists()) directory.mkdirs()

            val csvFileName = videoFileName.replace(".mp4", ".csv")
            csvFile = File(directory, csvFileName)
            fileWriter = FileWriter(csvFile)
            
            // Write CSV Header
            fileWriter?.append("SystemTime,GPSTimestamp,Latitude,Longitude,Accuracy\n")
            fileWriter?.flush()
            
            Log.d("CsvLogger", "Started logging to: ${csvFile?.absolutePath}")
            return csvFile
        } catch (e: IOException) {
            Log.e("CsvLogger", "Error starting CSV logger", e)
            return null
        }
    }

    fun logLocation(location: LocationData) {
        synchronized(this) {
            try {
                val systemTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(System.currentTimeMillis())
                fileWriter?.append("$systemTime,${location.timestamp},${location.latitude},${location.longitude},${location.accuracy}\n")
            } catch (e: IOException) {
                Log.e("CsvLogger", "Error writing to CSV", e)
            }
        }
    }

    fun stopLogging() {
        synchronized(this) {
            try {
                fileWriter?.flush()
                fileWriter?.close()
                fileWriter = null
                Log.d("CsvLogger", "Stopped logging to: ${csvFile?.absolutePath}")
            } catch (e: IOException) {
                Log.e("CsvLogger", "Error stopping CSV logger", e)
            }
        }
    }
}
