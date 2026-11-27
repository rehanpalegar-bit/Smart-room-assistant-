
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
import java.net.URLEncoder
import kotlin.concurrent.thread
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var result: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var selectedLanguage = "en-IN"

    private val devices = mapOf(
        "light" to 1,
        "fan" to 2,
        "lamp" to 3,
        "socket" to 4,
        "profile" to 5,
        "hanger" to 6
    )

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

    private fun normalize(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace("light", "light")
            .replace("chalu", "on")
            .replace("chaalu", "on")
            .replace("band karo", "off")
            .replace("band kar do", "off")
            .replace("बंद करो", "off")
            .replace("बंद कर दो", "off")
            .replace("चालू करो", "on")
            .replace("चालू कर दो", "on")
            .replace("पंखा", "fan")
            .replace("बत्ती", "light")
            .replace("लाइट", "light")
            .replace("लैंप", "lamp")
            .replace("सॉकेट", "socket")
            .replace("प्लग", "socket")
            .replace("प्रोफाइल लाइट", "profile light")
            .replace("हैंगर लाइट", "hanger light")
            .replace("चालू", "on")
            .replace("बंद", "off")
    }

    private fun processCommand(command: String) {
        val text = normalize(command)

        val channel = when {
            text.contains("profile") -> 5
            text.contains("hanger") -> 6
            text.contains("socket") || text.contains("plug") -> 4
            text.contains("fan") -> 2
            text.contains("lamp") -> 3
            text.contains("light") -> 1
            else -> 0
        }

        val action = when {
            text.contains("off") -> "off"
            text.contains("on") -> "on"
            else -> ""
        }

        if (channel == 0 || action.isEmpty()) {
            status.text = "Command not understood"
            return
        }

        result.text = "Device channel: CH$channel\nAction: $action"
        status.text = "Sending command..."

        controlRelay(channel, action)
    }

    private fun controlRelay(channel: Int, action: String) {
        thread {
            var connection: HttpURLConnection? = null

            try {
                val url = URL(
                    "http://192.168.4.1/api/relay?ch=$channel&state=$action"
                )

                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val code = connection.responseCode
                val response = if (code in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                        ?: "HTTP Error: $code"
                }

                runOnUiThread {
                    if (code in 200..299) {
                        status.text = "Command sent successfully!"
                        result.text = "CH$channel $action\n$response"
                    } else {
                        status.text = "Relay command failed"
                        result.text = response
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Connection failed"
                    result.text = e.message ?: "Unknown error"
                }
            } finally {
                connection?.disconnect()
            }
        }
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

                val code = connection.responseCode
                val response = if (code == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    "HTTP Error: $code"
                }

                runOnUiThread {
                    status.text =
                        if (code == 200) "ESP32 Connected!"
                        else "ESP32 Connection Failed"
                    result.text = response
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
                val spokenText = matches?.firstOrNull() ?: ""

                result.text = "You said: $spokenText"
                processCommand(spokenText)
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
