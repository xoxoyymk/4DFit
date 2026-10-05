package com.fourdfit.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.BuildConfig
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.MotionType
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.DisclaimerNote
import com.fourdfit.app.ui.components.ExerciseAnimation
import com.fourdfit.app.ui.components.FourDTextField
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.NeonButton
import com.fourdfit.app.ui.components.SegmentedToggle
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.settings.LegalTexts
import com.fourdfit.app.ui.theme.FourD

@Composable
fun OnboardingScreen() {
    val vm = appViewModel { OnboardingViewModel(it.authRepository, it.imageStorage) }
    val state by vm.state.collectAsStateWithLifecycle()
    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) vm.onPhotoPicked(uri)
        }
    var legalDoc by remember { mutableStateOf<String?>(null) }
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    AdaptiveContainer {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(screenPadding(withBottomBar = false)),
        ) {
            BrandHero()
            Spacer(Modifier.height(18.dp))
            AnimatedVisibility(
                visible = cardVisible,
                enter =
                    fadeIn(tween(450)) +
                        scaleIn(tween(650, easing = FastOutSlowInEasing), initialScale = 0.86f) +
                        slideInVertically(tween(650, easing = FastOutSlowInEasing)) { it / 8 },
            ) {
                GlassCard(glow = true, contentPadding = PaddingValues(20.dp)) {
                    SegmentedToggle(
                        options = AuthMode.entries,
                        selected = state.mode,
                        label = { if (it == AuthMode.REGISTER) "Create profile" else "Sign in" },
                        onSelect = vm::setMode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    AnimatedContent(
                        targetState = state.mode,
                        transitionSpec = {
                            val direction = if (targetState == AuthMode.SIGN_IN) 1 else -1
                            (fadeIn(tween(280)) + slideInHorizontally(tween(320)) { direction * it / 5 }) togetherWith fadeOut(tween(160))
                        },
                        label = "authMode",
                    ) { mode ->
                        when (mode) {
                            AuthMode.REGISTER ->
                                RegisterForm(
                                    state = state,
                                    vm = vm,
                                    onPickPhoto = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onOpenLegal = { legalDoc = it },
                                )
                            AuthMode.SIGN_IN -> SignInForm(state = state, vm = vm)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "4D FIT offers general fitness and wellness guidance, not medical advice. If you have a health condition, check with a professional before starting a new routine.",
                style = MaterialTheme.typography.bodySmall,
                color = FourD.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    legalDoc?.let { doc -> LegalDialog(doc = doc, onDismiss = { legalDoc = null }) }
}

@Composable
private fun BrandHero() {
    val c = FourD.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "4D",
                    style = MaterialTheme.typography.displayLarge.copy(shadow = Shadow(color = c.cyan, blurRadius = 32f)),
                    color = c.cyan,
                )
                Spacer(Modifier.width(8.dp))
                Text("FIT", style = MaterialTheme.typography.displayLarge, color = c.textPrimary)
            }
            Text(
                "Train, eat well and track your progress — in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                modifier = Modifier.semantics { heading() },
            )
        }
        ExerciseAnimation(
            motion = MotionType.JUMPING_JACK,
            modifier = Modifier.size(116.dp),
            contentDescription = null,
        )
    }
}

@Composable
private fun RegisterForm(
    state: OnboardingUiState,
    vm: OnboardingViewModel,
    onPickPhoto: () -> Unit,
    onOpenLegal: (String) -> Unit,
) {
    val c = FourD.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Create your fitness profile", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
        Text(
            "Takes about a minute. You can change any of this later.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        ProfileFormFields(
            form = state.form,
            errors = state.errors,
            onChange = vm::onFormChange,
            onPickPhoto = onPickPhoto,
            photoBusy = state.isSavingPhoto,
            afterName = {
                FourDTextField(
                    value = state.email,
                    onValueChange = vm::onEmailChange,
                    label = "Email",
                    leadingIcon = Icons.Rounded.Email,
                    error = state.errors[FormField.EMAIL],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                PasswordField(
                    value = state.password,
                    onValueChange = vm::onPasswordChange,
                    label = "Password",
                    error = state.errors[FormField.PASSWORD],
                    supportingText = "8+ characters with a letter and a number. Never stored on this device.",
                    imeAction = ImeAction.Next,
                )
            },
        )
        ConsentRow(
            checked = state.acceptedTerms,
            onCheckedChange = vm::onTermsChange,
            error = state.errors[FormField.CONSENT],
            onOpenLegal = onOpenLegal,
        )
        FormMessage(state.message)
        NeonButton(
            text = "Create My Fitness Profile",
            onClick = vm::register,
            loading = state.isSubmitting,
            icon = Icons.Rounded.Bolt,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SignInForm(
    state: OnboardingUiState,
    vm: OnboardingViewModel,
) {
    val c = FourD.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Welcome back", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
        Text("Sign in to restore your profile and progress.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        FourDTextField(
            value = state.signInEmail,
            onValueChange = vm::onSignInEmailChange,
            label = "Email",
            leadingIcon = Icons.Rounded.Email,
            error = state.errors[FormField.EMAIL],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        PasswordField(
            value = state.signInPassword,
            onValueChange = vm::onSignInPasswordChange,
            label = "Password",
            error = state.errors[FormField.PASSWORD],
            imeAction = ImeAction.Done,
            onDone = vm::signIn,
        )
        FormMessage(state.message)
        NeonButton("Sign in", vm::signIn, Modifier.fillMaxWidth(), loading = state.isSubmitting)
        if (BuildConfig.USE_MOCK_API) {
            DisclaimerNote("Demo mode: accounts live on this device only. Sign in with the email you registered here.")
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onDone: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    FourDTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Rounded.Lock,
        error = error,
        supportingText = supportingText,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        trailing = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
    )
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    error: String?,
    onOpenLegal: (String) -> Unit,
) {
    val c = FourD.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = c.violet,
                        uncheckedColor = if (error != null) c.danger else c.textSecondary,
                        checkmarkColor = Color.White,
                    ),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "I agree to the Terms of Use and Privacy Policy",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textPrimary,
            )
        }
        Row {
            TextButton(onClick = { onOpenLegal(LegalTexts.TERMS) }) {
                Text("Read terms", color = c.cyan, style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { onOpenLegal(LegalTexts.PRIVACY) }) {
                Text("Read privacy policy", color = c.cyan, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = c.danger, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Form-level message (server errors, validation summary). Announced by screen readers. */
@Composable
fun FormMessage(message: String?) {
    if (message == null) return
    val c = FourD.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.danger.copy(alpha = 0.14f))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = c.danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
    }
}

@Composable
fun LegalDialog(
    doc: String,
    onDismiss: () -> Unit,
) {
    val c = FourD.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceHigh,
        title = { Text(LegalTexts.title(doc), style = MaterialTheme.typography.titleLarge, color = c.textPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LegalTexts.sections(doc).forEach { (heading, body) ->
                    Text(heading, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = c.cyan) }
        },
    )
}
