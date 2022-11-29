package com.anilkilinc.superlivetutorial.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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

    val TAG = "!!!"
    private lateinit var binding: ActivityVideoBinding
    private lateinit var vm:VideoViewModel
    private lateinit var fullscreenContent: SurfaceView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)

    private var mCamera: Camera? = null
    private var mPreview: SurfaceView? = null

    var videoEngine:RtcEngine? = null
    var isJoined = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[VideoViewModel::class.java]
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.surfaceMain
        fullscreenContent.setOnClickListener { toggle() }

        fullscreenContentControls = binding.fullscreenContentControls

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        binding.dummyButton.setOnTouchListener(delayHideTouchListener)
        binding.dummyButton.setOnClickListener {
            joinChannel()
        }

        //todo recycler view or list view
        val scrollView = binding.linChatPanel.parent as ScrollView
        vm.message.observe(this) {
            if (it.size > 0) {
                val tw = TextView(this, null)
                tw.text = it[it.size-1]
                tw.setTextColor(Color.WHITE)
                binding.linChatPanel.addView(tw)
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        setLocalViewDrag()

        runBlocking {
            launch {
                initVideoEngine()
            }
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
            val config = RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = Constants.APP_ID;
            config.mEventHandler = mRtcEventHandler;
            videoEngine = RtcEngine.create(config);
            // By default, the video module is disabled, call enableVideo to enable it.
            videoEngine?.enableVideo();
        } catch (e:Exception) {
            Log.e(TAG, e.toString());
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
        binding.surfaceMain.visibility = View.VISIBLE
        // Start local preview.
        videoEngine?.startPreview()
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        var id = Random().nextInt()
        videoEngine?.joinChannel(null, Constants.ROOM_ID, id, options)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setLocalViewDrag() {
        mPreview = binding.surfaceLocal

        //calculate how many pixels camera view can be moved in the screen
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val viewWidth = resources.getDimension(R.dimen.camera_preview_width).toInt()
        val viewHeight = resources.getDimension(R.dimen.camera_preview_height).toInt()
        val xLimit = screenWidth - viewWidth
        val yLimit = screenHeight - viewHeight

        val cameraFrame = binding.frSurface

        vm.setCameraParams(xLimit, yLimit)

        vm.cameraX.observe(this) {
            cameraFrame.x = it
        }
        vm.cameraY.observe(this){
            cameraFrame.y = it
        }

        mPreview?.setOnTouchListener { view, motionEvent ->
            vm.handleTouchEvent(motionEvent, cameraFrame.x, cameraFrame.y);
            true
        }
    }


    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }

    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    /** A safe way to get an instance of the Camera object. */
    fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}