package com.anilkilinc.superlivetutorial.video

import android.content.Context
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilkilinc.superlivetutorial.Constants
import com.anilkilinc.superlivetutorial.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.rtm.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(private val repo: Repo):ViewModel() {

    val TAG = this::class.java.simpleName

    private var drag = false
    private var startX = 0
    private var startY = 0
    private var totalX = 0
    private var totalY = 0
    private var xLimit = 0
    private var yLimit = 0

    // client instance
    private var mRtmClient: RtmClient? = null

    // channel instance
    private var mRtmChannel: RtmChannel? = null

    val cameraX: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val cameraY: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val messageList:MutableLiveData<MutableList<String>> by lazy {
        MutableLiveData<MutableList<String>>()
    }

    init {
        cameraX.value = -1f
        cameraX.value = -1f
    }

    private fun <T> MutableLiveData<MutableList<T>>.add(item: T) {
        if(this.value == null) {
            this.value = mutableListOf()
        }
        val updatedItems = this.value as ArrayList
        updatedItems.add(item)
        this.value = updatedItems
    }


    fun setCameraParams(xLimit:Int, yLimit:Int) {
        this.xLimit = xLimit
        this.yLimit = yLimit
    }

    fun handleTouchEvent(motionEvent: MotionEvent, x:Float?, y:Float?) {
        if(cameraX.value == -1f) {
            cameraX.value = x
            cameraY.value = y
        }

        when(motionEvent.action) {
            0 -> {
                drag = true
                startX = motionEvent.rawX.toInt()
                startY = motionEvent.rawY.toInt()
            }
            1 ->{
                drag = false
            }
            2 -> {
                if(drag) {
                    val endX = motionEvent.rawX.toInt()
                    val endY = motionEvent.rawY.toInt()
                    var xDif = endX - startX
                    var yDif = endY - startY
                    startX = endX
                    startY = endY

                    if(xDif > 0) {
                        if(totalX < xLimit) {
                            if(totalX + xDif > xLimit) {
                                xDif = xLimit - totalX
                            }
                            cameraX.value = cameraX.value?.plus(xDif)
                            totalX += xDif
                        }
                    } else if(xDif < 0) {
                        if(totalX > 0) {
                            if(totalX + xDif < 0) {
                                xDif = 0 - totalX
                            }
                            cameraX.value = cameraX.value?.plus(xDif)
                            totalX += xDif
                        }
                    }

                    if(yDif > 0) {
                        if(totalY < yLimit) {
                            if(totalY + yDif > yLimit) {
                                yDif = yLimit - totalY
                            }
                            cameraY.value = cameraY.value?.plus(yDif)
                            totalY += yDif
                        }
                    } else if(yDif < 0) {
                        if(totalY > 0) {
                            if(totalY + yDif < 0) {
                                yDif = 0 - totalY
                            }
                            cameraY.value = cameraY.value?.plus(yDif)
                            totalY += yDif
                        }
                    }
                }
            }
        }
    }

    fun initRtmService(context:Context, uid:String) {
        val listener = object : RtmClientListener {
            override fun onConnectionStateChanged(p0: Int, p1: Int) {}
            override fun onTokenExpired() {}
            override fun onTokenPrivilegeWillExpire() {}
            override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {}
            override fun onMessageReceived(p0: RtmMessage?, p1: String?) {
                onMessageReceived(p0?.text ?: "?", p1.toString())
            }
        }

        try {
            mRtmClient = RtmClient.createInstance(context, Constants.APP_ID, listener);

            mRtmClient!!.login(null, uid, object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {}
                override fun onFailure(errorInfo: ErrorInfo) {
                    val text: CharSequence =
                        "User: " + uid.toString() + " failed to log in to Signaling!" + errorInfo.toString()
                }
            })

            val mRtmChannelListener: RtmChannelListener = object : RtmChannelListener {
                override fun onMemberCountUpdated(i: Int) {}
                override fun onAttributesUpdated(list: List<RtmChannelAttribute>) {}
                override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
                    onMessageReceived(fromMember.userId, message.text)
                }
                override fun onMemberJoined(member: RtmChannelMember) {}
                override fun onMemberLeft(member: RtmChannelMember) {}
            }

            // Create an <Vg k="MESS" /> channel
            mRtmChannel = mRtmClient?.createChannel(Constants.ROOM_ID, mRtmChannelListener)

            // Join the <Vg k="MESS" /> channel
            mRtmChannel!!.join(object : ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {}
                override fun onFailure(errorInfo: ErrorInfo) {
                    val text: CharSequence = "User: $uid failed to join the channel!$errorInfo"
                }
            })

        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    // Button to send channel message
    fun onClickSendChannelMsg(text:String) {

        // Create <Vg k="MESS" /> message instance
        val message = mRtmClient!!.createMessage()
        message.text = text

        // Send message to channel
        mRtmChannel!!.sendMessage(message, object : ResultCallback<Void?> {
            override fun onSuccess(aVoid: Void?) {
                onMessageSent(message.text)
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                val text = """Message fails to send to channel ${mRtmChannel!!.id} Error: $errorInfo"""
                onError(text)
            }
        })
    }

    fun onError(text:String) {
        viewModelScope.launch {
            messageList.add(text)
        }
    }

    fun onMessageReceived(userId: String, text: String) {
        viewModelScope.launch {
            messageList.add("$userId: $text")
        }
    }

    fun onMessageSent(text: String) {
        viewModelScope.launch {
            messageList.add(text)
        }
    }
}