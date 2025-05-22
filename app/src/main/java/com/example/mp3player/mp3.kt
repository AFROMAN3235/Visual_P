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
import com.example.mp3player.R
import java.io.File

class mp3 : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var currentTrackText: TextView
    private lateinit var tracksList: ListView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button

    private var audioFiles = mutableListOf<String>()
    private var currentTrackPath: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                seekBar.progress = mediaPlayer.currentPosition
                handler.postDelayed(this, 1000)
            }
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
            .setMessage("Please grant permission in system settings:\n\n1. Open Settings\n2. Go to Apps\n3. Select this app\n4. Tap Permissions\n5. Allow 'Media files'")
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
        tracksList = findViewById(R.id.tracksListView)
        playButton = findViewById(R.id.playButton)
        pauseButton = findViewById(R.id.pauseButton)
        stopButton = findViewById(R.id.stopButton)
    }

    private fun setupButtons() {
        playButton.setOnClickListener { playMusic() }
        pauseButton.setOnClickListener { pauseMusic() }
        stopButton.setOnClickListener { stopMusic() }
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
            currentTrackPath = audioFiles[position]
            currentTrackText.text = File(currentTrackPath!!).name
            prepareMediaPlayer()
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

    private fun prepareMediaPlayer() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(currentTrackPath)
            prepareAsync()
            setOnPreparedListener {
                seekBar.max = mediaPlayer.duration
                playMusic()
            }
            setOnCompletionListener {
                stopMusic()
            }
        }
    }

    private fun playMusic() {
        if (currentTrackPath != null) {
            if (!::mediaPlayer.isInitialized) {
                prepareMediaPlayer()
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
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            pauseMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        handler.removeCallbacks(updateSeekBar)
    }
}