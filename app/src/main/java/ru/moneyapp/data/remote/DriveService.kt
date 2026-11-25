package ru.moneyapp.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface DriveService {
    companion object {
        const val BASE_URL = "https://www.googleapis.com/drive/v3/"
    }

    @GET("files")
    suspend fun listFiles(
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name,createdTime,modifiedTime)"
    ): DriveFileList

    @POST("files")
    suspend fun createFile(): DriveFile

    @GET("files/{fileId}")
    suspend fun getFile(@Path("fileId") fileId: String): DriveFile
}

@JsonClass(generateAdapter = true)
data class DriveFileList(
    val files: List<DriveFile>?,
    val nextPageToken: String?
)

@JsonClass(generateAdapter = true)
data class DriveFile(
    val id: String,
    val name: String,
    val createdTime: String?,
    val modifiedTime: String?
)
