package com.flydog.connectany.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.flydog.connectany.utils.HttpUtils
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import okhttp3.internal.assertThreadDoesntHoldLock
import java.util.concurrent.Executors

class BackAccessibilityService : AccessibilityService() {
    private val hostIp: MutableList<String> = mutableListOf();
    private val executorService = Executors.newFixedThreadPool(25)
    private var finishNum = 0;
    private var startFind = true;
    private var deviceStart = false;
    private var deviceFinish = 0;
    private var devices: MutableMap<String, DeviceS> = mutableMapOf();
    private var deviceId = ""
    private var userName = ""

    private val bord = Intent("com.HostIp")

    override fun onCreate() {
        super.onCreate()

        val setting = getSharedPreferences("settings", 0);
        deviceId = setting.getString("deviceId", "").toString();
        userName = setting.getString("userName", "").toString();

        embeddedServer(Netty, port = 7677) {
            install(ContentNegotiation) {
                jackson {
                }
            }
            routing {
                get("/") {
                    call.respondText("Hello World");
                }

                get("/connect") {
                    call.respond(mapOf("deviceId" to deviceId, "userName" to userName))
                }

                get("/live") {
                    val setting = getSharedPreferences("settings", 0);
                    val deviceId = setting.getString("deviceId", "");
                    val userName = setting.getString("userName", "");
                    call.respond(mapOf("deviceId" to deviceId, "userName" to userName))
                }
            }
        }.start(wait = true)
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        if (startFind) {
            if (finishNum == 25) {
                //this.onDeviceSearchListener?.onFindIp(hostIp)
                bord.putExtra("hostIp", hostIp.toTypedArray())
                sendBroadcast(bord)
            }
            val localIp = HttpUtils().getIpAddress(this)
            executorService.execute {
                val hosts = HttpUtils().scanIp(localIp, 2, 5)
                Log.w("Info", "Start print hosts")
                hostIp.addAll(hosts)
                finishNum++;
            }
            var start = 5;
            var startArray: MutableList<Int> = mutableListOf();
            for (index in 1..24) {
                startArray.add(start)
                start += 10
            }
            for (index in startArray) {
                executorService.execute {
                    val hosts = HttpUtils().scanIp(localIp, index, index + 10)
                    Log.w("Info", "Start print hosts")
                    hostIp.addAll(hosts)
                    finishNum++;
                }
            }
        }
        if (deviceStart) {
            if (deviceFinish == hostIp.size) {
                //this.onDeviceSearchListener?.onFindDevice(devices)
            }
            for (item in hostIp) {
                executorService.execute {
                    val device = HttpUtils().sendConnect(item, deviceId, userName)
                    if (device != null) {
                        this.devices.put(item, device)
                    }
                    deviceFinish++
                }
            }
        }
    }

    fun startSearch() {
        this.startFind = true
        finishNum = 0
    }

    fun endSearch() {
        this.startFind = false
        finishNum = 0
    }

    fun deviceSearch() {
        if (finishNum == 25) {
            deviceStart = true
            deviceFinish = 0
        }
    }

    fun deviceSearchStop() {
        deviceStart = false
        deviceFinish = 0
    }

    fun getHostIp(): List<String> {
        return hostIp
    }

    fun getDevices(): Map<String, DeviceS> {
        return devices
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}