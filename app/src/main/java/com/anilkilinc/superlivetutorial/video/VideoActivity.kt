package com.anilkilinc.superlivetutorial.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.anilkilinc.superlivetutorial.Message
import com.anilkilinc.superlivetutorial.R
import com.anilkilinc.superlivetutorial.databinding.ActivityVideoBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import java.util.*


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@AndroidEntryPoint
class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var vm:VideoViewModel
    private var localPreview: SurfaceView? = null
    lateinit var adapter:ArrayAdapter<String>

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[VideoViewModel::class.java]
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btnJoin.setOnClickListener {
            vm.joinRtcService()
        }

        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it).setView(R.layout.dialog_gift).setIcon(R.drawable.ic_baseline_diamond_24)
        }

        binding.fabGift.setOnClickListener {
            val dialog = builder.create()
            dialog.show()
        }

        binding.etMessage.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = textView.text.toString()
                binding.etMessage.text.clear()

                //hide keyboard
                hideKeyboard()

                //rtm
                vm.onClickSendChannelMsg(text)
            }
            true
        }

        setLocalViewDrag()
        initObservers()
        initRtcService()
        setKeyboardListener()
    }

    private fun initObservers() {
        vm.messageList.observe(this) {
            if (it.size > 0) {
                val text = it[it.size-1]
                appendMesssage(text)
            }
        }

        vm.receivedGift.observe(this) {
            onGiftReceived(it)
        }

        vm.chatVisible.observe(this) {
            val alpha = if(it) 0.8f else 0.0f
            binding.listChat.animate().alpha(alpha).setDuration(if(it)200 else 500)
        }
    }

    fun sendGift(view:View) {
        vm.onClickSendGift(view.tag.toString())
    }

    var snack:Snackbar? = null
    private fun onGiftReceived(giftId:String) {
        if(snack != null && snack!!.isShown) {
            return
        }
        snack = Snackbar.make(binding.fabGift, getString(R.string.snackbar_gift_info), Snackbar.LENGTH_SHORT)
            .setAction(getString(R.string.snackbar_gift_show)){
                var drawable = 0
                when(giftId) {
                    Message.GIFT_TYPE_1 -> {
                        drawable = R.drawable.ic_baseline_attach_money_24
                    }
                    Message.GIFT_TYPE_2 -> {
                        drawable = R.drawable.ic_baseline_euro_24
                    }
                    Message.GIFT_TYPE_3 -> {
                        drawable = R.drawable.ic_baseline_currency_bitcoin_24
                    }
                }
                val dialog = Dialog(this)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.dialog_image)
                val iw = dialog.findViewById<ImageView>(R.id.iw_gift)
                iw?.setImageResource(drawable)
                dialog.show()
            }
        snack?.show()
    }

    private fun initRtcService() {
        vm.initRtcService(this, object : RtcListener{
            override fun setupRemoteVideoView(engine: RtcEngine?, uid: Int) {
                runOnUiThread {
                    val remoteVideoView = binding.surfaceMain
//                    remoteVideoView.setZOrderMediaOverlay(true)
                    engine?.setupRemoteVideo(
                        VideoCanvas(
                            remoteVideoView,
                            VideoCanvas.RENDER_MODE_FIT,
                            uid
                        )
                    )
                    // Display RemoteSurfaceView.
                    remoteVideoView.visibility = View.VISIBLE
                }
            }

            override fun hideRemoteVideoView() {
                runOnUiThread { binding.surfaceMain.visibility = View.GONE }
            }

            override fun setupLocalVideoView(engine: RtcEngine?, uid: Int) {
                runOnUiThread{
                    val localVideoView = binding.surfaceLocal
                    // Pass the SurfaceView object to Agora so that it renders the local video.
                    engine?.setupLocalVideo(
                        VideoCanvas(
                            localVideoView,
                            VideoCanvas.RENDER_MODE_FIT,
                            uid
                        )
                    )
                    localVideoView.bringToFront()
                }
            }

            override fun onJoinedChannel(id: Int) {
                vm.joinRtmService(this@VideoActivity, id.toString())

                runOnUiThread{
                    binding.btnJoin.visibility = View.GONE
                    binding.etMessage.visibility = View.VISIBLE
                    binding.fabGift.visibility = View.VISIBLE

                    adapter = ArrayAdapter<String>(
                        this@VideoActivity,
                        R.layout.item_list, android.R.id.text1, vm.messageList.value!!
                    )
                    binding.listChat.adapter = adapter
                }
            }
        })
    }

    private fun appendMesssage(text:String) {
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.onDestroy()
    }

    private fun hideKeyboard() {
        val inputManager: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            this.currentFocus!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun setKeyboardListener() {
        // ContentView is the root view of the layout of this activity/fragment
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r);
            val screenHeight = binding.root.rootView.height;

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            val keypadHeight = screenHeight - r.bottom;

            Log.d("!!!", "keypadHeight = $keypadHeight");

            if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                // keyboard is opened
                if (!vm.isKeyboardOpen) {
                    vm.onKeyboardOpen()
                }
            }
            else {
                // keyboard is closed
                if (vm.isKeyboardOpen) {
                    vm.onKeyboardClose()
                }
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setLocalViewDrag() {
        localPreview = binding.surfaceLocal
        val layout = binding.surfaceLocal.parent as View

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
            layout.x = it
        }
        vm.cameraY.observe(this){
            layout.y = it
        }

        localPreview?.setOnTouchListener { view, motionEvent ->
            vm.handleTouchEvent(motionEvent, localPreview?.x, localPreview?.y);
            true
        }
    }

    private fun display(text:String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}