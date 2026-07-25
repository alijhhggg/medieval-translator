package com.trae.medievaltranslator

import android.content.Context

class ScreenTranslatorEngine(private val context: Context) {

    private val glossaryTranslator = RuleBasedGlossaryTranslator()
    private val onnxTranslator = OnnxOfflineTranslator(context)

    init {
        onnxTranslator.initialize()
    }

    fun processAndTranslate(extractedText: String): String {
        val cleanInput = extractedText.trim()
        if (cleanInput.isEmpty()) return ""

        // ۱. بررسی حافظه کش
        TranslationCache.get(cleanInput)?.let { cachedResult ->
            return cachedResult
        }

        // ۲. اعمال واژه‌نامه تخصصی بازی Medieval II
        var processedText = glossaryTranslator.applyGlossary(cleanInput)

        // ۳. ترجمه با موتور عصبی آفلاین
        var finalTranslation = onnxTranslator.translate(processedText)

        // ۴. ذخیره در کش برای دفعات بعدی
        TranslationCache.put(cleanInput, finalTranslation)

        return finalTranslation
    }
}
