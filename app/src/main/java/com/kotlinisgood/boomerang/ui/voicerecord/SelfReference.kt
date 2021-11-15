package com.kotlinisgood.boomerang.ui.voicerecord

class SelfReference<T>(val initializer: SelfReference<T>.() -> T) {
    val self: T by lazy {
        inner ?: throw IllegalStateException("Do not use `self` until initialized.")
    }

    private val inner = initializer()
}

fun <T> selfReference(initializer: SelfReference<T>.() -> T): T {
    return SelfReference(initializer).self
}