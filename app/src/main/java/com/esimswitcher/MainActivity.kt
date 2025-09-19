package com.esimswitcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.esimswitcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ESIMViewModel
    private lateinit var adapter: ESIMProfileAdapter
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[ESIMViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        if (checkPermissions()) {
            loadProfiles()
        } else {
            requestPermissions()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ESIMProfileAdapter { profile ->
            showSwitchConfirmation(profile)
        }
        
        binding.profilesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.profiles.observe(this) { profiles ->
            adapter.updateProfiles(profiles)
            binding.emptyStateText.visibility = if (profiles.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.refreshButton.isEnabled = !isLoading
            binding.refreshButton.text = if (isLoading) "Loading..." else "Refresh Profiles"
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.refreshButton.setOnClickListener {
            loadProfiles()
        }
    }
    
    private fun loadProfiles() {
        viewModel.loadESIMProfiles(this)
    }
    
    private fun showSwitchConfirmation(profile: ESIMProfile) {
        if (profile.isActive) {
            Toast.makeText(this, "Profile is already active", Toast.LENGTH_SHORT).show()
            return
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Switch eSIM Profile")
            .setMessage("Switch to ${profile.displayName}?")
            .setPositiveButton("Yes") { _, _ ->
                switchProfile(profile)
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun switchProfile(profile: ESIMProfile) {
        viewModel.switchProfile(this, profile) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, getString(R.string.profile_switched), Toast.LENGTH_SHORT).show()
                    loadProfiles()
                } else {
                    Toast.makeText(this, getString(R.string.switch_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadProfiles()
            } else {
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }
}