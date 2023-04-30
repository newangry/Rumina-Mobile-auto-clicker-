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
package com.buzbuz.smartautoclicker.overlays.config.action.click

import android.content.Context
import android.graphics.Point
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.baseui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.baseui.bindings.setItems
import com.buzbuz.smartautoclicker.baseui.bindings.setLabel
import com.buzbuz.smartautoclicker.baseui.bindings.setOnTextChangedListener
import com.buzbuz.smartautoclicker.baseui.bindings.setButtonEnabledState
import com.buzbuz.smartautoclicker.baseui.bindings.setSelectedItem
import com.buzbuz.smartautoclicker.baseui.bindings.setText
import com.buzbuz.smartautoclicker.baseui.utils.DurationInputFilter
import com.buzbuz.smartautoclicker.baseui.overlays.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogConfigActionClickBinding
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.overlays.base.utils.setError
import com.buzbuz.smartautoclicker.overlays.config.action.ClickSwipeSelectorMenu
import com.buzbuz.smartautoclicker.overlays.config.action.CoordinatesSelector

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class ClickDialog(
    context: Context,
    private val editedClick: EditedAction,
    private val onDeleteClicked: (EditedAction) -> Unit,
    private val onConfirmClicked: (EditedAction) -> Unit,
) : OverlayDialogController(context, R.style.SmartAutoClickerTheme) {

    /** The view model for this dialog. */
    private val viewModel: ClickViewModel by lazy {
        ViewModelProvider(this).get(ClickViewModel::class.java)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigActionClickBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setConfiguredClick(editedClick)

        viewBinding = DialogConfigActionClickBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_click)

                buttonDismiss.setOnClickListener { destroy() }
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onDeleteButtonClicked() }
                }
            }

            editNameLayout.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }

            editPressDurationLayout.apply {
                textField.filters = arrayOf(DurationInputFilter())
                setLabel(R.string.input_field_label_click_press_duration)
                setOnTextChangedListener {
                    viewModel.setPressDuration(if (it.isNotEmpty()) it.toString().toLong() else null)
                }
            }

            clickPositionField.setItems(
                label = context.getString(R.string.dropdown_label_click_position_type),
                items = viewModel.clickTypeItems,
                onItemSelected = viewModel::setClickOnCondition,
            )

            onPositionSelectButton.setOnClickListener { showPositionSelector() }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.name.collect(::updateClickName) }
                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.pressDuration.collect(::updateClickDuration) }
                launch { viewModel.pressDurationError.collect(viewBinding.editPressDurationLayout::setError)}
                launch { viewModel.clickOnCondition.collect(::updateClickType) }
                launch { viewModel.position.collect(::updateClickOnPositionButtonText) }
                launch { viewModel.isValidAction.collect(::updateSaveButton) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        onConfirmClicked(viewModel.getConfiguredClick())
        destroy()
    }

    private fun onDeleteButtonClicked() {
        onDeleteClicked(editedClick)
        destroy()
    }

    private fun updateClickName(newName: String?) {
        viewBinding.editNameLayout.setText(newName)
    }

    private fun updateClickDuration(newDuration: String?) {
        viewBinding.editPressDurationLayout.setText(newDuration)
    }

    private fun updateClickType(newType: DropdownItem) {
        viewBinding.apply {
            clickPositionField.setSelectedItem(newType)

            when (newType) {
                viewModel.clickTypeItemOnCondition -> {
                    onPositionSelectButton.isEnabled = false
                }
                viewModel.clickTypeItemOnPosition -> {
                    onPositionSelectButton.isEnabled = true
                    onPositionSelectButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateClickOnPositionButtonText(position: Point?) {
        if (position == null) {
            viewBinding.onPositionSelectButton.setText(R.string.button_text_click_position_select)
            return
        }

        viewBinding.onPositionSelectButton.text = context.getString(
            R.string.item_desc_click_on_position,
            position.x,
            position.y
        )
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }

    private fun showPositionSelector() {
        showSubOverlay(
            overlayController = ClickSwipeSelectorMenu(
                context = context,
                selector = CoordinatesSelector.One(),
                onCoordinatesSelected = { selector ->
                    (selector as CoordinatesSelector.One).coordinates?.let { viewModel.setPosition(it) }
                },
            ),
            hideCurrent = true,
        )
    }
}