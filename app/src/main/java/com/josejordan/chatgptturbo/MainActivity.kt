package com.josejordan.chatgptturbo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val API_KEY = "CHATGPTKEY"
        private const val MODEL_NAME = "gpt-3.5-turbo"
        private const val MAX_TOKENS = 200
        private const val TEMPERATURE = 1.0
    }

    private lateinit var responseTV: TextView
    private lateinit var questionTV: TextView
    private lateinit var queryEdt: TextInputEditText
    private lateinit var job: Job
    private var query: String = ""

    private fun initViews() {
        responseTV = findViewById(R.id.idTVResponse)
        questionTV = findViewById(R.id.idTVQuestion)
        queryEdt = findViewById(R.id.idEdtQuery)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()

        queryEdt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                responseTV.text = "Please wait.."
                if (queryEdt.text.toString().isNotEmpty()) {
                    query = queryEdt.text.toString()

                    // Utiliza la instancia de Job para controlar la corrutina
                    job = CoroutineScope(Dispatchers.Main).launch {
                        val response = getResponse(query)
                        processResponse(response)
                    }
                } else {
                    Toast.makeText(this, "Please enter your query..", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    private fun processResponse(response: String?) {
        response?.let {
            Log.i(TAG, "ChatGPTurbo: $response")
            questionTV.text = "Question: $query"
            queryEdt.setText("")
            responseTV.text = "ChatGPTurbo: $response"
        } ?: run {
            Toast.makeText(
                applicationContext, "Error getting response", Toast.LENGTH_SHORT
            ).show()
        }
    }


// Función que realiza la petición a la API de OpenAI y devuelve la respuesta
/*    private suspend fun getResponse(query: String): String? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val url = "https://api.openai.com/v1/chat/completions"
            val mediaType = "application/json".toMediaTypeOrNull()
            val postFields = JSONObject()
            postFields.put("model", MODEL_NAME)
            val messagesList: MutableList<JSONObject?> = ArrayList()
            val messageObj = JSONObject()
            messageObj.put("role", "user")
            messageObj.put("content", query)
            messagesList.add(messageObj)
            val messagesArr = JSONArray(messagesList)
            postFields.put("messages", messagesArr)
            postFields.put("max_tokens", MAX_TOKENS)
            postFields.put("temperature", TEMPERATURE)
            val requestBody = postFields.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            // Realiza la petición HTTP a la API de OpenAI
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error: ${response.code}")
                    return@withContext null
                }

                // Procesa la respuesta y devuelve el texto de la respuesta
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody.toString())
                val choicesArray = jsonResponse.getJSONArray("choices")
                if (choicesArray.length() > 0) {
                    val choiceObject = choicesArray.getJSONObject(0)
                    val text = choiceObject.getJSONObject("message")
                    return@withContext text.getString("content")
                }
            }
            return@withContext null
        }
    }*/

    //Cambiando a GSON
    @Throws(IOException::class)
    private suspend fun getResponse(query: String): String? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val url = "https://api.openai.com/v1/chat/completions"
            val mediaType = "application/json".toMediaTypeOrNull()
            val postFields = JSONObject()
            postFields.put("model", MODEL_NAME)
            val messagesList: MutableList<JSONObject?> = ArrayList()
            val messageObj = JSONObject()
            messageObj.put("role", "user")
            messageObj.put("content", query)
            messagesList.add(messageObj)
            val messagesArr = JSONArray(messagesList)
            postFields.put("messages", messagesArr)
            postFields.put("max_tokens", MAX_TOKENS)
            postFields.put("temperature", TEMPERATURE)
            val requestBody = postFields.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()

            // Realiza la petición HTTP a la API de OpenAI
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error: ${response.code}")
                    return@withContext null
                }

                // Utiliza la biblioteca GSON para analizar la respuesta JSON en objetos Kotlin
                val responseBody = response.body?.string()
                val gson = Gson()
                val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
                val choices = openAIResponse.choices
                if (choices.isNotEmpty()) {
                    return@withContext choices[0].text
                }
            }
            return@withContext null
        }
    }



    // Cancela todas las coroutines cuando se destruye la actividad
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

data class OpenAIChoice(
    val text: String
)




