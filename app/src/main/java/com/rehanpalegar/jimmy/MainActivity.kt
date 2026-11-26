
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
    )

    putExtra(
        RecognizerIntent.EXTRA_LANGUAGE,
        "en-IN"
    )

    putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
        "en-IN"
    )

    putExtra(
        RecognizerIntent.EXTRA_PROMPT,
        "Speak in Hindi or English"
    )

    if (android.os.Build.VERSION.SDK_INT >= 34) {
        putExtra(
            RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION,
            true
        )

        putStringArrayListExtra(
            RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES,
            arrayListOf("en-IN", "hi-IN")
        )
    }
}
