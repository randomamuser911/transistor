/*
 * DiscoverAdapter.kt
 * Implements the DiscoverAdapter class
 * A DiscoverAdapter provides the station list of the Discover tab - results
 * come straight from the radio-browser.info directory and can be added to
 * the collection with one tap
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import org.y20k.transistor.R


/*
 * DiscoverAdapter class
 */
class DiscoverAdapter(private val listener: DiscoverAdapterListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /* Listener Interface */
    interface DiscoverAdapterListener {
        fun onDiscoverResultTapped(result: RadioBrowserResult)
    }


    /* Main class variables */
    private var results: List<RadioBrowserResult> = listOf()
    private var addedStationUuids: Set<String> = emptySet()


    /* Updates the list of results */
    fun setResults(newResults: List<RadioBrowserResult>) {
        results = newResults
        notifyDataSetChanged()
    }


    /* Updates the set of stations that are already in the collection */
    fun setAddedStationUuids(uuids: Set<String>) {
        addedStationUuids = uuids
        if (results.isNotEmpty()) notifyDataSetChanged()
    }


    /* Overrides onCreateViewHolder from RecyclerView.Adapter */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.element_discover_result, parent, false)
        return DiscoverViewHolder(v)
    }


    /* Overrides getItemCount from RecyclerView.Adapter */
    override fun getItemCount(): Int {
        return results.size
    }


    /* Overrides onBindViewHolder from RecyclerView.Adapter */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val discoverViewHolder: DiscoverViewHolder = holder as DiscoverViewHolder
        val result: RadioBrowserResult = results[position]
        // station name
        discoverViewHolder.nameView.text = result.name
        // meta line: country - codec - bitrate
        val metaBuilder: StringBuilder = StringBuilder()
        if (result.countrycode.isNotEmpty()) metaBuilder.append(result.countrycode)
        if (result.codec.isNotEmpty()) {
            if (metaBuilder.isNotEmpty()) metaBuilder.append(" - ")
            metaBuilder.append(result.codec.uppercase())
            if (result.bitrate > 0) metaBuilder.append(" ${result.bitrate} kbps")
        }
        if (result.votes > 0) {
            if (metaBuilder.isNotEmpty()) metaBuilder.append(" - ")
            metaBuilder.append("+${result.votes}")
        }
        discoverViewHolder.metaView.text = metaBuilder.toString()
        // add state: check icon when already in collection, plus icon otherwise
        val alreadyAdded: Boolean = addedStationUuids.contains(result.stationuuid)
        discoverViewHolder.addStateIcon.setImageResource(if (alreadyAdded) R.drawable.ic_check_18dp else R.drawable.ic_add_18dp)
        discoverViewHolder.addStateIcon.isVisible = true
        // tapping a result adds it to the collection
        discoverViewHolder.itemView.setOnClickListener {
            listener.onDiscoverResultTapped(result)
        }
    }


    /*
     * Inner class: ViewHolder for a discover result
     */
    private inner class DiscoverViewHolder (resultRowLayout: View): RecyclerView.ViewHolder(resultRowLayout) {
        val nameView: MaterialTextView = resultRowLayout.findViewById(R.id.discover_station_name)
        val metaView: MaterialTextView = resultRowLayout.findViewById(R.id.discover_station_meta)
        val addStateIcon: ImageView = resultRowLayout.findViewById(R.id.discover_add_state_icon)
    }
    /*
     * End of inner class
     */

}
