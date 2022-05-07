package com.flydog.connectany.service

import android.util.Log
import com.flydog.connectany.utils.HttpUtils

class DeviceS constructor(id: String, ip: String, p: Boolean = true) {
    var deviceId = id
    var hostIp = ip
    var livep = p

    public fun isLive(userName: String): Boolean {
        this.livep = HttpUtils().sendLive(this, userName)
        if (this.livep) {
            Log.w("Info", "device:$deviceId($hostIp) is live")
        } else {
            Log.w("Info", "device:$deviceId($hostIp) is unlive")
        }
        return this.livep
    }
}