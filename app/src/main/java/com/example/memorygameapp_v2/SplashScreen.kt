package com.example.memorygameapp_v2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

const val SPLASH_TIME = 4000L

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)
        supportActionBar!!.hide()

        Handler(Looper.myLooper()!!).postDelayed(
            {
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()
            },
            SPLASH_TIME
        )
    }
}