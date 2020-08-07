package com.example.background.workers

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.PROGRESS

import timber.log.Timber

class UpLoadFileWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {

        makeStatusNotification("Uploading Compressed Image", applicationContext)

        // Sleep for debugging purposes
        sleep()
        (0..100 step 10).forEach {
            setProgressAsync(workDataOf(PROGRESS to it))
            sleep()
        }

        return try {

            val resourceUri = inputData.getString(KEY_IMAGE_URI)
            ImageUtils.uploadFile(Uri.parse(resourceUri))

            Result.success()
        } catch (exception: Exception) {
            Timber.e(exception)
            Result.failure()
        }
    }
}