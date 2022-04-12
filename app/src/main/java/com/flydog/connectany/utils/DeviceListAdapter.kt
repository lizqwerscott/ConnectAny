package com.flydog.connectany.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flydog.connectany.R
import com.flydog.connectany.service.DeviceS

class DeviceListAdapter(private val dataSet: List<DeviceS>, private val clickListener: ClickListener) :
    RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    public interface ClickListener {
        fun onItmeClick(position: Int, v: View?)
        fun onItmeLongClick(position: Int, v: View?)
    }

    class ViewHolder(view: View, _clickListener: ClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {
        val id : TextView
        val ip: TextView
        val live: TextView
        val clickListener: ClickListener


        init {
            id = view.findViewById(R.id.id)
            ip = view.findViewById(R.id.ip)
            live = view.findViewById(R.id.live)
            clickListener = _clickListener
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            clickListener.onItmeClick(adapterPosition, p0)
        }

        override fun onLongClick(p0: View?): Boolean {
            clickListener.onItmeLongClick(adapterPosition, p0)
            return false
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_list, parent, false)
        return  ViewHolder(view, clickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = dataSet[position]
        holder.id.text = device.deviceId
        holder.ip.text = device.hostIp
        holder.live.text = device.livep.toString()
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

}