package com.flydog.connectany.activity

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flydog.connectany.R
import com.flydog.connectany.service.ConnectService
import com.flydog.connectany.service.DeviceS
import com.flydog.connectany.utils.DeviceListAdapter

class SelectDeviceActivity : AppCompatActivity() {
    private var devices = mapOf<String, DeviceS>()
    private var selectDeviceId = ""
    private var sendText = ""

    private var connectService: ConnectService? = null
    private var isBind = false
    private var conn = object: ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            isBind = true
            val myBinder = p1 as ConnectService.MsgBinder
            connectService = myBinder.getService()
            devices = connectService?.getDevices()!!
            updateViewData()
            Log.w("connectService", "bind service")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBind = false
            Log.w("connectService", "unBind service")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivty_select_device)
        bindService(Intent(this, ConnectService::class.java), conn, BIND_AUTO_CREATE)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    //this.connectService?.sendMessage()
                    Log.w("info", "send text")
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        this.sendText = it

                    }
                }
            }
        }

    }

    private fun toastInfo(info: String) {
        this.runOnUiThread {
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getChipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val data = clipboard.primaryClip
        return if (data == null || data.itemCount <= 0) {
            ""
        } else {
            val item = data.getItemAt(0)
            if (item == null || item.text == null) {
                return ""
            }
            item.text.toString()
        }
    }

    private fun updateViewData() {
        val recyclerView = findViewById<RecyclerView>(R.id.selectListView)
        val dataSet = devices.values.toList()
        val adapter = DeviceListAdapter(dataSet, object: DeviceListAdapter.ClickListener {
            override fun onItmeClick(position: Int, v: View?) {
                selectDeviceId = dataSet[position].deviceId
                if (sendText == "") {
                    sendText = getChipboardText()
                }
                connectService?.sendMessage(selectDeviceId, sendText)
                toastInfo("正在发送")
                finish()
            }
            override fun onItmeLongClick(position: Int, v: View?) {
            }
        })
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}