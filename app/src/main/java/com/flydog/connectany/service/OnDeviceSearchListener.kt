package com.flydog.connectany.service

interface OnDeviceSearchListener {
    fun onFindHostIp(ip: MutableList<String>)
    fun onConnectDevice(p0: Map<String, DeviceS>)
    fun onDeviceListRefersh(p0: Map<String, DeviceS>)
    fun onDeviceLiveRefersh(p0: Map<String, DeviceS>)
}