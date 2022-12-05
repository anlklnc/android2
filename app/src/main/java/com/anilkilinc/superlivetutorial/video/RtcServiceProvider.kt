package com.anilkilinc.superlivetutorial.video

import android.content.Context
import android.util.Log
import com.anilkilinc.superlivetutorial.Constants
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

class RtcServiceProvider(context: Context, val listener: RtcListener){

    val TAG = "!!!"
    var videoEngine: RtcEngine? = null
    var isJoined = false

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i(TAG,"Remote user joined $uid")
            // Set the remote video view
            listener.setupRemoteVideoView(videoEngine, uid)
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.i(TAG,"Joined Channel $channel")
            isJoined = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.i(TAG,"Remote user offline $uid $reason")
            listener.hideRemoteVideoView()
        }
    }

    init {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = Constants.APP_ID
            config.mEventHandler = mRtcEventHandler
            videoEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            videoEngine?.enableVideo()
        } catch (e:Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun joinChannel(userId:Int) {
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        val options = ChannelMediaOptions()
        // For a Video call, set the channel profile as COMMUNICATION.
        options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION
        // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
        options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
        // Display LocalSurfaceView.
        listener.setupLocalVideoView(videoEngine, userId)
        // Start local preview.
        videoEngine?.startPreview()
        // Join the channel with a temp token.
        videoEngine?.joinChannel(null, Constants.ROOM_ID, userId, options)

        //change bottom views
        listener.onJoinedChannel(userId)
    }

    fun stop() {
        videoEngine?.stopPreview()
        videoEngine?.leaveChannel()
    }

    fun destroy() {
        RtcEngine.destroy()
        videoEngine = null
    }
}