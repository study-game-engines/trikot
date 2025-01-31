package com.mirego.trikot.viewmodels.declarative.compose.viewmodel

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxColors
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mirego.trikot.viewmodels.declarative.components.VMDToggleViewModel
import com.mirego.trikot.viewmodels.declarative.components.factory.VMDComponents
import com.mirego.trikot.viewmodels.declarative.compose.extensions.hidden
import com.mirego.trikot.viewmodels.declarative.compose.extensions.isOverridingAlpha
import com.mirego.trikot.viewmodels.declarative.compose.extensions.observeAsState
import com.mirego.trikot.viewmodels.declarative.content.VMDContent
import com.mirego.trikot.viewmodels.declarative.content.VMDNoContent
import kotlinx.coroutines.MainScope

@Composable
fun VMDCheckbox(
    modifier: Modifier = Modifier,
    componentModifier: Modifier = Modifier,
    viewModel: VMDToggleViewModel<VMDNoContent>,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    VMDCheckbox(
        modifier = modifier,
        componentModifier = componentModifier,
        viewModel = viewModel,
        label = {},
        interactionSource = interactionSource,
        colors = colors
    )
}

@Composable
fun <C : VMDContent> VMDCheckbox(
    modifier: Modifier = Modifier,
    componentModifier: Modifier = Modifier,
    viewModel: VMDToggleViewModel<C>,
    label: @Composable (RowScope.(field: C) -> Unit),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    val toggleViewModel: VMDToggleViewModel<C> by viewModel.observeAsState(excludedProperties = if (modifier.isOverridingAlpha()) listOf(viewModel::isHidden) else emptyList())

    VMDLabeledComponent(
        modifier = Modifier
            .hidden(viewModel.isHidden)
            .then(modifier),
        label = { label(toggleViewModel.label) },
        content = {
            Checkbox(
                onCheckedChange = { checked -> viewModel.onValueChange(checked) },
                modifier = componentModifier,
                enabled = toggleViewModel.isEnabled,
                checked = toggleViewModel.isOn,
                interactionSource = interactionSource,
                colors = colors
            )
        }
    )
}

@Preview
@Composable
fun EnabledToggleCheckboxPreview() {
    val toggleViewModel = VMDComponents.Toggle.withState(true, MainScope())
    VMDCheckbox(viewModel = toggleViewModel)
}

@Preview
@Composable
fun DisabledToggleCheckboxPreview() {
    val toggleViewModel = VMDComponents.Toggle.withState(false, MainScope())
    VMDCheckbox(viewModel = toggleViewModel)
}

@Preview
@Composable
fun SimpleTextToggleCheckboxPreview() {
    val toggleViewModel = VMDComponents.Toggle.withText("Label", true, MainScope())
    VMDCheckbox(viewModel = toggleViewModel, label = { Text(it.text) })
}
