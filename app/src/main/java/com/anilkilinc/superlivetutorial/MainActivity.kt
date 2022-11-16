package com.anilkilinc.superlivetutorial

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.anilkilinc.superlivetutorial.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var presenter:MainPresenter

    lateinit var model: MainViewModel

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[MainViewModel::class.java]

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setSupportActionBar(binding.toolbar)

        val button = binding.content.btn
        button.setOnClickListener {
            var s = "" + presenter.i + " - " + presenter.obj.value + " - "
            Toast.makeText(this, s, Toast.LENGTH_LONG).show()

            model.update();
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        model.currentName.observe(this) { name ->
            binding.content.tw.text = name
        }
    }
}