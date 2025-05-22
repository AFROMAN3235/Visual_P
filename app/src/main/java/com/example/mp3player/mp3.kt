package com.example.mp3player

import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Environment
import android.Manifest
import androidx.appcompat.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.concurrent.TimeUnit

class mp3 : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var currentTrackText: TextView
    private lateinit var trackTimeText: TextView
    private lateinit var tracksList: ListView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private var audioFiles = mutableListOf<String>()
    private var currentTrackIndex = -1
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                seekBar.progress = mediaPlayer.currentPosition
                updateTrackTimeText()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun formatTime(millis: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateTrackTimeText() {
        if (::mediaPlayer.isInitialized) {
            val current = formatTime(mediaPlayer.currentPosition)
            val total = formatTime(mediaPlayer.duration)
            trackTimeText.text = "$current / $total"
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkRealPermissionState()
        } else {
            showPermissionExplanationDialog()
        }
    }

    private fun checkRealPermissionState() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            loadAudioFiles()
        } else {
            showPermissionExplanationDialog()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission needed")
            .setMessage("Please grant permission in system settings")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mp3)

        initViews()
        setupButtons()
        setupSeekBar()
        requestPermissions()
    }

    private fun initViews() {
        seekBar = findViewById(R.id.seekBar)
        currentTrackText = findViewById(R.id.currentTrackText)
        trackTimeText = findViewById(R.id.trackTimeText)
        tracksList = findViewById(R.id.tracksListView)
        playButton = findViewById(R.id.playButton)
        pauseButton = findViewById(R.id.pauseButton)
        stopButton = findViewById(R.id.stopButton)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
    }

    private fun setupButtons() {
        playButton.setOnClickListener { playMusic() }
        pauseButton.setOnClickListener { pauseMusic() }
        stopButton.setOnClickListener { stopMusic() }

        prevButton.setOnClickListener { playPreviousTrack() }
        nextButton.setOnClickListener { playNextTrack() }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && ::mediaPlayer.isInitialized) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun loadAudioFiles() {
        audioFiles.clear()
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (!storageDir.exists()) {
            Toast.makeText(this, "Music folder not found", Toast.LENGTH_SHORT).show()
            return
        }
        scanDirectory(storageDir)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            audioFiles.map { File(it).name }
        )
        tracksList.adapter = adapter

        tracksList.setOnItemClickListener { _, _, position, _ ->
            currentTrackIndex = position
            playSelectedTrack()
        }
    }

    private fun scanDirectory(directory: File) {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    scanDirectory(file)
                } else if (file.name.endsWith(".mp3")) {
                    audioFiles.add(file.absolutePath)
                }
            }
        }
    }

    private fun playSelectedTrack() {
        if (currentTrackIndex in audioFiles.indices) {
            currentTrackText.text = File(audioFiles[currentTrackIndex]).name
            prepareMediaPlayer(audioFiles[currentTrackIndex])
        }
    }

    private fun playNextTrack() {
        if (audioFiles.isEmpty()) return

        currentTrackIndex = if (currentTrackIndex < audioFiles.size - 1) {
            currentTrackIndex + 1
        } else {
            0
        }
        playSelectedTrack()
    }

    private fun playPreviousTrack() {
        if (audioFiles.isEmpty()) return

        currentTrackIndex = if (currentTrackIndex > 0) {
            currentTrackIndex - 1
        } else {
            audioFiles.size - 1
        }
        playSelectedTrack()
    }

    private fun prepareMediaPlayer(trackPath: String) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(trackPath)
            prepareAsync()
            setOnPreparedListener {
                seekBar.max = mediaPlayer.duration
                updateTrackTimeText()
                playMusic()
            }
            setOnCompletionListener {
                playNextTrack()
            }
        }
    }

    private fun playMusic() {
        if (currentTrackIndex in audioFiles.indices) {
            if (!::mediaPlayer.isInitialized) {
                prepareMediaPlayer(audioFiles[currentTrackIndex])
            } else {
                mediaPlayer.start()
                handler.post(updateSeekBar)
            }
        } else {
            Toast.makeText(this, "No track selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseMusic() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            handler.removeCallbacks(updateSeekBar)
        }
    }

    private fun stopMusic() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
            seekBar.progress = 0
            handler.removeCallbacks(updateSeekBar)
            trackTimeText.text = "00:00 / 00:00"
        }
    }

    override fun onPause() {
        super.onPause()
        pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        handler.removeCallbacks(updateSeekBar)
    }
}