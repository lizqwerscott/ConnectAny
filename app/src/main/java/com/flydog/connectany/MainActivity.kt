package com.flydog.connectany

import android.app.AlertDialog
import android.content.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flydog.connectany.service.ConnectService
import com.flydog.connectany.service.DeviceS
import com.flydog.connectany.service.OnDeviceSearchListener
import com.flydog.connectany.utils.DeviceListAdapter
import com.flydog.connectany.utils.MyArrayAdapter
import java.util.*


class MainActivity : AppCompatActivity() {
    private var loadingProcessBar: ProgressBar? = null

    //private val executorService = Executors.newFixedThreadPool(25)
    private val hostIp: MutableList<String> = mutableListOf()
    var devices: Map<String, DeviceS> = mapOf()
    var finishNum = 0

    private var deviceID: String = ""
    private var userName: String = ""

    private var connectService: ConnectService? = null
    private var isBind = false
    private var conn = object: ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            isBind = true
            val myBinder = p1 as ConnectService.MsgBinder
            connectService = myBinder.getService()
            connectService!!.setOnDeviceSearchListener(object : OnDeviceSearchListener {
                override fun onFindHostIp(ip: MutableList<String>) {
                    Log.w("Info", "return Host")
                    hostIp.clear()
                    hostIp.addAll(ip)
                    viewAddText(ip, R.id.ipView)

                    //setProgressInvisible(false)
                    toastInfo("IP搜索完成")
                }

                override fun onConnectDevice(p0: Map<String, DeviceS>) {
                    Log.w("Info", "return device")
                    devices = p0
                    p0.forEach {
                        Log.w("Info", "ip: " + it.key + "id: " + it.value.deviceId)
                    }
                    deviceRViewAdd()

                    setProgressInvisible(false)
                    toastInfo("设备搜索完成")
                }

                override fun onDeviceListRefersh(p0: Map<String, DeviceS>) {
                    devices = p0
                    p0.forEach {
                        Log.w("Info", "ip: " + it.key + "id: " + it.value.deviceId)
                        deviceRViewAdd()
                    }

                    toastInfo("发现新设备")
                }

                override fun onDeviceLiveRefersh(p0: Map<String, DeviceS>) {
                    devices = p0
                    deviceRViewAdd()
                }
            })

            val tempDevices = connectService?.getDevices()
            if (tempDevices != null) {
                devices = tempDevices
                deviceRViewAdd()
            } else {
                Log.w("Info", "get NUll devices")
            }
            Log.w("connectService", "bind service")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBind = false
            Log.w("connectService", "unBind service")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadingProcessBar = findViewById(R.id.loadingProgressBar)

        if (loadIdName()) {
            this.showLoginDialog()
        } else {
            startForegroundService(Intent(this, ConnectService::class.java))
            //Bind connectService
            bindService(Intent(this, ConnectService::class.java), conn, Context.BIND_AUTO_CREATE)
        }

        //getInfo
        val updateInfo = findViewById<Button>(R.id.update)
        updateInfo.setOnClickListener {
            this.showLoginDialog()
        }


        setProgressInvisible(true)

        val searchButton = findViewById<Button>(R.id.searchIp)
        searchButton.setOnClickListener {
            if (this.connectService != null) {
                Log.w("Info", "connectService find Ip")
                this.connectService?.findDevice()
                setProgressInvisible(true)
                toastInfo("开始查找Ip")
            }
        }

        val searchDeviceButton = findViewById<Button>(R.id.searchDevice)
        searchDeviceButton.setOnClickListener {
            if (this.connectService != null) {
                Log.w("Info", "connectService search Device")
                this.connectService?.connectDevices()
                setProgressInvisible(true)
                toastInfo("开始连接设备")
            }
        }
        val startServiceButton = findViewById<Button>(R.id.startService)
        startServiceButton.setOnClickListener {
            if (this.connectService != null) {
                Log.w("Info", "connectService start http server")
                this.connectService?.startService()
                toastInfo("启动服务器完成")
            }
        }
        val sendMessageButton = findViewById<Button>(R.id.sendMessage)
        sendMessageButton.setOnClickListener {
            this.showSendMessageDialog()
        }

        class UpdateText : TimerTask() {
            override fun run() {
                //viewAddTxt(devices)
                //Log.w("Info", "finishNum: " + finishNum)
                if (finishNum == 25) {
                    //viewAddText(devices)
                    finishNum = 0
                }
                //Log.w("Info", "Start print device")
                //printArray(devices)
            }
        }

        val timer = Timer()
        timer.schedule(UpdateText(), 0, 2000)

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun viewAddText(arrayText: MutableList<String>, id: Int) {
        this.runOnUiThread {
            val textView = findViewById<ListView>(id)
            val adapter = MyArrayAdapter(this, arrayText)
            textView.adapter = adapter
        }
    }

    private fun deviceRViewAdd() {
        this.runOnUiThread {
            val recyclerView = findViewById<RecyclerView>(R.id.deviceRecycleView)
            val dataSet = this.devices.values.toList()
            val adapter = DeviceListAdapter(dataSet, object: DeviceListAdapter.ClickListener {
                override fun onItmeClick(position: Int, v: View?) {
                    showSendMessageDialog(dataSet[position].deviceId)
                }

                override fun onItmeLongClick(position: Int, v: View?) {
                }
            })
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun setProgressInvisible(p0: Boolean) {
        this.runOnUiThread {
            if (p0) {
                loadingProcessBar!!.visibility = View.VISIBLE
            } else {
                loadingProcessBar!!.visibility = View.INVISIBLE
            }
        }

    }

    private fun printArray(arrayText: MutableList<String>) {
        for (item in arrayText) {
            Log.w("Info", "item$item")
        }
    }

    private fun toastInfo(info: String) {
        this.runOnUiThread {
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeviceIdList(): Array<String> {
        val result = mutableListOf<String>()
        this.devices.forEach {
            result.add(it.value.deviceId)
        }
        return result.toTypedArray()
    }

    private fun updateIdName(name: String, id: String) {
        this.deviceID = id
        this.userName = name
        val settings = getSharedPreferences("settings", 0)
        val editor = settings.edit()
        editor.putString("deviceId", this.deviceID)
        editor.putString("userName", this.userName)
        editor.apply()

        val showUserName = findViewById<TextView>(R.id.showUserName)
        showUserName.text = "Name:$name"
        val showDeviceId = findViewById<TextView>(R.id.showDeviceID)
        showDeviceId.text = "Id:$id"
    }

    private fun isIdNamep(): Boolean {
        val setting = getSharedPreferences("settings", 0)
        val id = setting.getString("deviceId", "-1").toString()
        val name = setting.getString("userName", "-1").toString()
        return id != "-1" && name != "-1"
    }

    private fun loadIdName(): Boolean {
        val setting = getSharedPreferences("settings", 0)
        val id = setting.getString("deviceId", "-1").toString()
        val name = setting.getString("userName", "-1").toString()

        val showUserName = findViewById<TextView>(R.id.showUserName)
        showUserName.text = "Name:${name}"
        val showDeviceId = findViewById<TextView>(R.id.showDeviceID)
        showDeviceId.text = "Id:${id}"

        this.deviceID = id
        this.userName = name
        return id == "-1" && name == "-1"
    }

    private fun showLoginDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_sign, null)

        AlertDialog.Builder(this)
            .setTitle("登陆")
            .setView(view)
            .setPositiveButton("登陆") { _, _ ->
                val editTextDeviceId = view.findViewById<EditText>(R.id.deviceID)
                val editTextUserName = view.findViewById<EditText>(R.id.userName)

                val id = editTextDeviceId.text.toString().trim()
                val name = editTextUserName.text.toString().trim()

                if (id == "" || name == "") {
                    Toast.makeText(this, "请输入设备id和用户名, 重新登陆", Toast.LENGTH_SHORT).show()
                } else {
                    if (isIdNamep()) {
                        updateIdName(name, id)
                        startForegroundService(Intent(this, ConnectService::class.java))
                        //Bind connectService
                        bindService(Intent(this, ConnectService::class.java), conn, Context.BIND_AUTO_CREATE)
                    }
                    updateIdName(name, id)
                    Toast.makeText(this, "提交完成", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private fun showSendMessageDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_send_message, null)

        AlertDialog.Builder(this)
            .setTitle("登陆")
            .setView(view)
            .setPositiveButton("完成") { _, _ ->
                val editTextMessage = view.findViewById<EditText>(R.id.message)
                val editTextDeviceId = view.findViewById<EditText>(R.id.deviceID)

                val message = editTextMessage.text.toString().trim()
                val deviceId = editTextDeviceId.text.toString().trim()

                if (message == "" || deviceId == "") {
                    Toast.makeText(this, "请输入信息", Toast.LENGTH_SHORT).show()
                } else {
                    this.connectService?.sendMessage(deviceId, message)
                    Toast.makeText(this, "正在发送", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private fun showSendMessageDialog(deviceId: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_send_message, null)

        AlertDialog.Builder(this)
            .setTitle("登陆")
            .setView(view)
            .setPositiveButton("完成") { _, _ ->
                val editTextMessage = view.findViewById<EditText>(R.id.message)
                //val editTextDeviceId = view.findViewById<EditText>(R.id.deviceID)

                val message = editTextMessage.text.toString().trim()

                if (message == "" || deviceId == "") {
                    Toast.makeText(this, "请输入信息", Toast.LENGTH_SHORT).show()
                } else {
                    this.connectService?.sendMessage(deviceId, message)
                    Toast.makeText(this, "正在发送", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }
}