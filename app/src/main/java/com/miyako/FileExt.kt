package com.miyako

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.miyako.core.debug
import com.miyako.core.debugLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object FileExt {

  // fun requestReadPermission(context: Activity, block: () -> Unit) {
  //   if (checkReadPermission(context)) {
  //     block.invoke()
  //   } else {
  //     ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
  //   }
  // }

  private fun createTestFile(parent: File, content: String) {
    val file = File(parent, "test.txt")
    "file: $file, exist: ${file.exists()}".debugLog()
    if (file.exists().not()) {
      val result = file.createNewFile()
      "create: $result".debugLog()
    }
    file.bufferedWriter().use {
      it.write(SimpleDateFormat("yyyy-MM-dd, hh:mm:ss").format(Date()) + ", $content")
      it.flush()
    }
  }

  fun checkReadPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun checkWritePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun readExternalFile(path: String): File? {
    return when (Build.VERSION.SDK_INT) {
      in 0..Build.VERSION_CODES.M -> readExternalFileBelow23(path)
      else -> null
    }
  }

  private fun readExternalFileBelow23(path: String): File? {
    return try {
      File(Environment.getExternalStorageDirectory(), path).debug {
        "readExternalFileBelow23: ${this?.absolutePath}".debugLog()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  // private fun readExternalFileAbove24(context: Context, path: String): File? {
  //   return try {
  //     FileProvider.getUriForFile(context, context.packageName, )
  //     File(Environment.getExternalStorageDirectory(), path).debug {
  //       "readExternalFileBelow23: ${this?.absolutePath}".debugLog()
  //     }
  //   } catch (e: Exception) {
  //     e.printStackTrace()
  //     null
  //   }
  // }

  private fun File.handleFile(action: (File) -> Unit): File {
    val file = this
    val isExists = file.exists()
    "dir exists: $isExists, ${file.absolutePath}".debugLog()
    if (isExists.not()) {
      val result = file.mkdirs()
      "create: $result".debugLog()
    }
    action(file)
    return file
  }

  fun privetFile(context: Context) {
    "privetFile".debugLog()
    val file: File = File(context.filesDir, "miyako")
    file.handleFile {
      createTestFile(it, "private file")
    }
  }

  fun privetFile1(context: Context) {
    "privetFile1".debugLog()
    val file: File = File(context.filesDir.parentFile, "miyako1")
    file.handleFile {
      createTestFile(it, "parent private file")
    }
  }

  fun privetCache(context: Context) {
    "privetCache".debugLog()
    val file: File = File(context.cacheDir, "miyako")
    file.handleFile {
      createTestFile(it, "private cache")
    }
  }

  fun externalFile(context: Context, type: String?) {
    "externalFile".debugLog()
    val file: File = File(context.getExternalFilesDir(type), "miyako")
    file.handleFile {
      createTestFile(it, "external app file")
    }
  }

  fun externalFile1(context: Context, type: String?) {
    "externalFile1".debugLog()
    val file: File = File(context.getExternalFilesDir(type)?.parentFile, "miyako")
    file.handleFile {
      createTestFile(it, "external app file")
    }
  }

  fun externalCache(context: Context) {
    "externalCache".debugLog()
    val file: File = File(context.externalCacheDir, "miyako")
    file.handleFile {
      createTestFile(it, "external app cache")
    }
  }

  fun externalFiles(context: Context, type: String?) {
    "externalFiles".debugLog()
    context.getExternalFilesDirs(type).forEach {
      val file: File = File(it, "miyako")
      file.handleFile {
        createTestFile(it, "external app files")
      }
    }
  }

  fun externalCaches(context: Context) {
    "externalCaches".debugLog()
    context.externalCacheDirs.forEach {
      val file: File = File(it, "miyako")
      file.handleFile {
        createTestFile(it, "external app caches")
      }
    }
  }
}
