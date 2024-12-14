package org.icpclive.reactions.recorder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.icpclive.cds.util.getLogger
import java.nio.channels.UnresolvedAddressException
import kotlin.time.Duration
import io.ktor.client.plugins.auth.Auth as AuthPlugin

class GrabberClient(grabberUrl: String, private val grabberAuth: Pair<String, String>? = null) {
    constructor(grabberUrl: Config) : this(grabberUrl.grabberUrl, grabberAuth = grabberUrl.grabberAuth)

    private val apiPath = "$grabberUrl/api/admin"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json)
        }

        grabberAuth?.let { (login, password) ->
            install(AuthPlugin) {
                basic {
                    credentials { BasicAuthCredentials(login, password) }
                    sendWithoutRequest { true }
                }
            }
        }
    }

    private suspend inline fun <reified T> sendRequest(urlSuffix: String, content: T): String {
        try {
            val request = client.post("${apiPath}${urlSuffix}") {
                contentType(ContentType.Application.Json)
                setBody(content)
            }
            val body = request.bodyAsText()
            if (request.status != HttpStatusCode.OK) {
                throw GrabberClientException("${request.status.value}: $body")
            }
            return body
        } catch (e: IOException) {
            throw GrabberClientException("Sending request failed", e)
        } catch (e: UnresolvedAddressException) {
            throw GrabberClientException("Sending request failed", e)
        }
    }

    suspend fun startRecord(peerName: String, recordId: String, timeout: Duration) {
        val response = sendRequest(
            "/record_start",
            StartRecordRequest(peerName, recordId, timeout.inWholeMilliseconds)
        )
        log.info { "Grabber record started $peerName $recordId $response" }
    }

    suspend fun stopRecord(peerName: String, recordId: String) {
        val response = sendRequest(
            "/record_stop",
            StopRecordRequest(peerName, recordId)
        )
        log.info { "Grabber record stopped $peerName $recordId $response" }
    }

    suspend fun uploadRecord(peerName: String, recordId: String) {
        val response = sendRequest(
            "/record_upload",
            UploadRecordRequest(peerName, recordId)
        )
        log.info { "Grabber record upload requested $peerName $recordId $response" }
    }

    class GrabberClientException(message: String, cause: Exception? = null) : RuntimeException(message, cause)

    @Serializable
    private class StartRecordRequest(val peerName: String, val recordId: String, val timeout: Long)

    @Serializable
    private class StopRecordRequest(val peerName: String, val recordId: String)

    @Serializable
    private class UploadRecordRequest(val peerName: String, val recordId: String)

    companion object {
        val log by getLogger()
    }

    data class Config(val grabberUrl: String, val grabberAuth: Pair<String, String>?)
}
