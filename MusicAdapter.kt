package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private val items: List<Song>,
    private val onClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textTitle)
        val artist: TextView = itemView.findViewById(R.id.textArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.title.text = s.title
        holder.artist.text = s.artist

        // Simple modern “3D-ish” card effect on hover/press
        holder.itemView.apply {
            cameraDistance = 8000f
            scaleX = 0.98f
            scaleY = 0.98f
            elevation = 6f

            setOnTouchListener { v, event ->
                // brief tilt animation
                v.animate()
                    .rotationY(6f)
                    .translationZ(12f)
                    .setDuration(120)
                    .withEndAction {
                        v.animate().rotationY(0f).translationZ(0f).setDuration(160).start()
                    }.start()
                false
            }

            setOnClickListener {
                onClick(s, holder.bindingAdapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}