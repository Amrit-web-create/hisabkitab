package com.hisablist.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * On-device speech recognition wrapper. Asks the system recogniser to PREFER OFFLINE.
 * Failures are reported as message keys (see Strings.kt) so the UI can fall back to typing.
 */
class VoiceHandle(private val ctx: Context) {
    var listening by mutableStateOf(false)
    var partial by mutableStateOf("")

    var lang: String = "en"
    var onResult: (List<String>) -> Unit = {}
    var onMessage: (String) -> Unit = {}
    var askPermission: () -> Unit = {}

    private var rec: SpeechRecognizer? = null

    fun toggle() {
        if (listening) { stop(); return }
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) begin() else askPermission()
    }

    fun begin() {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            onMessage("voice_unavailable")
            return
        }
        destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                listening = false
                partial = ""
                onMessage(
                    when (error) {
                        12, 13 -> "voice_offline_pack"   // language (pack) not supported / unavailable
                        9 -> "voice_perm"
                        6, 7 -> "nothing_heard"
                        else -> "voice_error"
                    }
                )
            }

            override fun onResults(results: Bundle?) {
                listening = false
                partial = ""
                val l = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                if (l.isEmpty()) onMessage("nothing_heard") else onResult(l)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == "hi") "hi-IN" else "en-IN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        listening = true
        r.startListening(i)
    }

    fun stop() {
        try { rec?.stopListening() } catch (_: Exception) {}
        listening = false
    }

    fun destroy() {
        try { rec?.destroy() } catch (_: Exception) {}
        rec = null
        listening = false
    }
}

@Composable
fun rememberVoice(
    lang: String,
    onResult: (List<String>) -> Unit,
    onMessage: (String) -> Unit
): VoiceHandle {
    val ctx = LocalContext.current
    val h = remember { VoiceHandle(ctx) }
    h.lang = lang
    h.onResult = onResult
    h.onMessage = onMessage
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) h.begin() else h.onMessage("voice_perm")
    }
    h.askPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(Unit) { onDispose { h.destroy() } }
    return h
}

/** Text-to-speech for the calculator readout (uses the phone's offline TTS voices). */
class Speaker(ctx: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(ctx.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String, lang: String) {
        if (!ready) return
        tts.setLanguage(if (lang == "hi") Locale("hi", "IN") else Locale("en", "IN"))
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "calc")
    }

    fun shutdown() {
        try { tts.stop(); tts.shutdown() } catch (_: Exception) {}
    }
}
