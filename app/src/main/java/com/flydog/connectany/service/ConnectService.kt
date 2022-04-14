package com.flydog.connectany.service

import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.eclipsesource.json.Json
import com.flydog.connectany.R
import com.flydog.connectany.activity.SelectDeviceActivity
import com.flydog.connectany.utils.HttpUtils
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import java.util.concurrent.Executors

class ConnectService: Service() {
    private var localIp: String = ""
    private val hostIp: MutableList<String> = mutableListOf()
    private var finishNum = 0
    private var findStart = true
    private var deviceLiveC = true

    private val devices: MutableMap<String, DeviceS> = mutableMapOf()
    private var deviceFinishNum = 0
    private var connectStart = true

    private var deviceId: String = ""
    private var userName: String = ""

    private val executorService = Executors.newFixedThreadPool(127)
    private var onDeviceSearchListener: OnDeviceSearchListener? = null
    private val onDeviceListener = object : OnDeviceSearchListener {
        override fun onFindHostIp(ip: MutableList<String>) {
            connectDevices()
        }
        override fun onConnectDevice(p0: Map<String, DeviceS>) {
            saveDevice()
        }

        override fun onDeviceListRefersh(p0: Map<String, DeviceS>) {
            saveDevice()
        }

        override fun onDeviceLiveRefersh(p0: Map<String, DeviceS>) {
        }
    }

    private val binder = MsgBinder()

    private val service = embeddedServer(Netty, port = 7677) {
        install(ContentNegotiation) {
            jackson {
            }
        }
        routing {
            get("/") {
                call.respondText("Hello World")
            }
            get("/connect") {
                Log.w("info", "connect: " + call.request.uri)
                Log.w("info", "connect(call host): " + call.request.origin.host)

                val parameterData = call.request.queryParameters.toMap()
                parameterData.forEach {
                    Log.w("info", "connect(call str): " + it.key + ", " + it.value[0])
                }
                val ip = call.request.origin.host
                val name = parameterData["name"]?.get(0)
                val id = parameterData["id"]?.get(0)

                if (id != null) {
                    if (name == userName) {
                        if (localIp != ip) {
                            addDevice(ip, id)
                        } else {
                            Log.w("info", "ip same localIP, ktor?????")
                        }
                    } else {
                        Log.w("info", "another user name")
                    }
                }

                val json = Json.`object`().add("device", deviceId).add("name", userName).toString()
                call.respondText(json)
                //call.respond(mapOf("device" to deviceId, "name" to userName).toString())
            }
            get("/live") {
                val json = Json.`object`().add("device", deviceId).add("name", userName).toString()
                //Log.w("info", "live: " + call.request.uri)
                call.respondText(json)
                //call.respond(mapOf("device" to deviceId, "name" to userName).toString())
            }
            post("/recive") {
                val text = call.receiveText()
                Log.w("info", "recive text: $text")
                val json = Json.parse(text).asObject()
                val deviceId = json.getString("device", "-1")
                val type = json.getString("type", "-1")
                val data = json.getString("data", "-1")
                if (deviceId != "-1" && type != "-1" && data != "-1") {
                    Log.w("info", "recive($deviceId): $data")
                    sendReciveNotification(deviceId, type, data)
                    executorService.execute {
                        if (type == "text") {
                            handleText(deviceId, data)
                        }
                        if (type == "url") {
                            handleUrl(deviceId, data)
                        }
                    }
                } else {
                    Log.w("info", "recive error")
                }
                val reciveJson = Json.`object`().add("msg", 200).add("result", "recive").toString()
                call.respondText(reciveJson)
            }
        }
    }

    private fun sendReciveNotification(id: String, type: String, data: String) {
        val notificationChannelId = "connectService_id_02"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName = "提醒通知"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val notificationChannel = NotificationChannel(notificationChannelId, channelName, importance)

        notificationChannel.description = "保证可以和其他设备互相通知"
        notificationManager.createNotificationChannel(notificationChannel)

        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("连接服务")
            .setContentText("recive $id $type")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(data))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setTimeoutAfter(10000)

        notificationManager.notify(2, builder.build())
    }

    private fun handleText(id: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("recive text", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun isBilibiliPhoneShare(url: String): Boolean {
        return  url.contains("b23.tv")
    }

    private fun handleBilibiliPhoneShare(url: String): String? {
        val regex = """https://b23.tv/.*$""".toRegex()
        val result = regex.find(url)
        return result?.value
    }

    private fun handleUrl(id: String, url: String) {
        var tempUrl = url
        if (isBilibiliPhoneShare(url)) {
            Log.w("Info", "Is bilibili phone share")
            val result = handleBilibiliPhoneShare(url)
            Log.w("Info", "show bilibili phon share: $result")
            if (result != null) {
                tempUrl = result
            }
        }
        val webpage: Uri = Uri.parse(tempUrl)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Log.w("info", "error dont't have any")
        val chooseIntent = Intent.createChooser(intent, "Open with")
        chooseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(chooseIntent)
        /*
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Log.w("info", "error dont't have any")
            val chooseIntent = Intent.createChooser(intent, "Open with")
            chooseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(chooseIntent)
        }
         */
        Log.w("info", "recive: url")
    }

    override fun onCreate() {
        super.onCreate()
        Log.w("Info", "ConnectService created")

        this.localIp = HttpUtils().getIpAddress(this)
        val notification = createForegroundNotification()
        startForeground(1, notification)
        loadDevice()
        findDevice()
        deviceLivep()
        this.startService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.w("Info", "ConnectService start")
        return START_NOT_STICKY
    }

    fun setOnDeviceSearchListener(p0: OnDeviceSearchListener) {
        this.onDeviceSearchListener = p0
    }

    private fun loadDevice() {
        val datas = getSharedPreferences("data", 0)
        val json = datas.getString("devices", "-1").toString()
        if (json != "-1") {
            val data = Json.parse(json).asArray()
            data.forEach {
                val id = it.asObject().getString("id", "-1")
                val ip = it.asObject().getString("ip", "-1")
                if (id != "-1" && ip != "-1") {
                    Log.w("Info", "load devices: $id $ip")
                    addDevice(ip, id)
                }
            }
        }
    }

    private fun saveDevice() {
        val data = Json.array()
        this.devices.forEach {
            val device = it.value
            val deviceObject = Json.`object`()
            deviceObject.add("id", device.deviceId)
            deviceObject.add("ip", device.hostIp)
            deviceObject.add("livep", device.livep)
            data.add(deviceObject)
        }
        val datas = getSharedPreferences("data", 0)
        val editor = datas.edit()
        editor.putString("devices", data.toString())
        Log.w("Info", "save data: ${data.toString()}")
        editor.apply()
    }

    private fun addDevice(ip: String, id: String) {
        if (this.devices.containsKey(ip)) {
            Log.w("info", "already in devices")
        } else {
            val device = DeviceS(id, ip)
            this.devices[ip] = device
            this.onDeviceListener.onDeviceListRefersh(devices)
            this.onDeviceSearchListener?.onDeviceListRefersh(devices)
        }
    }

    private fun addDevice(device: DeviceS) {
        if (this.devices.containsKey(device.hostIp)) {
            Log.w("info", "already in devices")
        } else {
            this.devices[device.hostIp] = device
            this.onDeviceListener.onDeviceListRefersh(devices)
            this.onDeviceSearchListener?.onDeviceListRefersh(devices)
        }
    }

    fun deviceLivep() {
        executorService.execute {
            while (this.deviceLiveC) {
                this.devices.forEach {
                    val device = it.value
                    val live = device.livep
                    val result = device.isLive(userName)
                    if (live != result) {
                        this.onDeviceListener.onDeviceLiveRefersh(this.devices)
                        this.onDeviceSearchListener?.onDeviceLiveRefersh(this.devices)
                    }
                }
                Thread.sleep(1000)
            }
        }

    }

    fun findDevice() {
        executorService.execute {
            val localIp = HttpUtils().getIpAddress(this)
            var start = 2
            val startArray: MutableList<Int> = mutableListOf()
            for (index in 1..126) {
                startArray.add(start)
                start += 2
            }
            for (index in startArray) {
                executorService.execute {
                    val hosts = HttpUtils().scanIp(localIp, index, index + 1)
                    Log.w("Info", "Start print hosts")
                    hostIp.addAll(hosts)
                    finishNum++
                }
            }
            findStart = true
            executorService.execute {
                while (findStart) {
                    if (finishNum == 126) {
                        this.onDeviceSearchListener?.onFindHostIp(hostIp)
                        this.onDeviceListener.onFindHostIp(hostIp)
                        findStart = false
                        finishNum = 0
                    }
                    Log.w("Info", "finishNum: $finishNum")
                    Thread.sleep(1000 * 5)
                }
            }
        }
    }

    fun connectDevices() {
        for (ip in hostIp) {
            executorService.execute {
                val device = HttpUtils().sendConnect(ip, deviceId, userName)
                if (device != null) {
                    addDevice(device)
                    Log.w("Info", "add device: $ip")
                }
                deviceFinishNum++
            }
        }
        connectStart = true
        executorService.execute {
            while (connectStart) {
                if (deviceFinishNum == hostIp.size) {
                    this.onDeviceSearchListener?.onConnectDevice(devices)
                    this.onDeviceListener.onConnectDevice(devices)
                    connectStart = false
                    deviceFinishNum = 0
                }
                Log.w("Info", "deviceFinishNum: $deviceFinishNum")
                Thread.sleep(1000 * 5)
            }
        }
    }

    fun startService() {
        val setting = getSharedPreferences("settings", 0)
        this.deviceId = setting.getString("deviceId", "").toString()
        this.userName = setting.getString("userName", "").toString()
        executorService.execute {
            this.service.start(wait = true)
        }
    }

    fun getDevice(id: String): DeviceS? {
        var device: DeviceS? = null
        this.devices.forEach {
            if (it.value.deviceId == id) {
                device = it.value
            }
        }
        return device
    }

    fun getDevices(): Map<String, DeviceS> {
        Log.w("info", "getDevices")
        this.devices.forEach {
            Log.w("info", "getDetices(): " + it.key)
        }
        return this.devices
    }

    fun sendMessage(deviceId: String, message: String) {
        executorService.execute {
            val device = getDevice(deviceId)
            if (device != null) {
                var type = "text"
                if (message.contains("http://") || message.contains("https://")) {
                    type = "url"
                }
                HttpUtils().sendMessage(device, this.deviceId, type, message)
                Log.w("info", "send message")
            } else {
                Log.w("info", "can't find device")
            }
        }
    }

    private fun createForegroundNotification(): Notification {

        val notificationChannelId = "connectService_id_01"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelName = "后台服务"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel = NotificationChannel(notificationChannelId, channelName, importance)

        notificationChannel.description = "保证可以和其他设备互相通知"
        notificationManager.createNotificationChannel(notificationChannel)

        val norifyIntent = Intent(this, SelectDeviceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(this, 0, norifyIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, notificationChannelId)
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        builder.setContentTitle("连接服务")
        builder.setContentText("点击即可发送剪切板内容")
        builder.setContentIntent(notifyPendingIntent)

        return builder.build()
    }

    override fun onBind(p0: Intent): IBinder {
        Log.w("Info", "ConnectService bind")
        return binder
    }

    override fun onDestroy() {
        this.service.stop(1000, 1000)
        stopForeground(true)
        Log.w("Info", "ConnectService destroy")
    }

    inner class MsgBinder: Binder() {
        fun getService(): ConnectService {
            return this@ConnectService
        }
    }
}