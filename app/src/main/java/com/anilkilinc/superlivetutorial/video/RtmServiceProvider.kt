package com.anilkilinc.superlivetutorial.video

import android.content.Context
import android.util.Log
import com.anilkilinc.superlivetutorial.Constants
import io.agora.rtm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RtmServiceProvider(context: Context, val listener:RtmListener){

    val TAG = "!!!"

    // client instance
    private var mRtmClient: RtmClient? = null

    // channel instance
    private var mRtmChannel: RtmChannel? = null

    private var mRtmChannelListener: RtmChannelListener? = null

    init {
        val clientListener = object : RtmClientListener {
            override fun onConnectionStateChanged(p0: Int, p1: Int) {}
            override fun onTokenExpired() {}
            override fun onTokenPrivilegeWillExpire() {}
            override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {}
            override fun onMessageReceived(p0: RtmMessage?, p1: String?) {
                listener.onMessageReceived(p0?.text ?: "?", p1.toString())
            }
        }

        mRtmChannelListener = object : RtmChannelListener {
            override fun onMemberCountUpdated(i: Int) {}
            override fun onAttributesUpdated(list: List<RtmChannelAttribute>) {}
            override fun onMemberJoined(member: RtmChannelMember) {}
            override fun onMemberLeft(member: RtmChannelMember) {}
            override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
                listener.onMessageReceived(fromMember.userId, message.text)
            }
        }

        try {
            mRtmClient = RtmClient.createInstance(context, Constants.APP_ID, clientListener);
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    suspend fun join(uid: String) = withContext(Dispatchers.IO){
        // Blocking network request code
        mRtmClient!!.login(null, uid, object : ResultCallback<Void?> {
            override fun onSuccess(responseInfo: Void?) {}
            override fun onFailure(errorInfo: ErrorInfo) {
                val text: CharSequence =
                    "User: " + uid.toString() + " failed to log in to Signaling!" + errorInfo.toString()
            }
        })

        // Create an <Vg k="MESS" /> channel
        mRtmChannel = mRtmClient?.createChannel(Constants.ROOM_ID, mRtmChannelListener)

        // Join the <Vg k="MESS" /> channel
        mRtmChannel!!.join(object : ResultCallback<Void?> {
            override fun onSuccess(responseInfo: Void?) {
                Log.i(TAG, "join success")
            }
            override fun onFailure(errorInfo: ErrorInfo) {
                val text: CharSequence = "User: $uid failed to join the channel!$errorInfo"
                Log.i(TAG, "join fail")
            }
        })
    }

    suspend fun sendMessage(text: String) = withContext(Dispatchers.IO){
        // Blocking network request code
        val message = mRtmClient!!.createMessage()
        message.text = text

        // Send message to channel
        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                listener.onMessageSent(message.text)
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                val text = """Message fails to send to channel ${mRtmChannel!!.id} Error: $errorInfo"""
                listener.onError(text)
            }
        })
    }

    fun destroy() {
        mRtmChannel?.leave(null)
        mRtmClient?.logout(null)
        mRtmChannel = null
        mRtmClient = null
    }
}