package com.kotlinisgood.boomerang.model

enum class OrderState(val order: String) {
    CREATE("create"),
    MODIFY("modify"),
    NONE("create")
}