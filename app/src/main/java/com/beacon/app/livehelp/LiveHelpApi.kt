package com.beacon.app.livehelp

import com.beacon.domain.livehelp.LiveHelpRoomCredentials
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private data class CreateRoomDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("userToken") val userToken: String,
    @SerializedName("helperToken") val helperToken: String,
    @SerializedName("expiresAt") val expiresAt: Long,
)

@Singleton
class LiveHelpApi @Inject constructor(
    private val gson: Gson,
    private val endpoints: LiveHelpEndpoints,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun createRoom(): Result<LiveHelpRoomCredentials> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${endpoints.httpBase.trimEnd('/')}/rooms")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("create_room_http_${response.code}")
                val body = response.body?.string() ?: error("empty_body")
                val dto = gson.fromJson(body, CreateRoomDto::class.java)
                LiveHelpRoomCredentials(
                    roomId = dto.roomId,
                    userToken = dto.userToken,
                    helperToken = dto.helperToken,
                    expiresAtEpochMs = dto.expiresAt,
                )
            }
        }
    }
}
