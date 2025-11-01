package com.harry.shortmining.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harry.shortmining.R
import com.harry.shortmining.models.Client
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ClientAdapter(private var clientList: List<Client>,
                    private val onReLaunchClick: (Client) -> Unit ) :
    RecyclerView.Adapter<ClientAdapter.ClientViewHolder>() {

    class ClientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvClientName)
        val emailTextView: TextView = itemView.findViewById(R.id.tvClientEmail)
        val phoneTextView: TextView = itemView.findViewById(R.id.tvClientPhoneNumber)
        val addressTextView: TextView = itemView.findViewById(R.id.tvClientAddress)
        val tokenExpireTimeTextView: TextView = itemView.findViewById(R.id.tvTokenExpireTime)
        val applicationStatusTextView: TextView = itemView.findViewById(R.id.tvApplicationStatus)
        val jobTypeTextView: TextView = itemView.findViewById(R.id.tvClientJobType)
        val reLaunchButton: Button = itemView.findViewById(R.id.btnReLaunch)
        val podTextView: TextView = itemView.findViewById(R.id.tvPod)
        val errorTextView: TextView = itemView.findViewById(R.id.tvError)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client, parent, false)
        return ClientViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        val client = clientList[position]
        holder.nameTextView.text = client.clientName
        holder.emailTextView.text = client.clientEmail
        // chnage client.expireTime to readable date
        val localTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        dateFormat.timeZone = localTimeZone
        val readableExpireTime = dateFormat.format(Date(client.expireTime))
        holder.tokenExpireTimeTextView.text = "Expiry: $readableExpireTime"
        holder.applicationStatusTextView.text = "Status: ${client.status} "
        holder.errorTextView.text = "Vendor: ${client.vendor}"
        holder.podTextView.text = "POD: ${client.pod}"
        holder.phoneTextView.text = client.clientPhoneNumber
        holder.addressTextView.text = client.location
        holder.jobTypeTextView.text = client.jobType

        when (client.status) {
            "finished" -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_finished))
            "documentation" -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_finished))
            "offer-accepted" -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_finished))
            "general-questions-completed" -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_finished))
            "processing" -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_processing))
            "token_expired","system_interrupt","generic_error" -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_error))
            else -> holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.status_default))
        }

        val reLaunchButton = holder.itemView.findViewById<Button>(R.id.btnReLaunch)
        if (client.status == "system_interrupt") {
            holder.reLaunchButton.visibility = View.VISIBLE
            holder.reLaunchButton.setOnClickListener {
                onReLaunchClick(client) // Trigger callback
            }
        }else if (client.status in listOf( "finished", "token_expired","processing","documentation","offer-accepted","general-questions-completed")) {
            holder.itemView.setOnClickListener {
                onReLaunchClick(client) // Trigger callback
            }
        }

        else {
            holder.reLaunchButton.visibility = View.GONE
        }

    }

    override fun getItemCount(): Int = clientList.size

    fun updateList(newList: List<Client>) {
        clientList = newList
        notifyDataSetChanged()
    }
}