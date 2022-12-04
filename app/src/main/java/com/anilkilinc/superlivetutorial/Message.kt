package com.anilkilinc.superlivetutorial

data class Message (
    val text:String,
    val type:Int,
) {
    companion object {
        const val MESSAGE_TYPE_CHAT = 1
        const val MESSAGE_TYPE_GIFT = 2
        const val GIFT_TYPE_1 = "Dollar"
        const val GIFT_TYPE_2 = "Euro"
        const val GIFT_TYPE_3 = "Bitcoin"
    }
}