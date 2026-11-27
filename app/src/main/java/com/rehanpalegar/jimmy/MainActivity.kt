
package com.rehanpalegar.jimmy

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer.RESULTS_RECOGNITION
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var result: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var selectedLanguage = "en-IN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(15, 20, 35))
        }

        val title = TextView(this).apply {
            text = "JIMMY"
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "JIMMY is ready"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 25)
        }

        result = TextView(this).apply {
            text = "Your voice command will appear here"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val englishButton = Button(this).apply {
            text = "ENGLISH VOICE"
            setOnClickListener {
                selectedLanguage = "en-IN"
                startVoiceRecognition()
            }
        }

        val hindiButton = Button(this).apply {
            text = "HINDI VOICE"
            setOnClickListener {
                selectedLanguage = "hi-IN"
                startVoiceRecognition()
            }
        }

        val connectionButton = Button(this).apply {
            text = "TEST ESP32 CONNECTION"
            setOnClickListener {
                testEsp32Connection()
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(result)
        layout.addView(englishButton)
        layout.addView(hindiButton)
        layout.addView(connectionButton)

        setContentView(layout)
    }

    private fun testEsp32Connection() {
        status.text = "Connecting to ESP32..."

        thread {
            var connection: HttpURLConnection? = null

            try {
                connection = URL(
                    "http://192.168.4.1/api/status"
                ).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode

                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use {
                        it.readText()
                    }
                } else {
                    "HTTP Error: $responseCode"
                }

                runOnUiThread {
                    if (responseCode == 200) {
                        status.text = "ESP32 Connected!"
                        result.text = response
                    } else {
                        status.text = "ESP32 Connection Failed"
                        result.text = response
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Connection Failed"
                    result.text = e.message ?: "Unknown error"
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun startVoiceRecognition() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition unavailable"
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                status.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                status.text = "Hearing your voice..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                status.text = "Processing..."
            }

            override fun onError(error: Int) {
                status.text = "Recognition error: $error"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(RESULTS_RECOGNITION)
                result.text = matches?.firstOrNull() ?: "No speech detected"
                status.text = "Command received"
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JIMMY")
        }

        speechRecognizer?.startListening(intent)
        status.text = "Starting microphone..."
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
