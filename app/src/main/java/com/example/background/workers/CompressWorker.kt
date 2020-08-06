package com.example.background.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import timber.log.Timber

class CompressWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {

        return try {

            val imageUriString = inputData.getString(KEY_IMAGE_URI)
            makeStatusNotification("Compressing Image - $imageUriString", applicationContext)
            Timber.e("Compressing Image - $imageUriString")
            // Sleep for debugging purposes
            sleep()

            val imagePaths = inputData.keyValueMap
                .filter { it.key.startsWith(imageUriString.toString())}
                .map { it.value as String }

            val zipFile = ImageUtils.createZipFile(applicationContext, imagePaths.toTypedArray())

            val outputData = workDataOf(KEY_IMAGE_URI to zipFile.path.toString())

            Result.success(outputData)
        } catch (exception: Exception) {
            Timber.e(exception)
            Result.failure()
        }
    }
}