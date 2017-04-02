package com.waicool20.kcsubswitcher

import java.util.*

fun Random.nextInt(min: Int, max: Int) = nextInt(max - min + 1) + min
fun Random.nextLong(min: Long, max: Long) = (nextDouble() * (max - min)).toLong() + min
fun Random.nextSign() = if (nextInt(100) > 50) 1 else -1

