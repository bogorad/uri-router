package com.example.uri_router

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val recycler: RecyclerView = findViewById(R.id.urlRecyclerView)
        val input: EditText = findViewById(R.id.urlInputEditText)
        val add: Button = findViewById(R.id.addButton)
        val debugSwitch: MaterialSwitch = findViewById(R.id.debugSwitch)

        val adapter = UrlPatternAdapter(onDelete = { item -> viewModel.deletePattern(item) })
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Initialize debug switch from saved setting
        debugSwitch.isChecked = SettingsManager.isDebugMode(this)
        debugSwitch.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setDebugMode(this, isChecked)
            Toast.makeText(this, if (isChecked) "Debug enabled" else "Debug disabled", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect list updates
                launch {
                    viewModel.patterns.collectLatest { list ->
                        adapter.submitList(list)
                    }
                }
                // Collect one-time events (e.g., duplicate pattern)
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MainViewModel.UiEvent.ShowMessage ->
                                Toast.makeText(applicationContext, event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        add.setOnClickListener {
            viewModel.addPattern(input.text.toString())
            input.setText("")
        }
    }
}

