package com.esimswitcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ESIMProfileAdapter(
    private val onSwitchClicked: (ESIMProfile) -> Unit
) : RecyclerView.Adapter<ESIMProfileAdapter.ESIMProfileViewHolder>() {

    private var profiles = listOf<ESIMProfile>()

    fun updateProfiles(newProfiles: List<ESIMProfile>) {
        profiles = newProfiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ESIMProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_esim_profile, parent, false)
        return ESIMProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ESIMProfileViewHolder, position: Int) {
        holder.bind(profiles[position])
    }

    override fun getItemCount(): Int = profiles.size

    inner class ESIMProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileNameText: TextView = itemView.findViewById(R.id.profileNameText)
        private val carrierNameText: TextView = itemView.findViewById(R.id.carrierNameText)
        private val profileIdText: TextView = itemView.findViewById(R.id.profileIdText)
        private val activeStatusText: TextView = itemView.findViewById(R.id.activeStatusText)
        private val switchButton: Button = itemView.findViewById(R.id.switchButton)

        fun bind(profile: ESIMProfile) {
            profileNameText.text = profile.displayName
            carrierNameText.text = profile.carrierName
            profileIdText.text = "ID: ${profile.getFormattedIccId()}"
            
            if (profile.isActive) {
                activeStatusText.visibility = View.VISIBLE
                activeStatusText.text = "ACTIVE"
                switchButton.isEnabled = false
                switchButton.text = "Currently Active"
                switchButton.alpha = 0.6f
            } else {
                activeStatusText.visibility = View.GONE
                switchButton.isEnabled = true
                switchButton.text = itemView.context.getString(R.string.switch_profile)
                switchButton.alpha = 1.0f
            }
            
            switchButton.setOnClickListener {
                if (!profile.isActive) {
                    onSwitchClicked(profile)
                }
            }
        }
    }
}