package com.anilkilinc.superlivetutorial.video

interface RtmListener {

    fun onError(text:String)

    fun onMessageReceived(userId: String, text: String)

    fun onMessageSent(text: String)
}