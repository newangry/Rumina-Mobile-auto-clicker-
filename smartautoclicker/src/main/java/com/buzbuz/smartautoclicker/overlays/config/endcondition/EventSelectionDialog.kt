/*
 * Copyright (C) 2022 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.endcondition

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.overlays.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.overlays.base.bindings.bind
import com.buzbuz.smartautoclicker.baseui.bindings.setEmptyText
import com.buzbuz.smartautoclicker.baseui.bindings.updateState
import com.buzbuz.smartautoclicker.domain.edition.EditedEvent

import com.google.android.material.bottomsheet.BottomSheetDialog


class EventSelectionDialog(
    context: Context,
    private val eventList: List<EditedEvent>,
    private val onEventClicked: (EditedEvent) -> Unit,
): OverlayDialogController(context, R.style.AppTheme) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Adapter for the list of events. */
    private val eventsAdapter = EndConditionEventsAdapter(::onEventSelected)

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_event_selection)
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { destroy() }
            }
        }

        viewBinding.layoutLoadableList.apply {
            setEmptyText(R.string.message_empty_event_list)
            list.apply {
                adapter = eventsAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewBinding.layoutLoadableList.updateState(eventList)
        eventsAdapter.submitList(eventList)
    }

    /**
     * Called when an event is selected in the list.
     * Notify the provided listener and dismiss the dialog.
     *
     * @param event the selected event.
     */
    private fun onEventSelected(event: EditedEvent) {
        onEventClicked(event)
        destroy()
    }
}

/**
 * Adapter for the list of events.
 * @param onEventSelected listener on user click on an event.
 */
private class EndConditionEventsAdapter(
    private val onEventSelected: (EditedEvent) -> Unit,
) : ListAdapter<EditedEvent, EndConditionEventViewHolder>(EndConditionEventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EndConditionEventViewHolder =
        EndConditionEventViewHolder(
            ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onEventSelected,
        )

    override fun onBindViewHolder(holder: EndConditionEventViewHolder, position: Int) =
        holder.onBind(getItem(position))
}

/** DiffUtil Callback comparing two EndConditionListItem when updating the [EndConditionEventsAdapter] list. */
private object EndConditionEventDiffUtilCallback: DiffUtil.ItemCallback<EditedEvent>() {
    override fun areItemsTheSame(oldItem: EditedEvent, newItem: EditedEvent): Boolean = oldItem.event.id == newItem.event.id
    override fun areContentsTheSame(oldItem: EditedEvent, newItem: EditedEvent): Boolean = oldItem == newItem
}

/**
 * ViewHolder for an Event.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param onEventSelected called when the user select an event.
 */
private class EndConditionEventViewHolder(
    private val viewBinding: ItemEventBinding,
    private val onEventSelected: (EditedEvent) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(confEvent: EditedEvent) {
        viewBinding.bind(confEvent.event, false) { onEventSelected(confEvent) }
    }
}