package com.globalfoods.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform