package com.kotlinisgood.boomerang.ui.home

enum class OrderState(val order: String) {
    CREATE_RECENT("create_recent"),
    CREATE_OLD("create_old"),
    MODIFY_RECENT("modify_recent"),
    MODIFY_OLD("modify_old"),
    NONE("create_recent")
}