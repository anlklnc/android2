package com.anilkilinc.superlivetutorial.video

import io.agora.rtc2.RtcEngine

interface RtcListener {
    fun setupRemoteVideoView(engine: RtcEngine?, uid:Int)
    fun hideRemoteVideoView()
    fun setupLocalVideoView(engine: RtcEngine?, uid:Int)
    fun onJoinedChannel(id: Int)
}