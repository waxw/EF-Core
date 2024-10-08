package com.miyako.core

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.miyako.FileExt
import com.miyako.FileExt.hasReadStoragePermission
import com.miyako.FileExt.readStoragePermissions
import com.miyako.core.databinding.ActivityFileBinding
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader

class FileActivity : AppCompatActivity() {

  private val safResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode != RESULT_OK) return@registerForActivityResult
    "SAF uri: ${result.data?.data}".debugLog()
  }

  private val pickSingleMedia =
    registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      "Single Pick uri: $uri".debugLog()
    }

  private val pickMultipleMedia =
    registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris: List<Uri> ->
      uris.forEach { uri ->
        "Multiple Pick uri: $uri".debugLog()
      }
    }

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

    binding.btnPublic.setOnClickListener {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Environment.isExternalStorageManager().not()) {
          startActivityForResult(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
              .setData(Uri.parse("package:${this.packageName}")), 0
          )
          return@setOnClickListener
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == -1) {
          requestPermissions(
            arrayOf(
              android.Manifest.permission.READ_EXTERNAL_STORAGE,
              android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 0
          )
          return@setOnClickListener
        }
      }
      // FileExt.publicFile(null)
      FileExt.publicFile(Environment.DIRECTORY_MUSIC)
      FileExt.publicFile(Environment.DIRECTORY_DOWNLOADS)
      FileExt.publicFile(Environment.DIRECTORY_DOCUMENTS)
      FileExt.publicFile("testPublic")
    }

    val external = "external"
    binding.btnMediaScan.setOnClickListener {
      FileExt.mediaStore(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 10)
      FileExt.mediaStore(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 10)
      FileExt.mediaStore(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 10)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        FileExt.mediaStore(this, MediaStore.Downloads.EXTERNAL_CONTENT_URI, 10)
      }
      FileExt.mediaStore(this, MediaStore.Files.getContentUri(external), 10)
    }

    binding.btnMediaReadImage.setOnClickListener {
      val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      // val file = FileExt.mediaStoreReadFile(this, uri, "DCIM/Screenshots", "Screenshot_20240517-164822_Weather.jpg")
      val file = FileExt.mediaStoreReadFile(this, uri, "", "image.png")
      "file: $file".debugLog()
      binding.ivImage.setImageURI(file)
    }

    binding.btnMediaReadText.setOnClickListener {
      val uri = MediaStore.Files.getContentUri(external)
      FileExt.mediaStoreReadDir(this, uri, "Documents")?.let {
        "dir: ${it.path}".debugLog()
      }
      // FileExt.mediaStoreReadFile(this, uri, "Documents", "Ai_Life_Manage_0.0_2.0_2024_07_17_17_37_32.log")?.let {
      //   BufferedReader(InputStreamReader(contentResolver.openInputStream(it))).use {
      //     it.readLines().forEach {
      //       "read: $it".debugLog()
      //     }
      //   }
      // }

      FileExt.mediaStoreReadFile(this, uri, "", "text.txt")?.let {
        BufferedReader(InputStreamReader(contentResolver.openInputStream(it))).use {
          it.readLines().forEach {
            "read: $it".debugLog()
          }
        }
      }
    }

    binding.btnMediaWrite.setOnClickListener {
      val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // MediaStore.Downloads.EXTERNAL_CONTENT_URI
        MediaStore.Files.getContentUri(external)
      } else {
        MediaStore.Files.getContentUri(external)
      }

      val content = "Media Documents, Download11111".toByteArray(Charsets.UTF_8)

      // val path = "media_write111"
      val path = "Documents"
      // val path = Environment.DIRECTORY_DOWNLOADS
      // val fileName = "media_miyako1111.txt"
      val fileName = "mediaTxxxx111222.png"

      // val inputStream = ByteArrayInputStream(content)
      val inputStream = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565).let {
        Canvas(it).run {
          drawColor(Color.GRAY)
        }
        val output = ByteArrayOutputStream()
        it.compress(Bitmap.CompressFormat.PNG, 100, output)
        ByteArrayInputStream(output.toByteArray())
      }

      FileExt.mediaStoreWrite(
        this,
        uri,
        path,
        fileName,
        inputStream
      )
    }

    binding.btnQueryImage.setOnClickListener {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!hasReadStoragePermission()) {
          requestPermissions(readStoragePermissions, 0)
          return@setOnClickListener
        }
      }
      if (hasReadStoragePermission()) {
        FileExt.mediaStore(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 10)
      }
    }

    binding.btnSafGallery.setOnClickListener {
      val intent = Intent(Intent.ACTION_GET_CONTENT)
      intent.type = "image/*"
      safResultLauncher.launch(intent)
    }

    binding.btnSafVideo.setOnClickListener {
      val intent = Intent(Intent.ACTION_GET_CONTENT)
      intent.type = "video/*"
      safResultLauncher.launch(intent)
    }

    binding.btnSafText.setOnClickListener {
      val intent = Intent(Intent.ACTION_GET_CONTENT)
      intent.type = "text/*"
      safResultLauncher.launch(intent)
    }

    binding.btnPickSingle.setOnClickListener {
      pickSingleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) // 包含图片和视频

//      singlePickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) // 只包含图片

//      singlePickMedia.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly)) //只包含视频

//      val mimeType = "image/gif"
//      singlePickMedia.launch(PickVisualMediaRequest(PickVisualMedia.SingleMimeType(mimeType))) //筛选固定类型的媒体文件
    }

    binding.btnPickMultiple.setOnClickListener {
      pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
  }
}
