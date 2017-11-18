package com.waicool20.kcsubswitcher

import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.sikuli.basics.Settings
import org.sikuli.script.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

val CLASS_SIMILARITY = 0.77
val DMG_SIMILARITY = 0.66
val FATIGUE_SIMILARITY = 0.96

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Main Function")
    // Test Case
    val switcher = KCSubSwitcher(
            Screen().subRegion(80, 105, 800, 480), // Manual coordinates to kc window
            listOf("ssv", "i-8", "i-19", "i-58", "i-168", "ro-500", "u-511"),
            0,
            true
    )
    // App.focus("Chromium")
    val time = measureTimeMillis {
        switcher.switchSubs()
    }
    logger.info("Finished executing in $time ms!")
}

class KCSubSwitcher(
        val kcRegion: Region,
        subs: List<String>,
        damageLevel: Int,
        val fatigueSwitch: Boolean) {

    init {
        Settings.AutoWaitTimeout = 1f
        Settings.MinSimilarity = 0.8
        kcRegion.autoWaitTimeout = 1.0
        ImagePath.add(javaClass.classLoader.getResource("images"))
    }

    val logger = LoggerFactory.getLogger(javaClass)

    val ENABLED_SUBMARINES = Submarines.parseSubmarineList(subs)
    val DAMAGE_LEVELS = Damage.getForLevel(damageLevel)

    val SHIP_REGIONS = listOf(
            kcRegion.subRegion(121, 133, 330, 105), kcRegion.subRegion(463, 133, 330, 105), // Ships 1, 2
            kcRegion.subRegion(121, 246, 330, 105), kcRegion.subRegion(463, 246, 330, 105), // Ships 3, 4
            kcRegion.subRegion(121, 359, 330, 105), kcRegion.subRegion(463, 359, 330, 105)  // Ships 5, 6
    )

    val SHIP_LIST_REGION = kcRegion.subRegion(351, 97, 449, 376)

    val fleetSubsRegions = mutableSetOf<Region>()

    fun switchSubs(): Boolean {
        // Cache the subs so we don't need to check them everytime
        fillFleetCache()

        val regionsToSwitch = mutableSetOf<Region>()
        logger.info("Checking for ships that need switching!")

        fleetSubsRegions.parallelForEach({ region ->
            val statusRegion = region.subRegion(161, 10, 174, 44)
            val number = SHIP_REGIONS.indexOf(region) + 1
            logger.info("Checking Ship $number")
            when {
                statusRegion.has("status/fleetcomp_dmg_repair.png") -> {
                    logger.info("Ship $number is under repair")
                    regionsToSwitch.add(region)
                }
                statusRegion.has(DAMAGE_LEVELS.map(Damage::image).toSet(), DMG_SIMILARITY) -> {
                    logger.info("Ship $number is under the damage threshold")
                    regionsToSwitch.add(region)
                }
                fatigueSwitch && statusRegion.has(Fatigued.values().map(Fatigued::image).toSet(), FATIGUE_SIMILARITY) -> {
                    logger.info("Ship $number is under the fatigue threshold")
                    regionsToSwitch.add(region)
                }
            }
        }, newFixedThreadPoolContext(fleetSubsRegions.size, ""))

        logger.info("Checking complete! Ships ${regionsToSwitch.map { SHIP_REGIONS.indexOf(it) + 1 }.sorted()} need switching!")
        if (regionsToSwitch.map { switch(it) }.toSet().contains(false)) {
            logger.info("Not all subs could be replaced!")
            return false
        } else {
            logger.info("All subs were replaced!")
            return true
        }
    }

    private fun fillFleetCache() {
        if (fleetSubsRegions.isEmpty()) {
            logger.info("Current ship cache is empty, scanning for SS and SSVs!")
            val hasSS = ENABLED_SUBMARINES.any { !it.isSSV }
            val hasSSV = ENABLED_SUBMARINES.any { it.isSSV }
            SHIP_REGIONS.parallelForEach({ region ->
                val number = SHIP_REGIONS.indexOf(region) + 1
                when {
                    hasSS && region.has("ship_class_ss.png", similarity = CLASS_SIMILARITY) -> {
                        fleetSubsRegions.add(region)
                        logger.info("Ship $number is an SS")
                    }
                    hasSSV && region.has("ship_class_ssv.png", similarity = CLASS_SIMILARITY) -> {
                        fleetSubsRegions.add(region)
                        logger.info("Ship $number is an SSV")
                    }
                }
            }, newFixedThreadPoolContext(SHIP_REGIONS.size, ""))
            logger.info("Scan complete! Ships ${fleetSubsRegions.map { SHIP_REGIONS.indexOf(it) + 1 }.sorted()} were found as SS(V)")
        }
    }

    private fun switch(region: Region): Boolean {
        val number = SHIP_REGIONS.indexOf(region) + 1
        logger.info("Switching ship $number!")
        region.subRegion(245, 66, 78, 33).clickItself().normally()
        SHIP_LIST_REGION.waitFor("nav/fleetcomp_shiplist_sort_arrow.png").toAppear()

        logger.info("Checking shiplist sort order and moving to first page if necessary!")
        SHIP_LIST_REGION.clickOn("nav/fleetcomp_shiplist_sort_arrow.png").untilThisAppears("nav/fleetcomp_shiplist_sort_type.png")
        kcRegion.hoverRandomly()
        val startingPage = findStartingPage()
        if (startingPage < 0) return false

        for (pgNumber in startingPage..10) {
            logger.info("Starting search at page $pgNumber")
            if (SHIP_LIST_REGION.has("subs/fleetcomp_shiplist_submarine.png")) {
                val entries = mutableMapOf<Submarines, MutableList<Match>>()
                ENABLED_SUBMARINES.parallelForEach({ sub ->
                    SHIP_LIST_REGION.findAllOrEmpty(sub.pattern(0.99))
                            .let {
                                if (it.isNotEmpty()) {
                                    logger.info("Found ${it.size} ${sub.subName} that should be checked")
                                    entries[sub]?.addAll(it.toMutableList()) ?: entries.put(sub, it.toMutableList())
                                }
                            }
                }, newFixedThreadPoolContext(ENABLED_SUBMARINES.size, ""))


                SubEntryLoop@ for (subEntry in entries) {
                    for (foundEntry in subEntry.value) {
                        foundEntry.clickItself().normally()
                        when {
                            SHIP_LIST_REGION.subRegion(278, 325, 125, 45)
                                    .doesntHave(Pattern("nav/fleetcomp_shiplist_ship_switch_button.png").exact()) -> {
                                logger.info("Can't switch with ${subEntry.key.subName}, skipping them all!")
                                while (SHIP_LIST_REGION.doesntHave("nav/fleetcomp_shiplist_next_button.png")) {
                                    SHIP_LIST_REGION.subRegion(0, 0, 237, 376).clickItself().normally()
                                }
                                continue@SubEntryLoop
                            }
                            SHIP_LIST_REGION.subRegion(264, 62, 169, 40).let {
                                it.doesntHave(Damage.UNDER_REPAIR.pattern(DMG_SIMILARITY)) &&
                                        it.doesntHave(DAMAGE_LEVELS.map { it.pattern(DMG_SIMILARITY) }.toSet()) &&
                                        it.doesntHave(Fatigued.values().map(Fatigued::image).toSet())
                            } -> {
                                logger.info("Found a free submarine! Swapping submarines!")
                                SHIP_LIST_REGION.clickOn("nav/fleetcomp_shiplist_ship_switch_button.png").ifItExists()
                                kcRegion.subRegion(736, 98, 64, 32).waitFor("nav/edit_fleet_name_button.png").toAppear()
                                TimeUnit.MILLISECONDS.sleep(250)
                                return true
                            }
                            else -> {
                                logger.info("Submarine not available, moving on!")
                                SHIP_LIST_REGION.clickOn("nav/fleetcomp_shiplist_pg1.png").ifItExists()
                            }
                        }
                    }
                }
            } else {
                logger.info("No more available subs, couldn't replace ship $number")
                return false
            }
            logger.info("Couldn't find any subs on this page, switching to page ${pgNumber + 1}")
            SHIP_LIST_REGION.clickOn("nav/fleetcomp_shiplist_pg${pgNumber + 1}.png").normally()
        }
        return false
    }

    private fun findStartingPage(): Int {
        for (pgNumber in 1..11) {
            SHIP_LIST_REGION.clickOn("nav/fleetcomp_shiplist_pg$pgNumber.png").normally()
            if (SHIP_LIST_REGION.has("subs/fleetcomp_shiplist_submarine.png")) {
                return pgNumber
            }
        }
        logger.info("Did not find any subs in any pages")
        return -1
    }

    fun clearSubCache() = fleetSubsRegions.clear()
}



