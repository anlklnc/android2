package com.anilkilinc.superlivetutorial.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.anilkilinc.superlivetutorial.Constants
import com.anilkilinc.superlivetutorial.R
import com.anilkilinc.superlivetutorial.databinding.ActivityVideoBinding
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
class VideoActivity : AppCompatActivity() {

    val TAG = this::class.java.simpleName
    private lateinit var binding: ActivityVideoBinding
    private lateinit var vm:VideoViewModel

    private var localPreview: SurfaceView? = null
    private lateinit var scrollView: ScrollView

    var videoEngine:RtcEngine? = null
    var isJoined = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[VideoViewModel::class.java]
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btnJoin.setOnClickListener {
            joinChannel()
        }

        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it).setView(R.layout.dialog_gift)
        }

        binding.fabGift.setOnClickListener {
            builder.create().show()
        }

        binding.etMessage.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                appendMesssage(textView.text.toString())
                binding.etMessage.text.clear()
                //hide keyboard
                val inputManager: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    this.currentFocus!!.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
            true
        }

        //todo recycler view or list view
        scrollView = binding.linChatPanel.parent as ScrollView
        vm.message.observe(this) {
            if (it.size > 0) {
                var text = it[it.size-1]
                appendMesssage(text)
            }
        }

        setLocalViewDrag()

        runBlocking {
            launch {
                initVideoEngine()
            }
        }
    }

    private fun appendMesssage(text:String) {
        val tw = TextView(this, null)
        tw.text = text
        tw.setTextColor(Color.WHITE)
        binding.linChatPanel.addView(tw)
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoEngine?.stopPreview()
        videoEngine?.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        runBlocking { // this: CoroutineScope
            launch { // launch a new coroutine and continue
                RtcEngine.destroy()
                videoEngine = null
            }
        }
    }

    private fun initVideoEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = this
            config.mAppId = Constants.APP_ID
            config.mEventHandler = mRtcEventHandler
            videoEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            videoEngine?.enableVideo()
        } catch (e:Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i(TAG,"Remote user joined $uid")

            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            Log.i(TAG,"Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.i(TAG,"Remote user offline $uid $reason")
            runOnUiThread { binding.surfaceMain.setVisibility(View.GONE) }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        var remoteVideoView = binding.surfaceMain
//        remoteVideoView.setZOrderMediaOverlay(true)
        videoEngine?.setupRemoteVideo(
            VideoCanvas(
                remoteVideoView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteVideoView.setVisibility(View.VISIBLE)
    }

    private fun setupLocalVideo() {
        var localVideoView = binding.surfaceLocal
        // Pass the SurfaceView object to Agora so that it renders the local video.
        videoEngine?.setupLocalVideo(
            VideoCanvas(
                localVideoView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions()

        // For a Video call, set the channel profile as COMMUNICATION.
        options.channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION
        // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
        options.clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
        // Display LocalSurfaceView.
        setupLocalVideo()
        // Start local preview.
        videoEngine?.startPreview()
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        var id = Random().nextInt()
        videoEngine?.joinChannel(null, Constants.ROOM_ID, id, options)

        //change bottom views
        binding.btnJoin.visibility = View.GONE
        binding.etMessage.visibility = View.VISIBLE
        binding.fabGift.visibility = View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setLocalViewDrag() {
        localPreview = binding.surfaceLocal

        //calculate how many pixels camera view can be moved in the screen
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val viewWidth = resources.getDimension(R.dimen.camera_preview_width).toInt()
        val viewHeight = resources.getDimension(R.dimen.camera_preview_height).toInt()
        val xLimit = screenWidth - viewWidth
        val yLimit = screenHeight - viewHeight

        vm.setCameraParams(xLimit, yLimit)

        vm.cameraX.observe(this) {
            localPreview?.x = it
        }
        vm.cameraY.observe(this){
            localPreview?.y = it
        }

        localPreview?.setOnTouchListener { view, motionEvent ->
            vm.handleTouchEvent(motionEvent, localPreview?.x, localPreview?.y);
            true
        }
    }
}