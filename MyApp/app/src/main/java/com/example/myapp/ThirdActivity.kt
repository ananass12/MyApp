package com.example.myapp

import android.content.Intent
import android.media.MediaPlayer
import android.widget.SeekBar
import android.os.Bundle
import android.widget.Button
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ThirdActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var forwardButton: Button
    private lateinit var backwardButton: Button
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var progressSeekBar: SeekBar
    private lateinit var GoToMainActivity: Button

    private var wasPlaying = false

    private val handler = Handler()        //механизм для выполнения задач в основном потоке
    private val updateSeekBar = object : Runnable {
        override fun run() {
            progressSeekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(this, 1000)           //выполнение задачи с задержкой
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_third)

        mediaPlayer = MediaPlayer.create(this, R.raw.test2)
        playButton = findViewById(R.id.button3)
        pauseButton = findViewById(R.id.button2)
        forwardButton = findViewById(R.id.button)
        backwardButton = findViewById(R.id.button4)
        volumeSeekBar = findViewById(R.id.volume)
        progressSeekBar = findViewById(R.id.peremotka)
        GoToMainActivity = findViewById(R.id.button_back)

        //обработчики событий
        playButton.setOnClickListener { mediaPlayer.start() }
        pauseButton.setOnClickListener { mediaPlayer.pause() }
        forwardButton.setOnClickListener { mediaPlayer.seekTo(mediaPlayer.currentPosition + 5000) }
        backwardButton.setOnClickListener { mediaPlayer.seekTo(mediaPlayer.currentPosition - 5000) }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100.0f                //преобразуем значение в дробное(0.0 - 1.0)
                mediaPlayer.setVolume(volume, volume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        progressSeekBar.max = mediaPlayer.duration
        progressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        handler.post(updateSeekBar)             //добавляем задачу в очередь выполнения

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    override fun onPause() {
        super.onPause()
        wasPlaying = mediaPlayer.isPlaying
        if (wasPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasPlaying) {
            mediaPlayer.start()
        }
        GoToMainActivity.setOnClickListener({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }
}