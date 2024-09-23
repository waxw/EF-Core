package com.miyako.core

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.miyako.FileExt
import com.miyako.core.databinding.ActivityFileBinding

class FileActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = ActivityFileBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.btnPrivate.setOnClickListener {
      FileExt.privetFile(this)
      FileExt.privetFile1(this)
      FileExt.privetCache(this)
    }

    binding.btnExternal.setOnClickListener {
      FileExt.externalFile(this, null)
      FileExt.externalCache(this)
      FileExt.externalFiles(this, null)
      FileExt.externalCaches(this)
      FileExt.externalFile1(this, null)
      FileExt.externalFile(this, Environment.DIRECTORY_MUSIC)
      FileExt.externalFile(this, "hahaha")
    }
  }
}
