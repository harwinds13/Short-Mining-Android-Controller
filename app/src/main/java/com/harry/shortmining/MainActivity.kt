package com.harry.shortmining

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var buttonEnableService: Button
    private lateinit var buttonClientViewActivity: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonEnableService = findViewById(R.id.button)
        buttonClientViewActivity = findViewById(R.id.buttonToList)
        buttonEnableService.setOnClickListener {
            startActivity(Intent(this, WebView::class.java))
        }
        buttonClientViewActivity.setOnClickListener {
            startActivity(Intent(this, ClientSheetView::class.java))
        }
    }
}