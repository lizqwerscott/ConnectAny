package com.flydog.connectany.utils

import android.content.Context
import android.net.wifi.WifiManager
import okhttp3.OkHttpClient
import okhttp3.Request
import android.text.format.Formatter
import android.util.Log
import java.io.IOException
import java.lang.Exception
import com.eclipsesource.json.Json
import com.flydog.connectany.service.DeviceS
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.encodeUtf8
import java.util.concurrent.TimeUnit
import kotlin.time.toDuration

class HttpUtils {
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    private fun httpGet(url: String, timeout: Long = 1000): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string()
    }

    private fun httpPost(url: String, data: String, timeout: Long = 1000): String? {
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(data.encodeUtf8().toRequestBody(MEDIA_TYPE_JSON))
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string()
    }

    private fun ping(ip: String): Boolean {
        val runtime = Runtime.getRuntime()
        try {
            val process = runtime.exec("/system/bin/ping -c 1 $ip")
            val exitValue = process.waitFor()
            return exitValue == 0
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return false
    }

    fun getIpAddress(context: Context): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return if (wifiManager.isWifiEnabled) {
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress
            Formatter.formatIpAddress(ipAddress)
        } else {
            ""
        }
    }

    private fun getGateWayIp(localIp: String): String {
        val array = localIp.split(".").toMutableList()
        array[array.size - 1] = ""
        return array.joinToString(".")
    }

    fun scanIp(localIp: String, start: Int, end: Int): MutableList<String> {
        val gateWayIp = getGateWayIp(localIp)
        //Log.w("Info", "Ip:" + localIp)
        //Log.w("Info", "GateIp:" + gateWayIp)
        val hosts: MutableList<String> = mutableListOf()
        for (index in start..end) {
            val ip = gateWayIp + index
            if (ip == localIp) {
                continue
            }
            //Log.w("Info", "ping " + ip)
            if (ping(ip)) {
                Log.w("Info", "Add Ip:$ip")
                hosts.add(ip)
            }
        }
        return hosts
    }

    fun findIp(context: Context): MutableList<String> {
        val localIp = getIpAddress(context)
        val hosts1 = scanIp(localIp, 2, 4)
        var start = 5
        for (index in 0..24) {
            Thread {
                val hostsTemp = scanIp(localIp, start, start + 10)
                hosts1.addAll(hostsTemp)
            }.start()
            start += 10
        }
        return hosts1
    }

    fun sendConnect(ip: String, deviceId: String, userName: String): DeviceS? {
        val url = "http://$ip:7677/connect?name=$userName&id=$deviceId"
        val str = try {
            httpGet(url).toString()
        } catch (e: Exception) {
            ""
        }
        Log.w("Info", "ip: " + ip + "result: " + str)
        if (str == "") {
            return null
        }
        val json = Json.parse(str).asObject()
        val anotherName = json.getString("name", "-1")
        val anotherId = json.getString("device", "-1")

        var result: DeviceS? = null
        if (!anotherId.equals("-1") && !anotherName.equals("-1")) {
            if (anotherName.equals(userName)) {
                result = DeviceS(anotherId, ip)
            }
        }
        return result
    }

    fun sendLive(device: DeviceS, userName: String): Boolean {
        val url = "http://${device.hostIp}:7677/live?name=$userName&id=${device.deviceId}"
        val str = try {
            httpGet(url, 2000).toString()
        } catch (e: Exception) {
            ""
        }
        //Log.w("Info",  "sendLive: " + device.deviceId + ": " + str)
        if (str == "") {
            return false
        }
        val json = Json.parse(str).asObject()
        val anotherName = json.getString("name", "-1")
        val anotherId = json.getString("device", "-1")

        return anotherName == userName && anotherId == device.deviceId
    }

    fun sendMessage(device: DeviceS, deviceId: String, type: String, message: String): Boolean {
        val url = "http://${device.hostIp}:7677/recive"
        val data = Json.`object`().add("device", deviceId).add("type", type).add("data", message).toString()
        Log.w("info", "url: $url, data: $data")
        val str = try {
            httpPost(url, data, 2000).toString()
        } catch (e: Exception) {
            ""
        }
        if (str == "") {
            return false
        }
        Log.w("info", "result: " + str)
        val json = Json.parse(str).asObject()
        val msg = json.getInt("msg", -1)
        val result = json.getString("result", "-1")
        return msg != -1 && result != "-1" && msg == 200
    }
}