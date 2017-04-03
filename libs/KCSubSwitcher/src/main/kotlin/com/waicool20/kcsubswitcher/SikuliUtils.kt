package com.waicool20.kcsubswitcher

import org.sikuli.basics.Settings
import org.sikuli.script.*
import java.util.*
import java.util.concurrent.TimeUnit

typealias xCoord = Int
typealias yCoord = Int
typealias Width = Int
typealias Height = Int

fun Region.subRegion(x: xCoord, y: yCoord, width: Width, height: Height): Region {
    val xCoord = if (x in 0..w) (this.x + x) else w
    val yCoord = if (y in 0..h) (this.y + y) else h
    val newWidth = if (width in 0..w) width else w
    val newHeight = if (height in 0..w) height else h
    with(Region(xCoord, yCoord, newWidth, newHeight)) {
        autoWaitTimeout = this@subRegion.autoWaitTimeout
        return this
    }
}

enum class Quadrant {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

fun Region.getQuadrant(quadrant: Quadrant) = with(center) {
    when (quadrant) {
        Quadrant.TOP_LEFT -> subRegion(0, 0, x, y)
        Quadrant.TOP_RIGHT -> subRegion(x, 0, x, y)
        Quadrant.BOTTOM_LEFT -> subRegion(0, y, x, y)
        Quadrant.BOTTOM_RIGHT -> subRegion(x, y, x, y)
    }
}

/**
 * Find something in a region or return null instead of raising an Exception
 */

fun <PSI> Region.findOrNull(psi: PSI,
                            similarity: Double = Settings.MinSimilarity,
                            timeout: Double = Settings.AutoWaitTimeout.toDouble()): Match? = when (psi) {
    is String -> exists(Pattern(psi).similar(similarity.toFloat()), timeout)
    is Pattern -> exists(Pattern(psi).similar(similarity.toFloat()), timeout)
    is Image -> exists(Pattern(psi).similar(similarity.toFloat()), timeout)
    else -> throw IllegalArgumentException()
}


fun <PSI> Region.findAllOrEmpty(psi: PSI): List<Match> {
    val matches = mutableListOf<Match>()
    try {
        findAll(psi)?.forEachRemaining { matches.add(it) }
    } catch (e: FindFailed) {
        Unit // Ignore
    }
    return matches
}

/**
 * Region exist utils
 */

fun <PSI> Region.has(psi: PSI, similarity: Double = Settings.MinSimilarity) =
        findOrNull(psi, similarity, 0.5) != null

fun <PSI> Region.has(images: Set<PSI>, similarity: Double = Settings.MinSimilarity) =
        images.parallelMap({ has(it, similarity) }).contains(true)

/**
 * Inverse of region exists utils
 */

fun <PSI> Region.doesntHave(psi: PSI, similarity: Double = Settings.MinSimilarity) =
        !has(psi, similarity)

fun <PSI> Region.doesntHave(images: Set<PSI>, similarity: Double = Settings.MinSimilarity): Boolean =
        !has(images, similarity)

/**
 * Waiting utilities
 */

fun <PSI> Region.waitUntilThisAppears(psi: PSI,
                                      similarity: Double = Settings.MinSimilarity,
                                      timeout: Double = Settings.FOREVER.toDouble()) = when (psi) {
    is String -> wait(Pattern(psi).similar(similarity.toFloat()), timeout)
    is Pattern -> wait(Pattern(psi).similar(similarity.toFloat()), timeout)
    is Image -> wait(Pattern(psi).similar(similarity.toFloat()), timeout)
    else -> throw IllegalArgumentException()
}

fun <PSI> Region.waitUntilThisDissappears(psi: PSI,
                                          similarity: Double = Settings.MinSimilarity,
                                          timeout: Double = Settings.FOREVER.toDouble()) = when (psi) {
    is String -> wait(Pattern(psi).similar(similarity.toFloat()), timeout)
    is Pattern -> wait(Pattern(psi).similar(similarity.toFloat()), timeout)
    is Image -> wait(Pattern(psi).similar(similarity.toFloat()), timeout)
    else -> throw IllegalArgumentException()
}

/**
 * Utility class to make common clicking actions more readable
 */
class Clicker<out PSI>(val region: Region, val target: PSI) {

    fun <PSI2> untilThisDisappears(waitTarget: PSI2) {
        while (region.has(waitTarget)) normally()
    }

    fun <PSI2> untilThisAppears(waitTarget: PSI2) {
        while (region.doesntHave(waitTarget)) normally()
    }

    fun normally(times: Int = 1,
                 minMillis: Long = 200, maxMillis: Long = 500,
                 usePreviousMatch: Boolean = false) {
        randomly(times, usePreviousMatch)
        TimeUnit.MILLISECONDS.sleep(Random().nextLong(minMillis, maxMillis))
    }

    fun ifItExists() {
        if (region != target && region.has(target)) normally()
    }

    private fun randomly(times: Int = 1, usePreviousMatch: Boolean = false) {
        val RNG = Random()
        repeat(times) {
            if (region == target) {
                with (region) {
                    val dx = RNG.nextInt((w / 2.2).toInt()) * RNG.nextSign()
                    val dy = RNG.nextInt((h / 2.2).toInt()) * RNG.nextSign()
                    region.click(Location(center.x + dx, center.y + dy))
                }
            } else {
                val match = if (usePreviousMatch) region.lastMatch else region.findOrNull(target)
                if (match != null) match.clickItself().randomly()
            }
            TimeUnit.MILLISECONDS.sleep(100)
        }
    }
}

/**
 * Function that keeps on clicking target untilThisAppears target is seen
 */

fun <PSI> Region.clickOn(target: PSI) = Clicker(this, target)

fun Region.clickItself() = Clicker(this, this)

/**
 * Find minimum similarity for something to have match, it will sweep from max to min
 */
fun <PSI> Region.findMinimumSimilarity(psi: PSI, min: Double = 0.0, max: Double = 1.0, steps: Double = 0.05): Double {
    var similarity = max
    while (similarity >= min) {
        println("Checking similarity $similarity for $psi")
        if (this.findOrNull(psi) != null) return similarity
        similarity -= steps
    }
    return -1.0
}

/**
 * Random hover function
 */

fun Region.hoverRandomly(times: Int = 1) {
    val RNG = Random()
    repeat(times) {
        hover(Location(x + RNG.nextInt(w), y + RNG.nextInt(h)))
        TimeUnit.MILLISECONDS.sleep(100)
    }
}
