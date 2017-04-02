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
    return Region(xCoord, yCoord, newWidth, newHeight)
}

enum class Quadrant {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

fun Region.getQuadrant(quadrant: Quadrant) = when (quadrant) {
    Quadrant.TOP_LEFT -> subRegion(0, 0, w / 2, h / 2)
    Quadrant.TOP_RIGHT -> subRegion(w / 2, 0, w / 2, h / 2)
    Quadrant.BOTTOM_LEFT -> subRegion(0, h / 2, w / 2, h / 2)
    Quadrant.BOTTOM_RIGHT -> subRegion(w / 2, h / 2, w / 2, h / 2)
}

/**
 * Find something in a region or return null instead of raising an Exception
 */

fun <PSI> Region.findOrNull(psi: PSI) = try {
    find(psi)
} catch (e: FindFailed) {
    null
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

fun <PSI> Region.has(psi: PSI, similarity: Double = Settings.MinSimilarity) = when (psi) {
    is String -> exists(Pattern(psi).similar(similarity.toFloat())) != null
    is Pattern -> exists(Pattern(psi).similar(similarity.toFloat())) != null
    is Image -> exists(Pattern(psi).similar(similarity.toFloat())) != null
    else -> throw IllegalArgumentException()
}

fun <PSI> Region.has(images: Set<PSI>, similarity: Double = Settings.MinSimilarity) = when(images.firstOrNull()) {
    is String -> images.parallelMap({ has(it, similarity) }).contains(true)
    is Pattern -> images.parallelMap({ has(it, similarity) }).contains(true)
    is Image -> images.parallelMap({ has(it, similarity) }).contains(true)
    else -> false
}

/**
 * Inverse of region exists utils
 */

fun <PSI> Region.doesntHave(psi: PSI, similarity: Double = Settings.MinSimilarity) =
        !has(psi, similarity)

fun <PSI> Region.doesntHave(images: Set<PSI>, similarity: Double = Settings.MinSimilarity): Boolean =
        !has(images, similarity)


/**
 * Random click generator
 */

fun Match.clickRandomly() {
    val RNG = Random()
    val dx = RNG.nextInt(w / 2) * RNG.nextSign()
    val dy = RNG.nextInt(h / 2) * RNG.nextSign()
    Pattern(image).targetOffset(dx, dy)
    click()
}

fun <PSI> Region.clickRandomly(psi: PSI, usePreviousMatch: Boolean = false) {
    try {
        (if (usePreviousMatch) lastMatch else findOrNull(psi))?.let(Match::clickRandomly)
    } catch (e: FindFailed) {
        e.printStackTrace()
    }
}

fun Region.clickRandomly(times: Int = 1) {
    val RNG = Random()
    repeat(times) {
        click(Location(x + RNG.nextInt(w), y + RNG.nextInt(h)))
        TimeUnit.MILLISECONDS.sleep(100)
    }
}

/**
 * Random rest after clicking generator
 */

fun <PSI> Region.clickAndRest(psi: PSI,
                              minMillis: Long = 200, maxMillis: Long = 500,
                              usePreviousMatch: Boolean = false) {
    clickRandomly(psi, usePreviousMatch)
    TimeUnit.MILLISECONDS.sleep(Random().nextLong(minMillis, maxMillis))
}

/**
 * Check before clicking
 */
fun <PSI> Region.checkAndClick(psi: PSI, minMillis: Long = 200, maxMillis: Long = 500) {
    if (has(psi)) clickAndRest(psi, minMillis, maxMillis, true)
}

/**
 * Utility class to make common clicking actions more readable
 */
class Clicker<out PSI>(val region: Region, val source: PSI) {

    fun <PSI2> untilThisDisappears(target: PSI2) {
        while (region.has(target)) region.clickAndRest(source)
    }

    fun <PSI2> untilThisAppears(target: PSI2) {
        while (region.doesntHave(target)) region.clickAndRest(source)
    }
}

/**
 * Function that keeps on clicking source untilThisAppears target is seen
 */

fun <PSI> Region.clickOn(source: PSI) = Clicker(this, source)

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
