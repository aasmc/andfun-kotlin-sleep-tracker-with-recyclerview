/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.trackmysleepquality.TextItemViewHolder
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.databinding.ListItemSleepNightBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ClassCastException

/**
 * Unique identifiers to distinguish among the types of ViewHolders
 */
private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1
/**
 * Subclass of ListAdapter to allow for efficient operations when an item or the whole list of items changes
 * First type parameter is SleepNight - item to be tracked,
 * Second type parameter is the viewHolder
 * SuperClass has one property - DiffUtilCallBack, we pass custom callBack
 * No need to store a list of SleepNight objects in a separate variable
 * No need for getItemCount method
 * Click listener is passed as a property to handle clicks on the views
 */
class SleepNightAdapter(val clickListener: SleepNightListener) : ListAdapter<DataItem, RecyclerView.ViewHolder>(SleepNightDiffUtilCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            ITEM_VIEW_TYPE_HEADER -> TextItemViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> ViewHolder.from(parent)
            else -> throw ClassCastException("Uknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                // get the SleepNight by position, use ListAdapter method
                val item = getItem(position) as DataItem.SleepNightItem
                holder.bind(item.sleepNight, clickListener)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.SleepNightItem -> ITEM_VIEW_TYPE_ITEM
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
        }
    }

    fun addHeaderAndSubmitList(list: List<SleepNight>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(DataItem.Header)
                else -> listOf(DataItem.Header) + list.map { DataItem.SleepNightItem(it) }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    /**
     * ViewHolder for our recycler view
     * Contains references to views from list_item_sleep_night
     * Constructor is private because we create the ViewHolder from method in a
     * Companion object
     */
    class ViewHolder private constructor(val binding: ListItemSleepNightBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Encapsulates the logic of binding viewHolder views to the adapter
         * to separate concerns. Now the ViewHolder knows hot to bind all its views
         * DataBinding allows to get rid of references to views in the code and place all the
         * logic in XML
         */
        fun bind(item: SleepNight, clickListener: SleepNightListener) {
            binding.sleep = item
            // pass the listener to the dataBinding
            binding.clickListener = clickListener
            // optimize to execute pending bindings right away. it's a good idea to do so in a recycler view
            binding.executePendingBindings()
        }

        companion object {
            /**
             * Encapsulates the logic of creating a viewHolder
             * Now the ViewHolder knows how to get created
             */
            fun from(parent: ViewGroup): ViewHolder {
                //create inflater from parent context
                val layoutInflater = LayoutInflater.from(parent.context)
                //Use dataBinding to inflate the layout
                val binding = ListItemSleepNightBinding.inflate(layoutInflater, parent, false)
                // inflate view from the inflater
                return ViewHolder(binding)
            }
        }
    }
}

/**
 * Subclass of DiffUtil.ItemCallback<SleepNight>
 * Needed for the RecyclerView to efficiently redraw the UI
 */
class SleepNightDiffUtilCallback : DiffUtil.ItemCallback<DataItem>() {

    /**
     * Checks if two items are the same without checking their contents
     * needed to check for example when an item is moved up or down the list
     */
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Checks if the contents of two items are the same
     * Here we use equality check because SleepNight is a data class that overrides
     * equals and hashCode
     */
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }

}

/**
 * Listener for clicks in the recycler view
 * It is passed as a property to the Adapter and then to the ViewHolder's bind method
 * ViewHolder passes the listener to the DataBinding that in turn handles clicks on the view itself
 */
class SleepNightListener(val clickListener: (sleepId: Long) -> Unit) {
    fun onClick(night: SleepNight) = clickListener(night.nightId)
}

/**
 * Class that represents an item in this adapter
 */

sealed class DataItem {
    data class SleepNightItem(val sleepNight: SleepNight) : DataItem() {
        override val id: Long = sleepNight.nightId
    }

    object Header : DataItem() {
        override val id: Long = Long.MIN_VALUE
    }

    abstract val id: Long
}
