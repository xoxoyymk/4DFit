package com.fourdfit.app.ui.settings

import com.fourdfit.app.BuildConfig

/**
 * In-app legal copy. This is a starting template written for this app's actual data handling —
 * have it reviewed by a qualified professional for your jurisdiction before publishing.
 */
object LegalTexts {
    const val PRIVACY = "privacy"
    const val TERMS = "terms"
    const val ABOUT = "about"

    fun title(doc: String): String =
        when (doc) {
            PRIVACY -> "Privacy Policy"
            TERMS -> "Terms of Use"
            else -> "About 4D FIT"
        }

    fun sections(doc: String): List<Pair<String, String>> =
        when (doc) {
            PRIVACY -> privacy
            TERMS -> terms
            else -> about
        }

    private val privacy =
        listOf(
            "What we collect" to
                "Your name, email, date of birth, gender, height, activity level, fitness goal and country, plus the workouts you complete. Your password is sent once over an encrypted connection to create or access your account; it is never stored on your device.",
            "What stays on your device" to
                "Your profile photo, daily step count, water log and meal check-offs are stored only on this device. Steps are read from your phone's step sensor if you allow it.",
            "How data is protected" to
                "All network traffic uses HTTPS. Your session token is kept in encrypted storage backed by the Android Keystore. App data is excluded from device backups.",
            "How we use it" to
                "To personalise your plan, show your progress and send the reminders you switch on. We do not sell your data or show third-party ads.",
            "Your choices" to
                "You can edit your profile at any time, switch reminders off, sign out to remove your data from this device, or delete your account from Settings, which removes it from our servers.",
            "Age" to "4D FIT is intended for people aged 13 and over.",
            "Contact" to "Questions about privacy? Contact privacy@your-domain.com.",
        )

    private val terms =
        listOf(
            "General wellness only" to
                "4D FIT provides general fitness, nutrition and wellness information. It is not medical advice and is not a substitute for a doctor, physiotherapist or registered dietitian.",
            "Exercise safely" to
                "Check with a health professional before starting a new exercise programme, especially if you are pregnant, injured or have a medical condition. Stop immediately if you feel pain, dizziness or shortness of breath.",
            "Nutrition plan" to
                "Meal ideas are general suggestions for balanced eating. Adapt them to allergies, intolerances, cultural preferences and personal needs.",
            "Horoscopes" to
                "Horoscope content is for entertainment/general-interest purposes only and should not be used to make decisions about health, money, relationships or other important matters.",
            "Your account" to "Keep your login details private. You're responsible for activity on your account.",
            "Changes" to "We may update these terms. We'll let you know in the app when we do.",
        )

    private val about =
        listOf(
            "4D FIT" to
                "Version ${BuildConfig.VERSION_NAME}. A fitness and wellness companion with guided workouts, a 30-day balanced eating plan, progress tracking and a light-hearted daily horoscope.",
            "Data source" to
                if (BuildConfig.USE_MOCK_API) "Demo mode: running on the built-in on-device mock server." else "Connected to the 4D FIT online service.",
            "Typefaces" to "Space Grotesk and Manrope, both licensed under the SIL Open Font License 1.1.",
            "Accessibility" to
                "4D FIT supports system font scaling, TalkBack, a high-contrast theme and a reduce-motion option in Settings.",
        )
}
