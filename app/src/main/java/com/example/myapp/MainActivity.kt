package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var log_tag: String = "MY_LOG_TAG"
    private lateinit var GoToSecondActivity: Button
    private lateinit var GoToThirdActivity: Button
    private lateinit var GoToFourthActivity: Button

    private lateinit var GoToSocketActivity: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        GoToSecondActivity = findViewById(R.id.button_calc)
        GoToThirdActivity = findViewById(R.id.button_mp3)
        GoToFourthActivity = findViewById(R.id.button_gps)
        GoToSocketActivity = findViewById(R.id.button_socket)
        setupClickListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        GoToSecondActivity.setOnClickListener({
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        });
        GoToThirdActivity.setOnClickListener({
            val intent = Intent(this, ThirdActivity::class.java)
            startActivity(intent)
        });
        GoToFourthActivity.setOnClickListener({
            val intent = Intent(this, ActivityGPS::class.java)
            startActivity(intent)
        });
        GoToSocketActivity.setOnClickListener({
            val intent = Intent(this, SocketActivity::class.java)
            startActivity(intent)
        });
    }

    override fun onStart() {
        super.onStart()
        Log.d(log_tag, "onStart method")
    }

    override fun onResume() {
        super.onResume()
        Log.d(log_tag, "onResume method")
        // Убрана установка обработчиков из onResume
    }

    override fun onPause() {
        super.onPause()
        Log.d(log_tag, "onPause method")
    }

    override fun onStop() {
        super.onStop()
        Log.d(log_tag, "onStop method")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(log_tag, "onDestroy method")
    }
}