package com.miyako.core

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.miyako.FileExt
import com.miyako.FileExt.hasReadStoragePermission
import com.miyako.FileExt.readStoragePermissions
import com.miyako.core.databinding.ActivityFileBinding
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date

class FileActivity : AppCompatActivity() {

  var uri: Uri? = null

  private val safResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode != RESULT_OK) return@registerForActivityResult
    val uri = result.data?.data
    "SAF uri: $uri".debugLog()
    if (uri.toString().contains("document")) {
      BufferedReader(InputStreamReader(contentResolver.openInputStream(uri!!))).use {
        it.readLines().forEach {
          "read: $it".debugLog()
        }
      }
    } else {
      binding.ivImage.setImageURI(uri)
    }
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

  lateinit var binding: ActivityFileBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityFileBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.assetsFileSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
      if (isChecked) {
        binding.tvWriteFile.text = "write assets file"
      } else {
        binding.tvWriteFile.text = "write txt"
      }
    }

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

    binding.btnGrantUri.setOnClickListener {
      val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      val pkg = "com.android.providers.media"
      grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
      val input = getTargetFile()
      val file = FileExt.mediaStoreReadFile(this, uri, input.first, input.second)
      "file: $file".debugLog()
      binding.ivImage.setImageURI(file)
    }

    binding.btnMediaReadDir.setOnClickListener {
      val uri = MediaStore.Files.getContentUri(external)
      val input = getTargetFile()
      FileExt.mediaStoreReadDir(this, uri, input.first)?.let {
        "dir: ${it.path}".debugLog()
      }
    }

    binding.btnMediaReadText.setOnClickListener {
      val uri = MediaStore.Files.getContentUri(external)
      val input = getTargetFile()
      FileExt.mediaStoreReadFile(this, uri, input.first, input.second)?.let {
        BufferedReader(InputStreamReader(contentResolver.openInputStream(it))).use {
          it.readLines().forEach {
            "read: $it".debugLog()
          }
        }
      }
    }

    binding.btnMediaWrite.setOnClickListener {
      val uri = MediaStore.Files.getContentUri(external)
      val target = getTargetFile()
      val inputStream = getWriteFile()
      // FileExt.mediaStoreWrite(
      //   this,
      //   uri,
      //   target.first,
      //   inputStream.first,
      //   inputStream.second
      // )
      FileExt.writeFile(target.first, inputStream.first, inputStream.second)
    }

    binding.btnMediaWriteImage.setOnClickListener {
      val uri = MediaStore.Files.getContentUri(external)

      val target = getTargetFile()

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
        target.first,
        target.second,
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

    binding.btnSafImage1.setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK)
      intent.type = "image/*"
      safResultLauncher.launch(intent)
    }

    binding.btnSafImage2.setOnClickListener {
      val intent = Intent(Intent.ACTION_GET_CONTENT)
      intent.type = "image/*"
      safResultLauncher.launch(intent)
    }

    binding.btnSafImage3.setOnClickListener {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = "image/*"
      safResultLauncher.launch(intent)
    }

    binding.btnSafImage4.setOnClickListener {
      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
      safResultLauncher.launch(intent)
    }

    binding.btnSafImage5.setOnClickListener {
      val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
      intent.type = "text/*"
      safResultLauncher.launch(intent)
    }

    binding.deleteSafFile.setOnClickListener {
      uri?.let {
        val delSuccess = DocumentsContract.deleteDocument(contentResolver, it)
        "$it delete success $delSuccess".debugLog()
      }
    }

    binding.writeSafFile.setOnClickListener {
      uri?.let { uri->
        DocumentFile.fromTreeUri(this, uri)
          // 在文件夹内创建新文件夹
          ?.createDirectory("test")
          ?.apply {
            // 在新文件夹内创建文件
            val fileName = "${System.currentTimeMillis()}.txt"
            createFile("text/plain", fileName)

            // 通过文件名找到文件
            findFile(fileName)?.uri?.let {
              try {
                // 在文件中写入内容
                contentResolver.openOutputStream(it)?.write("hello world".toByteArray())
                "$uri write success success".debugLog()
              }catch (e:Exception){
                e.printStackTrace()
              }
            }
          }
      }
    }
  }

  fun getWriteFile(): Pair<String, InputStream> {
    return if (binding.assetsFileSwitch.isChecked) {
      "Weather.pdf" to readWeatherPdf()
    } else {
      val date = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(Date())
      "$date.txt" to ByteArrayInputStream(("$date---text file").toByteArray())
    }
  }

  fun getTargetFile(): Pair<String, String> {
    val text = binding.etTarget.text?.trim() ?: ""
    if (text.endsWith("/")) return text.toString() to ""
    val idx = text.lastIndexOf("/")
    return if (idx != -1) {
      text.substring(0, idx) to text.substring(idx + 1)
    } else if (text.contains(".")) "" to text.toString()
    else text.toString() to ""
  }

  fun readWeatherPdf(): InputStream {
    return assets.open("Weather.pdf")
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    "code: $requestCode, $grantResults".debugLog()
  }
}
