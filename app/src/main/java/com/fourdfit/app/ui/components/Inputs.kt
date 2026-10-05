package com.fourdfit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fourdfit.app.ui.theme.FourD

private val FieldShape = RoundedCornerShape(18.dp)

@Composable
fun fourDFieldColors(): TextFieldColors {
    val c = FourD.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        disabledTextColor = c.textPrimary,
        errorTextColor = c.textPrimary,
        focusedContainerColor = c.glassFill,
        unfocusedContainerColor = c.glassFillLow,
        disabledContainerColor = c.glassFillLow,
        errorContainerColor = c.glassFillLow,
        cursorColor = c.cyan,
        errorCursorColor = c.danger,
        focusedBorderColor = c.cyan,
        unfocusedBorderColor = c.glassBorder,
        disabledBorderColor = c.glassBorder,
        errorBorderColor = c.danger,
        focusedLeadingIconColor = c.cyan,
        unfocusedLeadingIconColor = c.textSecondary,
        disabledLeadingIconColor = c.textSecondary,
        errorLeadingIconColor = c.danger,
        focusedTrailingIconColor = c.textPrimary,
        unfocusedTrailingIconColor = c.textSecondary,
        disabledTrailingIconColor = c.textSecondary,
        errorTrailingIconColor = c.danger,
        focusedLabelColor = c.cyan,
        unfocusedLabelColor = c.textSecondary,
        disabledLabelColor = c.textSecondary,
        errorLabelColor = c.danger,
        focusedPlaceholderColor = c.textMuted,
        unfocusedPlaceholderColor = c.textMuted,
        focusedSupportingTextColor = c.textSecondary,
        unfocusedSupportingTextColor = c.textSecondary,
        disabledSupportingTextColor = c.textSecondary,
        errorSupportingTextColor = c.danger,
    )
}

@Composable
fun FourDTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    error: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailing,
        isError = error != null,
        supportingText =
            when {
                error != null -> {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(error)
                        }
                    }
                }
                supportingText != null -> {
                    { Text(supportingText) }
                }
                else -> null
            },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        enabled = enabled,
        shape = FieldShape,
        colors = fourDFieldColors(),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { if (error != null) this.error(error) },
    )
}

/**
 * Read-only field that opens a picker (date, country). The whole field is one accessible button
 * announcing its label, current value and any error.
 */
@Composable
fun PickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    error: String? = null,
    placeholder: String = "Tap to choose",
) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = FieldShape,
            colors = fourDFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { },
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(FieldShape)
                .clickable(onClickLabel = "Change $label", role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription =
                        buildString {
                            append(label).append(": ").append(value.ifBlank { "not set" })
                            if (error != null) append(". ").append(error)
                        }
                },
        )
    }
}

/** Selectable pill. Selection is shown by a check icon as well as colour. */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    role: Role = Role.RadioButton,
) {
    val c = FourD.colors
    val border by animateColorAsState(if (selected) c.cyan else c.glassBorder, label = "chipBorder")
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(shape)
                .background(
                    if (selected) {
                        Brush.horizontalGradient(listOf(c.violet.copy(alpha = 0.32f), c.cyan.copy(alpha = 0.18f)))
                    } else {
                        Brush.horizontalGradient(listOf(c.glassFillLow, c.glassFillLow))
                    },
                ).border(if (selected) 1.5.dp else 1.dp, border, shape)
                .selectable(selected = selected, role = role, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.cyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    description: ((T) -> String?)? = null,
    error: String? = null,
) {
    Column(modifier) {
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                SelectableChip(
                    label = label(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    description = description?.invoke(option),
                )
            }
        }
        if (error != null) {
            Row(Modifier.padding(top = 6.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = FourD.colors.danger, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = FourD.colors.danger)
            }
        }
    }
}

/** Two-to-four option segmented control. */
@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(c.glassFillLow)
                .border(1.dp, c.glassBorder, shape)
                .padding(4.dp)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(if (isSelected) c.violet else Color.Transparent, label = "segment")
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else c.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                )
            }
        }
    }
}
