package com.riposo.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppPickerAdapter(
    private val items: List<InstalledApp>,
    private val onCheckedChange: () -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    private var context: Context? = null
    private var visible: List<InstalledApp> = items

    fun allApps(): List<InstalledApp> = items

    fun filter(query: String) {
        val q = query.trim().lowercase()
        visible = if (q.isEmpty()) items else items.filter { it.label.lowercase().contains(q) }
        notifyDataSetChanged()
    }

    fun refreshChecks() {
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val checkbox: CheckBox = view.findViewById(R.id.appCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = visible[position]
        val ctx = holder.checkbox.context
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.label
        holder.checkbox.isChecked = Prefs.getBlockedApps(ctx).contains(app.packageName)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            val blocked = Prefs.getBlockedApps(ctx)
            if (isChecked) blocked.add(app.packageName) else blocked.remove(app.packageName)
            Prefs.setBlockedApps(ctx, blocked)
            onCheckedChange()
        }
        holder.itemView.setOnClickListener { holder.checkbox.toggle() }
    }

    override fun getItemCount() = visible.size
}
