package com.miyako

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.miyako.core.debug
import com.miyako.core.debugLog
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
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
    val format = SimpleDateFormat("yyyy-MM-dd, hh:mm:ss").format(Date())
    val file = File(parent, "$format.txt")
    "file: $file, exist: ${file.exists()}".debugLog()
    if (file.exists().not()) {
      val result = file.createNewFile()
      "create: $result".debugLog()
    }
    file.bufferedWriter().use {
      it.write("$format, $content")
      it.flush()
    }
  }

  fun checkReadPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
  }

  val readStoragePermissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
      arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

  fun Context.hasReadStoragePermission() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) true
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) true
    else if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) true
    else false

  private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

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

  fun publicFile(type: String?) {
    Environment.getExternalStoragePublicDirectory(type).handleFile {
      createTestFile(it, "public file: $type")
    }
  }

  fun mediaStore(context: Context, uri: Uri, total: Int) {
    "mediaStore: $uri".debugLog()
    var cnt = 0
    queryMediaStore(context, uri, sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} desc") { _, _, _ ->
      cnt++
      cnt >= total
    }
  }

  private fun queryMediaStore(
    context: Context,
    uri: Uri,
    path: String = "",
    fileName: String = "",
    sortOrder: String? = null,
    action: (Cursor, String, Uri?) -> Boolean
  ): Boolean {
    "queryMediaStore uri: $uri, $path/$fileName".debugLog()
    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) return false
    val projection = buildList {
      add(MediaStore.MediaColumns._ID)
      add(MediaStore.MediaColumns.DISPLAY_NAME)
      add(MediaStore.MediaColumns.DATA)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        add(MediaStore.MediaColumns.RELATIVE_PATH)
    }.toTypedArray()

    val (select, args) = if (path.isEmpty() && fileName.isEmpty()) {
      null to null
    } else {
      val selectList = mutableListOf<String>()
      val argList = mutableListOf<String>()

      val isFile = fileName.isNotEmpty()
      val dirName = if (path.isEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ""
        else Environment.getExternalStorageDirectory().path
      } else {
        path.dropLast(if (path.endsWith("/")) 1 else 0)
          .drop(if (path.startsWith("/")) 1 else 0)
      }
      "dir: $dirName".debugLog()

      if (isFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          selectList.add(MediaStore.MediaColumns.RELATIVE_PATH + " = ?")
          argList.add("$dirName/")
        } else {
          selectList.add(MediaStore.MediaColumns.DATA + " LIKE ?")
          argList.add("%$dirName/$fileName")
        }
      }
      selectList.add(MediaStore.MediaColumns.DISPLAY_NAME + " = ? ")
      argList.add(if (isFile) fileName else dirName)

      val select = selectList.joinToString(" AND ")
      "select: $select, $argList".debugLog()
      select to argList.toTypedArray()
    }

    context.contentResolver.query(uri, projection, select, args, sortOrder).use { cursor ->
      if (cursor != null && cursor.moveToFirst()) {
        do {
          val id: Long = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
          val name: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: ""
          val data: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
          val resultUri = Uri.withAppendedPath(uri, id.toString())
          "query result: $name, uri: $resultUri, path: $data".debugLog()
          // 使用数据
          File(data).debug {
            "isExists: ${this?.exists()}".debugLog()
          }
          val item = action(cursor, name, resultUri).not()
        } while (item && cursor.moveToNext())
      }
    }
    return true
  }

  fun mediaStoreReadDir(context: Context, uri: Uri, path: String): Uri? {
    var result: Uri? = null
    queryMediaStore(context, uri, path) { _, name, resultUri ->
      runTrue(name, path) {
        result = resultUri
      }
    }
    return result
  }

  fun mediaStoreReadFile(context: Context, uri: Uri, path: String, fileName: String): Uri? {
    var result: Uri? = null
    queryMediaStore(context, uri, path, fileName) { _, name, resultUri ->
      runTrue(name, fileName) {
        result = resultUri
      }
    }
    return result
  }

  fun getFinalPath(path: String, fileName: String): String {
    return "${Environment.getExternalStorageDirectory().path}${if (path.isNotEmpty()) "/$path" else ""}/$fileName"
  }

  /**
   * 通过 File IO API 创建文件，需要权限，Android 10 及以上无效
   */
  fun writeFile(
    path: String,
    fileName: String,
    inputStream: InputStream,
    action: ((File) -> Unit)? = null
  ) {
    "fileWrite: $path/$fileName".debugLog()
    val filePath = getFinalPath(path, fileName)

    "new file: $filePath".debugLog()
    val file = File(filePath)
    if (file.parentFile?.exists() != true) {
      file.parentFile?.mkdirs()
    }
    if (file.exists().not()) {
      file.createNewFile()
    }
    val bos = BufferedOutputStream(file.outputStream())
    inputStream.use { istream ->
      bos.use { bos ->
        val buff = ByteArray(1024)
        var count: Int
        while (istream.read(buff).apply { count = this } != -1) {
          bos.write(buff, 0, count)
        }
        bos.flush()
        "file write success".debugLog()
      }
    }
    action?.invoke(file)
  }

  /**
   * 通过 File IO API 访问文件，需要权限，Android 10 及以上无效
   */
  fun readFile(
    path: String,
    fileName: String,
    action: ((File?) -> Unit)? = null
  ) {
    "fileRead: $path/$fileName".debugLog()
    val filePath = getFinalPath(path, fileName)
    val file = File(filePath)
    "read file: ${file.absolutePath}, ${file.canRead()}, ${file.canWrite()}, ${file.canExecute()}".debugLog()
    if (file.parentFile?.exists() != true) {
      file.parentFile?.mkdirs()
    }
    if (file.exists().not()) {
      "file not exists".debugLog()
    } else {
      action?.invoke(file)
    }
  }

  /**
   *
   * 通过 MediaStore 创建文件，插入后需要触发 MediaStore 的扫描才能重新再 MediaStore 中查询到结果
   *
   * @param uri 插入到 MediaStore 的哪张表，Images/Audio/Video 必须匹配 MIME，Files 表不需要
   *            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
   *            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
   *            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
   * @param path 插入的目录
   * @param fileName 文件名
   */
  fun mediaStoreWrite(
    context: Context,
    uri: Uri,
    path: String,
    fileName: String,
    inputStream: InputStream,
    contentValues: ContentValues = ContentValues(),
    action: ((Uri) -> Unit)? = null
  ): Boolean {
    "mediaStoreWrite: $uri, $path/$fileName".debugLog()
    val filePath = getFinalPath(path, fileName)
    "new file: $filePath".debugLog()
    val file = File(filePath)
    if (file.parentFile?.exists() != true) {
      file.parentFile?.mkdirs()
    }
    // 设置文件名称
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // 设置文件路径
      contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path)
    } else {
      contentValues.put(MediaStore.MediaColumns.DATA, filePath)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    queryMediaStore(context, uri, path, fileName) { _, _, _ ->
      false
    }

    // ContentUri 表示操作哪个数据库, contentValues 表示要插入的数据内容
    "insert before".debugLog()
    val insertUri = context.contentResolver.insert(uri, contentValues) ?: return false
    "insert: $insertUri".debugLog()
    return runCatching {
      // 向 path/filename 文件中插入数据
      val os: OutputStream? = context.contentResolver.openOutputStream(insertUri)
      if (os == null) {
        "contentResolver openOutputStream failure".debugLog()
        return false
      }
      val bos = BufferedOutputStream(os)
      inputStream.use { istream ->
        bos.use { bos ->
          val buff = ByteArray(1024)
          var count: Int
          while (istream.read(buff).apply { count = this } != -1) {
            bos.write(buff, 0, count)
          }
          action?.invoke(insertUri)
          bos.flush()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(insertUri, contentValues, null, null)
          }
          "mediaStoreWrite success: $insertUri".debugLog()
        }
      }
    }.onSuccess {
      scanMediaStore(context, arrayOf(filePath))
    }.onFailure {
      "onFailure: $it".debugLog()
    }.isSuccess
  }

  private fun scanMediaStore(context: Context, paths: Array<String>, callback: ((String?, Uri?) -> Unit)? = null) {
    MediaScannerConnection.scanFile(context, paths, null) { path, uri ->
      callback?.invoke(path, uri)
    }
  }

  private inline fun runTrue(arg1: Any?, arg2: Any?, block: () -> Unit) = if (arg1 == arg2) {
    block()
    true
  } else false
}
