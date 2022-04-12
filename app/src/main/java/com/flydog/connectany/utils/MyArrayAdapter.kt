package com.flydog.connectany.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.flydog.connectany.R

class MyArrayAdapter(context: Context, list: MutableList<String>): BaseAdapter() {
    private val context = context
    private val datas: MutableList<String> = list

    override fun getCount(): Int {
        return datas.size
    }

    override fun getItem(p0: Int): Any {
        return datas.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var view: View? = null
        var viewHolder: ViewHolder? = null
        if (p1 == null) {
            view = LayoutInflater.from(context).inflate(R.layout.simple_list_item, null)
            viewHolder = ViewHolder(view.findViewById<TextView>(R.id.item))
            view.setTag(viewHolder)
            viewHolder.textView.text = datas[p0]
            return view
        } else {
            view = p1
            viewHolder = view.getTag() as ViewHolder
            viewHolder.textView.text = datas[p0]
            return view
        }
    }

    inner class ViewHolder(p0: TextView) {
        val textView: TextView = p0
    }
}