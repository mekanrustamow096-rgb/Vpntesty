package com.vpntester.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ConfigAdapter(private val items: MutableList<VpnConfig>) :
    RecyclerView.Adapter<ConfigAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
        val subtitle: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = "[${item.protocol.uppercase()}] ${item.name}"

        val statusText = when {
            item.latencyMs == -1L -> "Не проверено"
            item.latencyMs == -2L -> "Недоступен ✗"
            else -> "Доступен ✓  ${item.latencyMs} мс"
        }
        holder.subtitle.text = "${item.host}:${item.port}  —  $statusText"

        holder.subtitle.setTextColor(
            when {
                item.latencyMs == -2L -> Color.RED
                item.latencyMs >= 0 -> Color.parseColor("#2E7D32")
                else -> Color.GRAY
            }
        )

        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("config", item.raw))
            Toast.makeText(ctx, "Ссылка скопирована в буфер обмена", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = items.size

    fun sortByLatency() {
        items.sortWith(compareBy(
            { it.latencyMs == -2L },
            { it.latencyMs == -1L },
            { if (it.latencyMs >= 0) it.latencyMs else Long.MAX_VALUE }
        ))
        notifyDataSetChanged()
    }
    }
