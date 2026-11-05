package com.smartassistant.arabic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * يستمع للأوامر الصوتية بالعربي وينفذها:
 * "افتح فيسبوك" ، "رجوع" ، "الرئيسية" ، "الاشعارات" ، "اضغط على <نص>"
 */
class VoiceAssistantManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: VoiceAssistantManager? = null

        fun getInstance(context: Context): VoiceAssistantManager =
            instance ?: synchronized(this) {
                instance ?: VoiceAssistantManager(context.applicationContext).also { instance = it }
            }

        // أسماء عربية شائعة -> اسم التطبيق بالإنجليزي
        private val appAliases = mapOf(
            "فيسبوك" to "facebook",
            "فيس بوك" to "facebook",
            "واتساب" to "whatsapp",
            "واتس اب" to "whatsapp",
            "يوتيوب" to "youtube",
            "ماسنجر" to "messenger",
            "مسنجر" to "messenger",
            "كروم" to "chrome",
            "انستغرام" to "instagram",
            "انستجرام" to "instagram",
            "تلغرام" to "telegram",
            "تليجرام" to "telegram",
            "تيك توك" to "tiktok",
            "الكاميرا" to "camera",
            "المعرض" to "gallery",
            "الاعدادات" to "settings"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    fun startListening() {
        mainHandler.post {
            if (listening) return@post

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                toast("صلاحية المايكروفون غير مفعلة")
                return@post
            }
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                toast("خدمة التعرف على الصوت غير متوفرة على الجهاز")
                return@post
            }

            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            listening = true
            toast("تفضل، أنا أسمعك...")
            recognizer?.startListening(intent)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            listening = false
            toast("ما سمعتك منيح، جرب مرة ثانية")
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (text.isNullOrBlank()) {
                toast("ما سمعت شي")
            } else {
                handleCommand(text)
            }
        }
    }

    private fun normalize(s: String): String =
        s.trim()
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ة', 'ه').replace('ى', 'ي')
            .lowercase()

    private fun handleCommand(raw: String) {
        val text = normalize(raw)
        toast("سمعت: $raw")

        when {
            text.startsWith("افتح ") -> openApp(text.removePrefix("افتح ").trim())
            text.startsWith("شغل ") -> openApp(text.removePrefix("شغل ").trim())
            text.contains("رجوع") || text.contains("ارجع") -> withAccessibility { it.performGoBack() }
            text.contains("الرئيسيه") || text.contains("الشاشه الرئيسيه") ->
                withAccessibility { it.performGoHome() }
            text.contains("الاشعارات") -> withAccessibility { it.performOpenNotifications() }
            text.startsWith("اضغط على ") ->
                withAccessibility { it.clickByText(text.removePrefix("اضغط على ").trim()) }
            text.startsWith("اضغط ") ->
                withAccessibility { it.clickByText(text.removePrefix("اضغط ").trim()) }
            else -> toast("ما فهمت الأمر: $raw")
        }
    }

    private fun withAccessibility(action: (AssistantAccessibilityService) -> Boolean) {
        val service = AssistantAccessibilityService.instance
        if (service == null) {
            toast("فعّل خدمة إمكانية الوصول من إعدادات الجوال أولاً")
            return
        }
        if (!action(service)) toast("ما قدرت أنفذ الأمر")
    }

    private fun openApp(spokenName: String) {
        val target = (appAliases[spokenName] ?: spokenName).lowercase()
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        val apps = pm.queryIntentActivities(launcherIntent, 0)

        val match = apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(target)
        } ?: apps.firstOrNull {
            it.activityInfo.packageName.lowercase().contains(target)
        }

        if (match == null) {
            toast("ما لقيت تطبيق: $spokenName")
            return
        }

        val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
        if (launch == null) {
            toast("ما قدرت أفتح: $spokenName")
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }
}
