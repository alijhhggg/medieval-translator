package com.trae.medievaltranslator

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: Button
    private lateinit var resultTextView: TextView
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var translator: Translator? = null

    private val glossary = mapOf(
        "Excommunicated" to "تکفیر شده (اخراج از کلیسا)",
        "Excommunication" to "تکفیر (اخراج از کلیسا)",
        "Annex Settlement" to "تصرف و الحاق سکونتگاه",
        "Council of Nobles" to "شورای بزرگان",
        "Papal Election" to "انتخابات پاپ",
        "Mission from the Pope" to "ماموریت از طرف پاپ",
        "Papal" to "مربوط به پاپ",
        "Chivalry" to "جوانمردی و شجاعت",
        "Dread" to "ترس و وحشت",
        "Advisor Messages" to "پیام‌های مشاور",
        "Faction Leader" to "رهبر حکومت",
        "Faction" to "حکومت",
        "Factions" to "حکومت‌ها",
        "Diplomat" to "دیپلمات/سفیر",
        "Settlement" to "سکونتگاه/شهر",
        "Cardinal" to "کاردینال",
        "Inquisitor" to "بازرس کلیسا",
        "Merchant" to "تاجر",
        "Spy" to "جاسوس",
        "Assassin" to "تروریست/قاتل",
        "Princess" to "شاهزاده خانم",
        "General" to "ژنرال/فرمانده",
        "Siege" to "محاصره",
        "Garrison" to "پادگان دفاعی",
        "Tribute" to "باج و خراج",
        "Alliance" to "اتحاد",
        "Ceasefire" to "آتش‌بس",
        "Trade Rights" to "حقوق تجاری",
        "Vassal" to "حکومت دست‌نشانده",
        "Crusade" to "جنگ صلیبی",
        "Jihad" to "جهاد",
        "Guild" to "صنف/انجمن",
        "Pope" to "پاپ",
        "Papal States" to "دولت‌های پاپ",
        "Piety" to "تقوا و دینداری",
        "Loyalty" to "وفاداری",
        "Authority" to "اقتدار و قدرت",
        "Command" to "فرماندهی",
        "Subjugate" to "مطیع کردن",
        "Ransom" to "فدیه آزادی اسرا",
        "Release" to "آزادسازی اسرا",
        "Exterminate" to "قتل‌عام شهر",
        "Sack" to "غارت شهر",
        "Occupy" to "اشغال شهر",
        "Upkeep" to "هزینه نگهداری",
        "Public Order" to "نظم عمومی",
        "Squalor" to "کثیفی و فقر شهر",
        "Unrest" to "ناآرامی",
        "Heresy" to "کفر و بدعت",
        "Heretic" to "کافر/مرتد",
        "Witch" to "ساحره/جادوگر",
        "Retinue" to "همراهان و اطرافیان",
        "Traits" to "ویژگی‌های شخصیتی"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        setupTranslator()
        setupFloatingUI()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
            val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("DATA_INTENT", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra<Intent>("DATA_INTENT")
            }

            if (resultCode == Activity.RESULT_OK && dataIntent != null) {
                val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, dataIntent)

                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        stopCaptureSession()
                    }
                }, Handler(Looper.getMainLooper()))

                // ساخت دائمی سرویس عکس‌برداری (فقط یک‌بار)
                startPersistentCapture()
                Toast.makeText(this, "سرویس فعال شد! بدون محدودیت استفاده کنید.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "مجوز اسکرین‌شات دریافت نشد.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در راه‌اندازی: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return START_STICKY
    }

    private fun startPersistentCapture() {
        val proj = mediaProjection ?: return
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = proj.createVirtualDisplay(
            "ScreenCapture",
            width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, Handler(Looper.getMainLooper())
        )
    }

    private fun setupTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.PERSIAN)
            .build()
        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Toast.makeText(this, "مدل ترجمه آفلاین آماده است", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupFloatingUI() {
        floatingButton = Button(this).apply {
            text = "ترجمه ⚔️"
            setOnClickListener {
                captureAndTranslate()
            }
        }

        val btnParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        resultTextView = TextView(this).apply {
            setBackgroundColor(0xDD000000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(24, 24, 24, 24)
            textSize = 15f
            text = "روی (ترجمه ⚔️) بزنید تا متن اسکن و ترجمه شود."
        }

        val textParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        windowManager.addView(floatingButton, btnParams)
        windowManager.addView(resultTextView, textParams)
    }

    private fun captureAndTranslate() {
        val reader = imageReader
        if (mediaProjection == null || reader == null) {
            resultTextView.text = "خطا: سرویس اسکرین‌شات فعال نیست. برنامه را ببندید و دوباره شروع را بزنید."
            return
        }

        resultTextView.text = "در حال گرفتن عکس..."

        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                resultTextView.text = "تصویری دریافت نشد. دوباره بزنید."
                return
            }

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmapWidth = width + if (pixelStride > 0) rowPadding / pixelStride else 0
            val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            val cleanBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

            // فقط تصویر خامی که برداشتیم بسته می‌شود؛ VirtualDisplay زنده می‌ماند!
            image.close()
            processImage(cleanBitmap)

        } catch (e: Exception) {
            image?.close()
            resultTextView.text = "خطا در اسکن: ${e.localizedMessage}"
        }
    }

    private fun processImage(bitmap: Bitmap) {
        resultTextView.text = "در حال تشخیص متن..."
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val cleanTextBlocks = mutableListOf<String>()

                for (block in visionText.textBlocks) {
                    val blockText = block.text.trim()
                    if (blockText.length > 3 && !blockText.matches(Regex("^[0-9\\s\\W]+$"))) {
                        cleanTextBlocks.add(blockText)
                    }
                }

                if (cleanTextBlocks.isNotEmpty()) {
                    val fullTextToTranslate = cleanTextBlocks.joinToString("\n\n")
                    translateTextWithGlossary(fullTextToTranslate)
                } else {
                    resultTextView.text = "متن قابل ترجمه‌ای روی صفحه پیدا نشد."
                }
            }
            .addOnFailureListener { e ->
                resultTextView.text = "خطا در تشخیص متن: ${e.localizedMessage}"
            }
    }

    private fun translateTextWithGlossary(originalText: String) {
        resultTextView.text = "در حال ترجمه..."

        var preProcessedText = originalText
        for ((key, value) in glossary) {
            preProcessedText = preProcessedText.replace(Regex("(?i)\\b$key\\b"), value)
        }

        translator?.translate(preProcessedText)
            ?.addOnSuccessListener { translatedText ->
                var finalResult = translatedText
                for ((key, value) in glossary) {
                    finalResult = finalResult.replace(Regex("(?i)\\b$key\\b"), value)
                }
                resultTextView.text = finalResult
            }
            ?.addOnFailureListener { e ->
                resultTextView.text = "خطا در ترجمه: ${e.localizedMessage}\n\nمتن اصلی:\n$originalText"
            }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("مترجم مدویال")
            .setContentText("سرویس ترجمه فعال است")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun stopCaptureSession() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection = null
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized) windowManager.removeView(floatingButton)
        if (::resultTextView.isInitialized) windowManager.removeView(resultTextView)
        stopCaptureSession()
        translator?.close()
    }
}
