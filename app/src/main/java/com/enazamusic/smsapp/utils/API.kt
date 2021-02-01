package com.enazamusic.smsapp.utils

import android.content.Context
import com.enazamusic.smsapp.model.LastTransferAttempt
import com.enazamusic.smsapp.model.ListViewElement
import com.enazamusic.smsapp.model.QueueElement
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

object API : KoinComponent {

    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()
    private const val baseUrl = "https://google.com/"
    private val context: Context by inject()

    private fun getCall(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .build()
        return client.newCall(request).execute()
    }

    private fun postCall(url: String, json: JSONObject): Response {
        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
        return client.newCall(request).execute()
    }

    private fun executeFromAssets(fileName: String): Response? {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use {
                it.readText()
            }
            Response.Builder()
                .code(200)
                .protocol(Protocol.HTTP_1_0)
                .message("ok")
                .request(Request.Builder().url("http://$fileName").build())
                .body(jsonString.toResponseBody(jsonMediaType))
                .build()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logUserMessages(elements: List<ListViewElement>) {
        val url = "$baseUrl?method=metrics.LogUserMessages"
        val msisdn = Prefs.getPhoneNumber()
        withContext(bgDispatcher) {
            elements.filter { !it.isSentToServer }.forEach { element ->
                val json = Gson().toJson(element)
                val finalJson = JSONObject(json).put("msisdn", msisdn)
                val resp = postCall(url, finalJson)
                Prefs.setLastTransferAttempt(
                    LastTransferAttempt(
                        System.currentTimeMillis(),
                        resp.code
                    )
                )
                if (resp.code == 200) {
                    element.isSentToServer = true
                    Prefs.saveSentToServerElement(element)
                }
            }
        }
    }

    fun logUserMessage(element: ListViewElement) = ioScope.launch {
        val url = "$baseUrl?method=metrics.LogUserMessages"
        val msisdn = Prefs.getPhoneNumber()
        withContext(bgDispatcher) {
            val json = Gson().toJson(element)
            val finalJson = JSONObject(json).put("msisdn", msisdn)
            try {
                val resp = postCall(url, finalJson)
                Prefs.setLastTransferAttempt(
                    LastTransferAttempt(
                        System.currentTimeMillis(),
                        resp.code
                    )
                )
                if (resp.code == 200) {
                    element.isSentToServer = true
                    Prefs.saveSentToServerElement(element)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun checkNewQueueMessages(): List<QueueElement> {
        val url = "$baseUrl?method="
        //val resp = getCall(url)
        val resp = executeFromAssets("checkNewQueueMessages.json")
        val responseBody = resp?.body?.string() ?: return emptyList()
        try {
            val jsonArray =
                JsonParser.parseString(responseBody).asJsonObject.getAsJsonArray("data").toString()
            return Gson().fromJson(
                jsonArray,
                Array<QueueElement>::class.java
            ).toList()
        } catch (e: Exception) {
        }
        return emptyList()
    }
}