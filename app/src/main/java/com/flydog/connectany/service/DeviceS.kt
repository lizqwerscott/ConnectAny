package com.flydog.connectany.service

import android.util.Log
import com.flydog.connectany.utils.HttpUtils

class DeviceS constructor(id: String, ips: List<HostIp>, p: Boolean = true) {

    class HostIp constructor(ip: String, p: Boolean = true){
        var hostIp: String = ip
        var livep = p

        fun isLive(anotherId: String, deviceId: String, userName: String): Boolean {
            this.livep = HttpUtils().sendLive(this.hostIp, anotherId, deviceId, userName)
            if (this.livep) {
                Log.w("Info", "device:$deviceId($hostIp) is live")
            } else {
                Log.w("Info", "device:$deviceId($hostIp) is dead")
            }
            return this.livep
        }
    }

    var deviceId = id
    var hostIps: MutableList<HostIp> = mutableListOf()
    var livep = p

    init {
        this.hostIps.addAll(ips)
    }

    fun isLive(deviceId: String, userName: String): Boolean {
        this.livep = false
        for (hostIp in this.hostIps) {
            val tempLive = hostIp.isLive(this.deviceId, deviceId, userName)
            if (tempLive) {
                this.livep = true
            }
        }
        if (this.livep) {
            Log.w("Info", "device:$deviceId is live")
        } else {
            Log.w("Info", "device:$deviceId is dead")
        }
        return this.livep
    }

    fun getLiveIp(): String {
        var ip = ""
        for (hostIp in this.hostIps) {
            if (hostIp.livep) {
                ip = hostIp.hostIp
                break
            }
        }
        return ip
    }

    fun getAllIp(): String {
        val hostIpList: MutableList<String> = mutableListOf()
        for (hostIp in this.hostIps) {
            hostIpList.add(hostIp.hostIp)
        }
        return hostIpList.joinToString("\n")
    }

    fun addIp(addIp: String) {
        var containIp: Boolean = true
        for (ip in hostIps) {
            if (ip.hostIp == addIp) {
                containIp = false
            }
        }
        if (containIp) {
            hostIps.add(HostIp(addIp))
        }
    }

    fun addIps(addIps: List<String>) {
        for (ip in addIps) {
            addIp(ip)
        }
    }

    fun addHostIps(addIps: List<HostIp>) {
        for (ip in addIps) {
            addIp(ip.hostIp)
        }
    }
}