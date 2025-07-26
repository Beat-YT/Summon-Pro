package com.justjdupuis.summonpro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.justjdupuis.summonpro.api.TeslaApi
import com.justjdupuis.summonpro.databinding.FragmentVehicleListBinding
import com.justjdupuis.summonpro.utils.Carpenter

class VehicleRecyclerViewAdapter(
    private val values: List<TeslaApi.Vehicle>,
    private val onItemClick: (TeslaApi.Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentVehicleListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.binding.vehicleName.text = item.displayName.ifBlank { "Unnamed Vehicle" }
        holder.binding.vehicleDetails.text = Carpenter.decodeTeslaVin(item.vin)

        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(val binding: FragmentVehicleListBinding) :
        RecyclerView.ViewHolder(binding.root)
}
