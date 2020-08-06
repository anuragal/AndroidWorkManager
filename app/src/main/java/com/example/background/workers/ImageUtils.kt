package com.example.background.workers

import android.content.Context
import android.net.Uri
import okhttp3.*
import timber.log.Timber
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object ImageUtils {

    private const val LOG_TAG = "ImageUtils"
    private const val SERVER_UPLOAD_PATH = "http://10.0.2.2:8000/sync" //local server URL

    private const val DIRECTORY_OUTPUTS = "outputs"
    private const val COMPRESS_BUFFER_CHUNK = 1024

    private val okHttpClient by lazy { OkHttpClient() }

    private const val MULTIPART_NAME = "file"

    private fun getOutputDirectory(applicationContext: Context): File {
        return File(applicationContext.filesDir, DIRECTORY_OUTPUTS).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun cleanFiles(applicationContext: Context) {
        val outputDirectory = getOutputDirectory(applicationContext)

        outputDirectory.listFiles()?.forEach { it.delete() }
    }

    fun createZipFile(applicationContext: Context, files: Array<String>): Uri {
        val randomId = UUID.randomUUID().toString()
        val name = "$randomId.zip"

        val outputDirectory = getOutputDirectory(applicationContext)
        val outputFile = File(outputDirectory, name)

        val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
        compressFiles(zipOutputStream, files)

        return Uri.fromFile(outputFile)
    }

    private fun compressFiles(zipOutputStream: ZipOutputStream, files: Array<String>) {
        zipOutputStream.use { out ->
            files.forEach { file ->
                FileInputStream(file).use { fileInput ->
                    BufferedInputStream(fileInput).use { origin ->
                        val entry = ZipEntry(file.substring(file.lastIndexOf("/")))
                        out.putNextEntry(entry)
                        origin.copyTo(out, COMPRESS_BUFFER_CHUNK)
                    }
                }
            }
        }
    }

    fun uploadFile(fileUri: Uri) {
        val file = File(fileUri.path)

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(MULTIPART_NAME, file.name, RequestBody.create(MediaType.parse("multipart/form-data;"), file))
                .build()

        val request = Request.Builder()
                .url(SERVER_UPLOAD_PATH)
                .put(requestBody)
                .build()

        val response = okHttpClient.newCall(request).execute()

        Timber.e("onResponse - Status: ${response?.code()} Body: ${response?.body()?.string()}")
    }
}