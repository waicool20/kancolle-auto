package com.waicool20.kcsubswitcher

import org.sikuli.script.Pattern

enum class Fatigued(val id: String) {
    HIGH_FATIGUE("fatigue_high"),
    MED_FATIGUE("fatigue_med");

    fun image() = "status/$id.png"
}

enum class Damage(val id: String, val level: Int) {
    UNDER_REPAIR("dmg_repair", -1),
    LIGHTLY_DAMAGED("dmg_light", 0),
    MODERATELY_DAMAGED("dmg_moderate", 1),
    CRITICALLY_DAMAGED("dmg_critical", 2);

    fun image() = "status/$id.png"

    fun pattern(similarity: Double) = Pattern(image()).similar(similarity.toFloat())

    companion object {
        fun getForLevel(level: Int) =
                Damage.values().filter { it.level >= level }.toSet()
    }
}

enum class Submarines(val subName: String, val isSSV: Boolean) {
    I_8("i-8", false),
    I_8_KAI("i-8-kai", true),
    I_13("i-13", true),
    I_14("i-14", true),
    I_19("i-19", false),
    I_19_KAI("i-19-kai", true),
    I_26("i-26", false),
    I_26_KAI("i-26-kai", true),
    I_58("i-58", false),
    I_58_KAI("i-58-kai", true),
    I_168("i-168", false),
    I_401("i-401", true),
    MARUYU("maruyu", false),
    RO_500("ro-500", false),
    U_511("u-511", false),
    LUIGI("luigi", false),
    UIT_25("uit-25", false),
    I_504("i-504", false);

    fun image() = "subs/fleetcomp_shiplist_submarine_$subName.png"

    fun pattern(similarity: Double) = Pattern(image()).similar(similarity.toFloat())

    companion object {
        fun parseSubmarineList(subs: List<String>): Set<Submarines> {
            val enabledSubs = mutableSetOf<Submarines>()
            subs.forEach { sub ->
                when {
                    sub.equals("all", true) -> return Submarines.values().asList().toSet()
                    sub.equals("ss", true) -> enabledSubs.addAll(Submarines.values().filter { !it.isSSV })
                    sub.equals("ssv", true) -> enabledSubs.addAll(Submarines.values().filter { it.isSSV })
                    else -> Submarines.values().find { it.subName.equals(sub, true) }?.let { enabledSubs.add(it) }
                }
            }
            return enabledSubs
        }
    }
}





