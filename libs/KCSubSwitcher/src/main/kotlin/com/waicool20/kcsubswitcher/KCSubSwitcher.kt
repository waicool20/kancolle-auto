package com.waicool20.kcsubswitcher

import org.sikuli.basics.Settings
import org.sikuli.script.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

val CLASS_SIMILARITY = 0.67
val DMG_SIMILARITY = 0.66
val FATIGUE_SIMILARITY = 0.96

fun main(args: Array<String>) {
    // Test Case
    val switcher = KCSubSwitcher(
            Screen().subRegion(80, 81, 800, 480), // Manual coordinates to kc window
            listOf("ssv", "i-8", "i-19", "i-58", "i-168", "ro-500", "u-511"),
            0,
            false
    )
    App.focus("Chromium")
    val time = measureTimeMillis {
        switcher.switchSubs()
    }
    println("Finished executing in $time ms!")
}

class KCSubSwitcher(
        val kcRegion: Region,
        subs: List<String>,
        damageLevel: Int,
        val fatigueSwitch: Boolean) {

    val ENABLED_SUBMARINES = Submarines.parseSubmarineList(subs)
    val DAMAGE_LEVELS = Damage.getForLevel(damageLevel)

    val SHIP_REGIONS = listOf(
            kcRegion.subRegion(121, 133, 330, 105), kcRegion.subRegion(463, 133, 330, 105), // Ships 1, 2
            kcRegion.subRegion(121, 246, 330, 105), kcRegion.subRegion(463, 246, 330, 105), // Ships 3, 4
            kcRegion.subRegion(121, 359, 330, 105), kcRegion.subRegion(463, 359, 330, 105)  // Ships 5, 6
    )

    val SHIP_LIST_REGION = kcRegion.subRegion(351, 97, 449, 376)

    val fleetSubsRegions = mutableSetOf<Region>()

    init {
        Settings.AutoWaitTimeout = 1f
        Settings.MinSimilarity = 0.8
        kcRegion.autoWaitTimeout = 1.0
        ImagePath.add(javaClass.classLoader.getResource("images"))
    }

    fun switchSubs(): Boolean {
        // Cache the subs so we don't need to check them everytime
        fillFleetCache()

        val regionsToSwitch = mutableSetOf<Region>()
        println("Checking for ships that need switching!")

        fleetSubsRegions.parallelForEach({ region ->
            val statusRegion = region.subRegion(161, 10, 164, 44)
            val number = SHIP_REGIONS.indexOf(region) + 1
            println("Checking Ship $number")
            when {
                statusRegion.has("status/fleetcomp_dmg_repair.png") -> {
                    println("Ship $number is under repair")
                    regionsToSwitch.add(region)
                }
                statusRegion.has(DAMAGE_LEVELS.map(Damage::image).toSet(), DMG_SIMILARITY) -> {
                    println("Ship $number is under the damage threshold")
                    regionsToSwitch.add(region)
                }
                fatigueSwitch && statusRegion.has(Fatigued.values().map(Fatigued::image).toSet(), FATIGUE_SIMILARITY) -> {
                    println("Ship $number is under the fatigue threshold")
                    regionsToSwitch.add(region)
                }
            }
        })

        println("Checking complete! Ships ${regionsToSwitch.map { SHIP_REGIONS.indexOf(it) + 1 }.sorted()} need switching!")
        if (regionsToSwitch.map { switch(it) }.toSet().contains(false)) {
            println("Not all subs could be replaced!")
            return false
        } else {
            println("All subs were replaced!")
            return true
        }
    }

    private fun fillFleetCache() {
        if (fleetSubsRegions.isEmpty()) {
            println("Current ship cache is empty, scanning for SS and SSVs!")
            SHIP_REGIONS.parallelForEach({ region ->
                val number = SHIP_REGIONS.indexOf(region) + 1
                when {
                    region.has("ship_class_ss.png", similarity = CLASS_SIMILARITY) -> {
                        fleetSubsRegions.add(region)
                        println("Ship $number is an SS")
                    }
                    region.has("ship_class_ssv.png", similarity = CLASS_SIMILARITY) -> {
                        fleetSubsRegions.add(region)
                        println("Ship $number is an SSV")
                    }
                }
            })
            println("Scan complete! Ships ${fleetSubsRegions.map { SHIP_REGIONS.indexOf(it) + 1 }.sorted()} were found as SS(V)")
        }
    }

    private fun switch(region: Region): Boolean {
        val number = SHIP_REGIONS.indexOf(region) + 1
        println("Switching ship $number!")
        region.subRegion(245, 66, 78, 33).clickRandomly()
        SHIP_LIST_REGION.wait("nav/fleetcomp_shiplist_sort_arrow.png")

        println("Checking shiplist sort order and moving to first page if necessary!")
        while (SHIP_LIST_REGION.doesntHave("nav/fleetcomp_shiplist_sort_type.png")) {
            SHIP_LIST_REGION.clickAndRest("nav/fleetcomp_shiplist_sort_arrow.png")
        }
        kcRegion.hoverRandomly()
        val startingPage = findStartingPage()
        if (startingPage < 0) return false

        for (pgNumber in startingPage..10) {
            println("Starting search at page $pgNumber")
            if (SHIP_LIST_REGION.has("subs/fleetcomp_shiplist_submarine.png")) {
                val entries = mutableListOf<Match>()
                ENABLED_SUBMARINES.parallelForEach({ sub ->
                    println("Scanning for ${sub.subName}")
                    SHIP_LIST_REGION.findAllOrEmpty(sub.pattern(0.95))
                            .let {
                                if (it.isNotEmpty()) {
                                    println("Found ${it.size} ${sub.subName} that should be checked")
                                    entries.addAll(it)
                                }
                            }
                })
                for (entry in entries) {
                    entry.clickRandomly()
                    TimeUnit.SECONDS.sleep(1)
                    when {
                        SHIP_LIST_REGION.subRegion(278, 325, 125, 45)
                                .doesntHave(Pattern("nav/fleetcomp_shiplist_ship_switch_button.png").exact()) -> {
                            println("Can't switch with this sub type!")
                            SHIP_LIST_REGION.checkAndClick("nav/fleetcomp_shiplist_pg1.png")
                        }
                        SHIP_LIST_REGION.subRegion(264, 62, 160, 40).let {
                            it.doesntHave(Damage.UNDER_REPAIR.pattern(DMG_SIMILARITY)) &&
                            it.doesntHave(DAMAGE_LEVELS.map { it.pattern(DMG_SIMILARITY) }.toSet())
                        } -> {
                            println("Found a free submarine! Swapping it in")
                            SHIP_LIST_REGION.checkAndClick("nav/fleetcomp_shiplist_ship_switch_button.png")
                            TimeUnit.SECONDS.sleep(1)
                            return true
                        }
                        else -> {
                            println("Submarine not available, moving on!")
                            SHIP_LIST_REGION.checkAndClick("nav/fleetcomp_shiplist_pg1.png")
                        }
                    }
                }
            } else {
                println("No more available subs, couldn't replace ship $number")
                return false
            }
            println("Couldn't find any subs on this page, switching to page ${pgNumber + 1}")
            SHIP_LIST_REGION.clickAndRest("nav/fleetcomp_shiplist_pg${pgNumber + 1}.png")
        }
        return false
    }

    private fun findStartingPage(): Int {
        for (pgNumber in 1..11) {
            SHIP_LIST_REGION.clickAndRest("nav/fleetcomp_shiplist_pg$pgNumber.png")
            if (SHIP_LIST_REGION.has("subs/fleetcomp_shiplist_submarine.png")) {
                return pgNumber
            } else {
                println("Did not find any subs in any pages")
                return -1
            }
        }
        return -1
    }

    fun clearSubCache() = fleetSubsRegions.clear()
}



