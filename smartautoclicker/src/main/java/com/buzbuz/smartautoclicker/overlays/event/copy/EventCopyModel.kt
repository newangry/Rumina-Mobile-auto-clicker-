/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.event.copy

import android.app.Application

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.CopyViewModel
import com.buzbuz.smartautoclicker.overlays.utils.getIconRes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [EventCopyDialog].
 *
 * @param application the Android application.
 */
class EventCopyModel(application: Application) : CopyViewModel<Event>(application) {

    /**
     * List of displayed event items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val eventList: Flow<List<EventCopyItem>?> =
        combine(repository.getAllEvents(), itemsFromCurrentContainer, searchQuery) { dbEvents, scenarioEvents, query ->
            if (query.isNullOrEmpty()) getAllItems(dbEvents, scenarioEvents) else getSearchedItems(dbEvents, query)
        }

    /**
     * Get all items with the headers.
     * @param dbEvents all actions in the database.
     * @param scenarioEvents all actions in the current event.
     * @return the complete list of action items.
     */
    private fun getAllItems(dbEvents: List<Event>, scenarioEvents: List<Event>?): List<EventCopyItem> {
        val allItems = mutableListOf<EventCopyItem>()

        // First, add the events from the current scenario
        val eventItems = scenarioEvents?.sortedBy { it.name }?.map { it.toEventItem() }?.distinct()
            ?: emptyList()
        if (eventItems.isNotEmpty()) allItems.add(EventCopyItem.HeaderItem(R.string.dialog_event_copy_header_event))
        allItems.addAll(eventItems)

        // Then, add all other events. Remove the one already in this scenario.
        val events = dbEvents
            .map { it.toEventItem() }
            .toMutableList()
            .apply {
                removeIf { allItem ->
                    eventItems.find { allItem.event!!.id == it.event!!.id || allItem == it } != null
                }
            }
            .distinct()
        if (events.isNotEmpty()) allItems.add(EventCopyItem.HeaderItem(R.string.dialog_event_copy_header_all))
        allItems.addAll(events)

        return allItems
    }

    /**
     * Get the result of the search query.
     * @param dbEvents all actions in the database.
     * @param query the current search query.
     */
    private fun getSearchedItems(dbEvents: List<Event>, query: String): List<EventCopyItem> = dbEvents
        .filter { event ->
            event.name.contains(query, true)
        }
        .map { it.toEventItem() }
        .distinct()

    /**
     * Get a copy of the provided event.
     *
     * @param event the event to get the copy of.
     */
    fun getCopyEvent(scenario: Long, event: Event): Event {
        return event.deepCopy().apply {
            priority = itemsFromCurrentContainer.value?.size ?: 0
            scenarioId = scenario
            cleanUpIds()
        }
    }

    /** @return the [EventCopyItem.EventItem] corresponding to this event. */
    private fun Event.toEventItem(): EventCopyItem.EventItem {
        val item = EventCopyItem.EventItem(name, actions!!.map { it.getIconRes() })
        item.event = this
        return item
    }

    /** Types of items in the event copy list. */
    sealed class EventCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(
            @StringRes val title: Int,
        ) : EventCopyItem()

        /**
         * Event item.
         * @param name the name of the event.
         * @param actions the icon resources for the actions of the event.
         */
        data class EventItem (
            val name: String,
            val actions: List<Int>,
        ) : EventCopyItem() {

            /** Event represented by this item. */
            var event: Event? = null
        }
    }
}