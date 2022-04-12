package com.flydog.connectany.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.flydog.connectany.R
import com.flydog.connectany.service.ConnectService

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        startForegroundService(Intent(this, ConnectService::class.java))
    }
}