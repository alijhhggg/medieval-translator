package com.trae.medievaltranslator

import androidx.collection.LruCache

object TranslationCache {
    private val cache = LruCache<String, String>(500)

    fun get(text: String): String? {
        val cleanText = text.trim()
        return cache.get(cleanText)
    }

    fun put(text: String, translation: String) {
        val cleanText = text.trim()
        if (cleanText.isNotEmpty() && translation.isNotEmpty()) {
            cache.put(cleanText, translation)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
