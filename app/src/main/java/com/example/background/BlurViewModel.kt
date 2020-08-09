/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.*
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.CompressWorker
import com.example.background.workers.UpLoadFileWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : AndroidViewModel(application) {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    private val workManager = WorkManager.getInstance(application)

    // New instance variable for the WorkInfo
    internal val outputWorkInfos: LiveData<List<WorkInfo>>
    internal val progressWorkInfoItems: LiveData<List<WorkInfo>>

    // Add an init block to the BlurViewModel class
    init {
        // This transformation makes sure that whenever the current work Id changes the WorkInfo
        // the UI is listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
        progressWorkInfoItems = workManager.getWorkInfosByTagLiveData(TAG_PROGRESS)
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     */
    internal fun saveImage() {
        // Add WorkRequest to Cleanup temporary images
        var continuation = workManager
                .beginUniqueWork(
                        IMAGE_MANIPULATION_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(CleanupWorker::class.java)
                )

        // Add WorkRequests to blur the image the number of times requested
        val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
                .setInputData(createInputDataForUri())
                .addTag(TAG_OUTPUT)
                .addTag(TAG_PROGRESS)
                .build()

        continuation = continuation.then(blurBuilder)


        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(TAG_OUTPUT)
                .addTag(TAG_PROGRESS)
                .build()

        continuation = continuation.then(save)

        //val zipFiles = OneTimeWorkRequest.Builder(CompressWorker::class.java)
        //        .addTag(TAG_OUTPUT)
        //        .addTag(TAG_PROGRESS)
        //        .build()

        // continuation = continuation.then(zipFiles)

        // Actually start the work
        continuation.enqueue()
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     */
    internal fun syncImage() {

        val outputuriBuilder = Data.Builder()
        outputUri?.let {
            outputuriBuilder.putString(KEY_IMAGE_URI, outputUri.toString())
        }
        val outputUriDatadata = outputuriBuilder.build()

        // Add WorkRequest to Cleanup temporary images
        val continuation = workManager
                .beginUniqueWork(
                        IMAGE_MANIPULATION_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.Builder(UpLoadFileWorker::class.java)
                                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                                .setInputData(outputUriDatadata)
                                .addTag(TAG_OUTPUT)
                                .addTag(TAG_PROGRESS)
                                .build()
                )

        // Actually start the work
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    /**
     * Setters
     */
    internal fun setImageUri(uri: String?) {
        imageUri = uriOrNull(uri)
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }
}
