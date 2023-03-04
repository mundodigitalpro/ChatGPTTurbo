package com.josejordan.chatgptturbo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // Constantes para utilizar en la comunicación con la API de OpenAI
    companion object {
        private const val TAG = "MainActivity"
        private const val API_KEY = "CHATGPTAPIKEY"
        private const val MODEL_NAME = "gpt-3.5-turbo"
        private const val MAX_TOKENS = 200
        private const val TEMPERATURE = 1.0
    }

    private lateinit var responseTV: TextView
    private lateinit var questionTV: TextView
    private lateinit var queryEdt: TextInputEditText
    private var query: String = ""

    // La función onCreate() se llama cuando la actividad es creada
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Asigna los TextViews y el TextInputEditText a las variables correspondientes
        responseTV = findViewById(R.id.idTVResponse)
        questionTV = findViewById(R.id.idTVQuestion)
        queryEdt = findViewById(R.id.idEdtQuery)

        // Asigna un Listener para detectar cuando el usuario pulsa la tecla 'Enviar' del teclado virtual
        queryEdt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                responseTV.text = "Please wait.."
                if (queryEdt.text.toString().isNotEmpty()) {
                    // Si el usuario ha introducido un mensaje, lanza una corrutina en segundo plano para obtener la respuesta
                    query = queryEdt.text.toString()
                    GlobalScope.launch(Dispatchers.IO) {
                        val response = getResponse(query)
                        withContext(Dispatchers.Main) {
                            // Una vez obtenida la respuesta, actualiza los TextViews con la pregunta y la respuesta
                            processResponse(response)
                        }
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

    // Función que procesa la respuesta obtenida de la API de OpenAI
    private fun processResponse(response: String?) {
        response?.let {
            Log.i(TAG, "ChatGPTurbo: $response")
            questionTV.text = "Question: $query"
            queryEdt.setText("")
            responseTV.text = "ChatGPTurbo: $response"
        } ?: run {
            Toast.makeText(this, "Error getting response", Toast.LENGTH_SHORT).show()
        }
    }

    // Función que realiza la petición a la API de OpenAI y devuelve la respuesta
    @Throws(IOException::class)
    private fun getResponse(query: String): String? {
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
                return null
            }

            // Procesa la respuesta y devuelve el texto de la respuesta
            val responseBody = response.body?.string()
            val jsonResponse = JSONObject(responseBody.toString())
            val choicesArray = jsonResponse.getJSONArray("choices")
            if (choicesArray.length() > 0) {
                val choiceObject = choicesArray.getJSONObject(0)
                val text = choiceObject.getJSONObject("message")
                return text.getString("content")
            }
        }
        return null
    }
}