package com.fourdfit.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fourdfit.app.domain.model.ActivityLevel
import com.fourdfit.app.domain.model.FitnessGoal
import com.fourdfit.app.domain.model.Gender
import com.fourdfit.app.domain.model.UnitSystem
import com.fourdfit.app.domain.model.UserProfile
import com.fourdfit.app.domain.model.ZodiacSign
import com.fourdfit.app.ui.components.Avatar
import com.fourdfit.app.ui.components.ChipGroup
import com.fourdfit.app.ui.components.FourDTextField
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.PickerField
import com.fourdfit.app.ui.components.SegmentedToggle
import com.fourdfit.app.ui.components.ZodiacEmblem
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.utils.DateUtils
import com.fourdfit.app.utils.Units
import com.fourdfit.app.utils.Validators
import com.fourdfit.app.utils.ZodiacCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.roundToInt

enum class FormField { NAME, EMAIL, PASSWORD, DOB, GENDER, HEIGHT, ACTIVITY, GOAL, COUNTRY, CONSENT }

data class ProfileFormState(
    val fullName: String = "",
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val heightUnit: UnitSystem = UnitSystem.METRIC,
    val heightCmText: String = "",
    val heightFtText: String = "",
    val heightInText: String = "",
    val activityLevel: ActivityLevel? = null,
    val fitnessGoal: FitnessGoal? = null,
    val country: String = "",
    val photoPath: String? = null,
) {
    fun heightCm(): Float? =
        when (heightUnit) {
            UnitSystem.METRIC -> heightCmText.replace(',', '.').toFloatOrNull()
            UnitSystem.IMPERIAL -> {
                val feet = heightFtText.toIntOrNull()
                val inches = heightInText.ifBlank { "0" }.toIntOrNull()
                if (feet == null || inches == null || inches !in 0..11) null else Units.feetInchesToCm(feet, inches)
            }
        }

    /** Switches unit while converting whatever the user already typed. */
    fun withHeightUnit(unit: UnitSystem): ProfileFormState {
        if (unit == heightUnit) return this
        val cm = heightCm()
        return when (unit) {
            UnitSystem.METRIC -> copy(heightUnit = unit, heightCmText = cm?.roundToInt()?.toString() ?: heightCmText)
            UnitSystem.IMPERIAL -> {
                val (ft, inch) = cm?.let { Units.cmToFeetInches(it) } ?: (null to null)
                copy(heightUnit = unit, heightFtText = ft?.toString() ?: heightFtText, heightInText = inch?.toString() ?: heightInText)
            }
        }
    }

    companion object {
        fun from(
            profile: UserProfile,
            units: UnitSystem,
        ): ProfileFormState {
            val (ft, inch) = Units.cmToFeetInches(profile.heightCm)
            return ProfileFormState(
                fullName = profile.fullName,
                dateOfBirth = profile.dateOfBirth,
                gender = profile.gender,
                heightUnit = units,
                heightCmText = profile.heightCm.roundToInt().toString(),
                heightFtText = ft.toString(),
                heightInText = inch.toString(),
                activityLevel = profile.activityLevel,
                fitnessGoal = profile.fitnessGoal,
                country = profile.country,
                photoPath = profile.photoPath,
            )
        }
    }
}

object ProfileFormValidator {
    fun validate(form: ProfileFormState): Map<FormField, String> =
        buildMap {
            Validators.name(form.fullName)?.let { put(FormField.NAME, it) }
            Validators.dateOfBirth(form.dateOfBirth)?.let { put(FormField.DOB, it) }
            if (form.gender == null) put(FormField.GENDER, "Choose an option")
            heightError(form)?.let { put(FormField.HEIGHT, it) }
            if (form.activityLevel == null) put(FormField.ACTIVITY, "Choose your activity level")
            if (form.fitnessGoal == null) put(FormField.GOAL, "Choose a goal")
            Validators.country(form.country)?.let { put(FormField.COUNTRY, it) }
        }

    private fun heightError(form: ProfileFormState): String? {
        val error = Validators.heightCm(form.heightCm()) ?: return null
        return if (form.heightUnit == UnitSystem.IMPERIAL) "Enter a height between 3 ft 4 in and 8 ft 2 in" else error
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = FourD.colors.textPrimary,
        modifier =
            Modifier
                .padding(top = 4.dp)
                .semantics { heading() },
    )
}

/**
 * Profile fields shared by registration and Edit Profile.
 * [afterName] lets registration slot in email and password right after the name.
 */
@Composable
fun ProfileFormFields(
    form: ProfileFormState,
    errors: Map<FormField, String>,
    onChange: (ProfileFormState) -> Unit,
    onPickPhoto: () -> Unit,
    photoBusy: Boolean,
    modifier: Modifier = Modifier,
    afterName: @Composable ColumnScope.() -> Unit = {},
) {
    var showDobPicker by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PhotoPickerRow(form.photoPath, form.fullName, photoBusy, onPickPhoto)

        FourDTextField(
            value = form.fullName,
            onValueChange = { onChange(form.copy(fullName = it.take(60))) },
            label = "Full name",
            leadingIcon = Icons.Rounded.Person,
            error = errors[FormField.NAME],
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
        )
        afterName()

        PickerField(
            value = form.dateOfBirth?.format(DateUtils.mediumDate).orEmpty(),
            label = "Date of birth",
            onClick = { showDobPicker = true },
            leadingIcon = Icons.Rounded.Cake,
            error = errors[FormField.DOB],
        )
        val dob = form.dateOfBirth
        AnimatedVisibility(
            visible = dob != null && Validators.dateOfBirth(dob) == null,
            enter = fadeIn() + expandVertically() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + shrinkVertically(),
        ) {
            if (dob != null) {
                ZodiacRevealCard(ZodiacCalculator.signFor(dob), ZodiacCalculator.ageOn(dob))
            }
        }

        FieldLabel("Gender")
        ChipGroup(
            options = Gender.entries,
            selected = form.gender,
            label = { it.label },
            onSelect = { onChange(form.copy(gender = it)) },
            error = errors[FormField.GENDER],
        )

        FieldLabel("Height")
        SegmentedToggle(
            options = UnitSystem.entries,
            selected = form.heightUnit,
            label = { if (it == UnitSystem.METRIC) "cm" else "ft / in" },
            onSelect = { onChange(form.withHeightUnit(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (form.heightUnit == UnitSystem.METRIC) {
            FourDTextField(
                value = form.heightCmText,
                onValueChange = { v -> onChange(form.copy(heightCmText = v.filter { it.isDigit() || it == '.' || it == ',' }.take(5))) },
                label = "Height in cm",
                leadingIcon = Icons.Rounded.Straighten,
                error = errors[FormField.HEIGHT],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FourDTextField(
                    value = form.heightFtText,
                    onValueChange = { v -> onChange(form.copy(heightFtText = v.filter(Char::isDigit).take(1))) },
                    label = "Feet",
                    leadingIcon = Icons.Rounded.Straighten,
                    error = errors[FormField.HEIGHT],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                FourDTextField(
                    value = form.heightInText,
                    onValueChange = { v -> onChange(form.copy(heightInText = v.filter(Char::isDigit).take(2))) },
                    label = "Inches",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FieldLabel("Activity level")
        ChipGroup(
            options = ActivityLevel.entries,
            selected = form.activityLevel,
            label = { it.label },
            description = { it.description },
            onSelect = { onChange(form.copy(activityLevel = it)) },
            error = errors[FormField.ACTIVITY],
        )

        FieldLabel("Fitness goal")
        ChipGroup(
            options = FitnessGoal.entries,
            selected = form.fitnessGoal,
            label = { it.label },
            onSelect = { onChange(form.copy(fitnessGoal = it)) },
            error = errors[FormField.GOAL],
        )

        PickerField(
            value = form.country,
            label = "Country",
            onClick = { showCountryPicker = true },
            leadingIcon = Icons.Rounded.Public,
            error = errors[FormField.COUNTRY],
        )
    }

    if (showDobPicker) {
        DobPickerDialog(
            initial = form.dateOfBirth,
            onDismiss = { showDobPicker = false },
            onConfirm = {
                onChange(form.copy(dateOfBirth = it))
                showDobPicker = false
            },
        )
    }
    if (showCountryPicker) {
        CountryPickerDialog(
            onDismiss = { showCountryPicker = false },
            onSelect = {
                onChange(form.copy(country = it))
                showCountryPicker = false
            },
        )
    }
}

@Composable
private fun PhotoPickerRow(
    photoPath: String?,
    name: String,
    busy: Boolean,
    onPick: () -> Unit,
) {
    val c = FourD.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = "Choose a profile photo", role = Role.Button, onClick = onPick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Avatar(photoPath = photoPath, name = name.ifBlank { "You" }, size = 72.dp)
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.violet)
                    .border(2.dp, c.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = c.textPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                if (photoPath == null) "Add a profile photo" else "Change profile photo",
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
            )
            Text("Optional. Stays on this device.", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/** Animated card revealing the zodiac sign once a valid date of birth is chosen. */
@Composable
fun ZodiacRevealCard(
    sign: ZodiacSign,
    age: Int,
    modifier: Modifier = Modifier,
) {
    val c = FourD.colors
    GlassCard(modifier = modifier.fillMaxWidth(), glow = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZodiacEmblem(sign, diameter = 76.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Your Zodiac Sign: ${sign.displayName} ${sign.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                )
                Text(sign.dateRange, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                Text("Age: $age", style = MaterialTheme.typography.bodyMedium, color = c.cyan)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(sign.description, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Zodiac content is for entertainment only.",
            style = MaterialTheme.typography.labelSmall,
            color = c.textMuted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DobPickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val c = FourD.colors
    val today = LocalDate.now()
    val latestAllowed = today.minusYears(Validators.MIN_AGE.toLong())
    val latestMillis = latestAllowed.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val start = initial ?: today.minusYears(25)
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            yearRange = (today.year - Validators.MAX_AGE)..latestAllowed.year,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= latestMillis
                },
        )
    val colors = DatePickerDefaults.colors(containerColor = c.surfaceHigh)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text("Confirm", color = c.cyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) }
        },
        colors = colors,
    ) {
        DatePicker(
            state = state,
            colors = colors,
            title = {
                Text(
                    "Date of birth",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            },
        )
    }
}

@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val c = FourD.colors
    val countries =
        remember {
            Locale
                .getISOCountries()
                .mapNotNull { code ->
                    runCatching {
                        Locale
                            .Builder()
                            .setRegion(code)
                            .build()
                            .displayCountry
                    }.getOrNull()
                }.filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query) {
            val q = query.trim()
            if (q.isEmpty()) countries else countries.filter { it.contains(q, ignoreCase = true) }
        }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.surfaceHigh)
                .border(1.dp, c.glassBorder, RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) {
            Text("Choose your country", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
            Spacer(Modifier.height(12.dp))
            FourDTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search countries",
                leadingIcon = Icons.Rounded.Search,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(filtered, key = { it }) { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textPrimary,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { onSelect(name) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "No countries match \"$query\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel", color = c.textSecondary)
            }
        }
    }
}
