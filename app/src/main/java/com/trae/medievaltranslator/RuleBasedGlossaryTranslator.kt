package com.trae.medievaltranslator

class RuleBasedGlossaryTranslator {

    private val medievalGlossary = mapOf(
        // Factions
        "Holy Roman Empire" to "امپراتوری مقدس روم",
        "Byzantine Empire" to "امپراتوری بیزانس",
        "Kingdom of France" to "پادشاهی فرانسه",
        "Kingdom of England" to "پادشاهی انگلستان",
        "Kingdom of Spain" to "پادشاهی اسپانیا",
        "The Turks" to "ترک‌ها (عثمانی/سلجوقی)",
        "Egypt" to "مصر (ایوبیان)",
        "Mongols" to "مغول‌ها",
        "Papal States" to "دولتهای پاپ",

        // Game UI & Mechanics
        "Dread" to "قساوت / ترس",
        "Chivalry" to "مروت / شوالیه گری",
        "Command" to "فرماندهی",
        "Piety" to "پارسایی / دین‌داری",
        "Loyalty" to "وفاداری",
        "Authority" to "اقتدار",
        "Upkeep" to "هزینه نگهداری",
        "Public Order" to "نظم عمومی",
        "Squalor" to "آلودگی و ازدحام",
        "Garrison" to "پادگان / محافظان",
        "Siege" to "محاصره",
        "Crusade" to "جنگ صلیبی",
        "Jihad" to "جهاد",
        "Excommunicated" to "تکفیرشده (اخراج از کلیسا)",

        // Units & Agents
        "Spear Militia" to "میلیشیای نیزه‌دار",
        "Archers" to "کمانداران",
        "Knights" to "شوالیه‌ها",
        "Feudal Knights" to "شوالیه‌های اقطاع‌دار",
        "Merchant" to "تاجر",
        "Spy" to "جاسوس",
        "Assassin" to "قاتل / تروریست",
        "Diplomat" to "دیپلمات",
        "Cardinal" to "کاردینال"
    )

    fun applyGlossary(text: String): String {
        var result = text
        for ((en, fa) in medievalGlossary) {
            val regex = Regex("(?i)\\b${Regex.escape(en)}\\b")
            result = result.replace(regex, fa)
        }
        return result
    }
}
