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
    private val items: List<InstalledApp>
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    private val checked = Prefs.getBlockedApps(contextRef)

    lateinit var contextRef: Context

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val checkbox: CheckBox = view.findViewById(R.id.appCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        contextRef = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.label
        holder.checkbox.isChecked = checked.contains(app.packageName)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checked.add(app.packageName) else checked.remove(app.packageName)
            Prefs.setBlockedApps(holder.checkbox.context, checked)
        }
    }

    override fun getItemCount() = items.size
}
