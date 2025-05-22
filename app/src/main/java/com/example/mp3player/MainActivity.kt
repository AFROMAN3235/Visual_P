package com.example.mp3player

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import com.example.mp3player.Calc3
import com.example.mp3player.R
import com.example.mp3player.mp3
import com.example.mp3player.gps

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.Calc3).setOnClickListener{
            val intent = Intent(this, Calc3::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.mp3).setOnClickListener{
            val intent = Intent(this, mp3::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.gps).setOnClickListener{
            val intent = Intent(this, gps::class.java)
            startActivity(intent)
        }

    }
}