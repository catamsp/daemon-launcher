package com.catamsp.Daemon.ui

import android.os.Bundle
import android.view.MenuItem
import com.catamsp.Daemon.R
import com.catamsp.Daemon.databinding.LegalInfoBinding

class LegalInfoActivity : UIObjectActivity() {
    private lateinit var binding: LegalInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise layout
        binding = LegalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setTitle(R.string.legal_info_title)
        setSupportActionBar(binding.legalInfoAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        applyFont(binding.root)
        applyFontToToolbar(binding.legalInfoAppbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }
}