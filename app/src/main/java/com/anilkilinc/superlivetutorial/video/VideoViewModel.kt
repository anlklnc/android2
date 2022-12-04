package com.anilkilinc.superlivetutorial.video

import android.content.Context
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilkilinc.superlivetutorial.Message
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor():ViewModel() {

    lateinit var rtmProvider:RtmServiceProvider
    lateinit var rtcProvider:RtcServiceProvider
    var gson:Gson

    private var drag = false
    private var startX = 0
    private var startY = 0
    private var totalX = 0
    private var totalY = 0
    private var xLimit = 0
    private var yLimit = 0

    private var keyboardOpen = false

    val cameraX: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val cameraY: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    val messageList:MutableLiveData<MutableList<String>> by lazy {
        MutableLiveData<MutableList<String>>()
    }

    val receivedGift:MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val chatVisible:MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    init {
        cameraX.value = -1f
        cameraX.value = -1f
        chatVisible.value = false
        gson = GsonBuilder().create()
        messageList.value = mutableListOf()
    }

    private fun <T> MutableLiveData<MutableList<T>>.add(item: T) {
        val updatedItems = this.value as ArrayList
        updatedItems.add(item)
        this.value = updatedItems
    }


    fun initRtcService(context:Context, listener: RtcListener) {
        rtcProvider = RtcServiceProvider(context, listener)
    }

    fun joinRtcService() {
        rtcProvider.joinChannel()
    }

    fun joinRtmService(context:Context, uid:String) {
        viewModelScope.launch(Dispatchers.IO) {
            rtmProvider = RtmServiceProvider(context, object : RtmListener{
                override fun onError(text: String) {
                    viewModelScope.launch {
                        updateMessageList(text)
                    }
                }

                override fun onMessageReceived(userId: String, messageJson: String) {
                    viewModelScope.launch {
                        var message = getMessage(messageJson)
                        handleIncomingMessage(message, userId)
                    }
                }

                override fun onMessageSent(messageJson: String) {
                    viewModelScope.launch {
                        val message = getMessage(messageJson)
                        var displayMessage = message.text
                        if(message.type == Message.MESSAGE_TYPE_GIFT) {
                            displayMessage = "Gift sent: " + displayMessage
                        }
                        updateMessageList(displayMessage)
                    }
                }
            })

            rtmProvider.join(uid)
        }
    }

    private fun getMessage(text:String):Message {
        return gson.fromJson(text, Message::class.java)
    }

    fun handleIncomingMessage(message: Message, userId: String) {
        when(message.type){
            Message.MESSAGE_TYPE_CHAT -> {
                updateMessageList("$userId: ${message.text}")
            }
            Message.MESSAGE_TYPE_GIFT -> {
                displayGiftReceived(message.text)
            }
        }
    }

    private fun displayGiftReceived(giftId: String) {
        receivedGift.value = giftId
    }

    var showPanelCount = 0
    private fun updateMessageList(text:String) {
        messageList.add(text)
        chatVisible.value = true;
        if (!keyboardOpen) {
            viewModelScope.launch {
                showPanelCount++
                delay(4000)
                showPanelCount--
                if(showPanelCount == 0 && !keyboardOpen) {
                    chatVisible.value = false;
                }
            }
        }
    }

    fun onClickSendChannelMsg(text:String) {
        val message = Message(text, Message.MESSAGE_TYPE_CHAT)
        val json = gson.toJson(message, Message::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            rtmProvider.sendMessage(json)
        }
    }

    fun onClickSendGift(giftId:String) {
        val message = Message(giftId, Message.MESSAGE_TYPE_GIFT)
        val json = gson.toJson(message, Message::class.java)
        viewModelScope.launch(Dispatchers.IO) {
            rtmProvider.sendMessage(json)
        }
    }

    fun onKeyboardOpen() {
        keyboardOpen = true
        chatVisible.value = true
    }

    fun onKeyboardClose() {
        keyboardOpen = false
        viewModelScope.launch {
            delay(4000)
            chatVisible.value = false
        }
    }

    fun onDestroy() {
        rtcProvider.stop()
        // Destroy the engine in a sub-thread to avoid congestion
        viewModelScope.launch {
            rtcProvider.destroy()
        }
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
}