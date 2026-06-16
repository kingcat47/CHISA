package com.example.chisa.backend.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {

    private const val MODEL_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

    fun getModelFile(context: Context): File =
        File(context.filesDir, MODEL_FILE_NAME)

    fun isModelDownloaded(context: Context): Boolean =
        getModelFile(context).let { it.exists() && it.length() > 0 }

    sealed class DownloadState {
        data class Progress(
            val percent: Int,
            val downloadedMb: Float,
            val totalMb: Float
        ) : DownloadState()
        object Done : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    fun downloadModel(context: Context): Flow<DownloadState> = flow {
        val destFile = getModelFile(context)
        val tempFile = File(context.filesDir, "$MODEL_FILE_NAME.tmp")

        try {
            val connection = URL(MODEL_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout   = 60_000
            connection.connect()

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val percent = if (totalBytes > 0)
                            (downloadedBytes * 100 / totalBytes).toInt() else 0
                        emit(DownloadState.Progress(
                            percent      = percent,
                            downloadedMb = downloadedBytes / 1_048_576f,
                            totalMb      = totalBytes      / 1_048_576f
                        ))
                    }
                }
            }
            tempFile.renameTo(destFile)
            emit(DownloadState.Done)

        } catch (e: Exception) {
            tempFile.delete()
            emit(DownloadState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
}