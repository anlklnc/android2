package com.anilkilinc.superlivetutorial.video

import android.content.Context
import android.view.MotionEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilkilinc.superlivetutorial.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(private val repo: Repo):ViewModel() {

    val TAG = this::class.java.simpleName

    lateinit var provider:RtmServiceProvider

    private var drag = false
    private var startX = 0
    private var startY = 0
    private var totalX = 0
    private var totalY = 0
    private var xLimit = 0
    private var yLimit = 0

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

    fun joinRtmService(context:Context, uid:String) {
        viewModelScope.launch {
            provider = RtmServiceProvider(context, object : RtmListener{
                override fun onError(text: String) {
                    viewModelScope.launch {
                        messageList.add(text)
                    }
                }

                override fun onMessageReceived(userId: String, text: String) {
                    viewModelScope.launch {
                        if (text.startsWith(".//")) {

                        } else if (text.startsWith("***")) {
                            val output = text.substring(3)
                        }
                        messageList.add("$userId: $text")
                    }
                }

                override fun onMessageSent(text: String) {
                    viewModelScope.launch {
                        messageList.add(text)
                    }
                }
            })

            provider.join(uid)
        }
    }

    // Button to send channel message
    fun onClickSendChannelMsg(text:String) {
        viewModelScope.launch {
            provider.sendMessage(text)
        }
    }
}