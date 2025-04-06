package com.example.events.data.api

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.events.data.model.*
import com.example.events.data.model.HabitList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class HabitsService(val token: String) {
    private val client = OkHttpClient()
    private val baseUrl = "http://192.168.1.93:3000/api/v1" // Ajusta a tu URL base

    suspend fun fetchHabits(): List<Habit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/habits/") // Endpoint para obtener hábitos
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let { parseHabits(it) } ?: emptyList()
                    } else {
                        Log.e("HabitsService", "Error fetching habits: ${response.code}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception fetching habits", e)
                emptyList()
            }
        }
    }

    private fun parseHabits(jsonString: String): List<Habit> {
        val jsonArray = JSONArray(jsonString)
        return (0 until jsonArray.length()).map { index ->
            val habitJson = jsonArray.getJSONObject(index)
            parseHabit(habitJson)
        }
    }

    private fun parseHabit(json: JSONObject): Habit {
        return Habit(
            id = json.getInt("id"),
            name = json.getString("name"),
            description = json.optString("description", null),
            frequency = json.optString("frequency", ""),
            daysOfWeek = json.optJSONArray("days_of_week")?.let { parseDaysOfWeek(it) },
            timeOfDay = json.optString("time_of_day", null),
            location = json.optJSONObject("location")?.let { parseLocation(it) },
            audioNote = json.optJSONObject("audio_note")?.let { parseAudioNote(it) },
            images = json.optJSONArray("images")?.let { parseImageList(it) },
            createdAt = json.getString("created_at"),
            updatedAt = json.getString("updated_at"),
            habitList = json.optJSONObject("habit_list")?.let { parseHabitList(it) } ?: HabitList(0, "Default List", null, emptyList()) // Proporciona un valor predeterminado si no hay habitList
        )
    }

    private fun parseHabitList(json: JSONObject): HabitList {
        return HabitList(
            id = json.getInt("id"),
            title = json.getString("title"),
            description = json.optString("description", null),
            items = json.optJSONArray("items")?.let { parseHabitItems(it) } ?: emptyList()
        )
    }

    private fun parseHabitItems(jsonArray: JSONArray): List<HabitItem> {
        return (0 until jsonArray.length()).map { index ->
            val itemJson = jsonArray.getJSONObject(index)
            HabitItem(
                id = itemJson.getInt("id"),
                name = itemJson.getString("name"),
                responsible = parseUser(itemJson.getJSONObject("responsible")),
                status = itemJson.getString("status")
            )
        }
    }


    private fun parseDaysOfWeek(jsonArray: JSONArray): List<Int> {
        return (0 until jsonArray.length()).map { index ->
            jsonArray.getInt(index)
        }
    }

    private fun parseLocation(json: JSONObject): Location? {
        if (json.length() == 0) return null
        return Location(
            id = json.getInt("id"),
            latitude = json.optDouble("latitude", 0.0), // Proporciona un valor predeterminado
            longitude = json.optDouble("longitude", 0.0), // Proporciona un valor predeterminado
            name = json.optString("name", null)
        )
    }

    private fun parseAudioNote(json: JSONObject): AudioNote? {
        if (json.length() == 0) return null
        return AudioNote(
            id = json.getInt("id"),
            audioFile = json.getString("audio_file"),
            title = json.optString("title", null),
            recordedAt = json.getString("recorded_at")
        )
    }

    private fun parseImageList(jsonArray: JSONArray): List<HabitImage> {
        return (0 until jsonArray.length()).map { index ->
            val imageJson = jsonArray.getJSONObject(index)
            HabitImage(
                id = imageJson.getInt("id"),
                image = imageJson.getString("image"),
                caption = imageJson.optString("caption", null),
                uploadedAt = imageJson.getString("uploaded_at") // Corregido aquí
            )
        }
    }

    suspend fun createItem(eventId: Int, itemName: String, responsibleId: Int): HabitItem? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("name", itemName)
                    put("responsible_id", responsibleId)
                    put("event_id", eventId)
                }.toString()

                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$baseUrl/items/") // Endpoint para crear items
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("Item creado correctamente")
                        val responseBody = response.body?.string()
                        responseBody?.let { parseHabitItem(JSONObject(it)) }
                    } else {
                        Log.e("HabitsService", "Error creating item: ${response.code}")
                        println("Cuerpo de la respuesta: ${response.body?.string()}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception creating item", e)
                null
            }
        }
    }

    private fun parseHabitItem(json: JSONObject): HabitItem {
        return HabitItem(
            id = json.getInt("id"),
            name = json.getString("name"),
            responsible = parseUser(json.getJSONObject("responsible")),
            status = json.getString("status"),
            addedAt = json.optString("added_at", "") // Añadido addedAt con valor predeterminado
        )
    }

    private fun parseUser(json: JSONObject): User {
        return User(
            id = json.getInt("id"),
            username = json.getString("username"),
            email = json.getString("email"),
            firstName = json.getString("first_name"),
            lastName = json.getString("last_name")
        )
    }

    suspend fun createHabit(habit: Habit): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("name", habit.name)
                    put("description", habit.description)
                    put("frequency", habit.frequency)
                    habit.daysOfWeek?.let { put("days_of_week", JSONArray(it)) }
                    habit.timeOfDay?.let { put("time_of_day", it) }
                    habit.location?.let { put("location", JSONObject().apply {
                        put("latitude", it.latitude)
                        put("longitude", it.longitude)
                        it.name?.let { name -> put("name", name) }
                    })}
                    habit.audioNote?.let { put("audio_note", JSONObject().apply {
                        put("audio_file", it.audioFile)
                        it.title?.let { title -> put("title", title) }
                        put("recorded_at", it.recordedAt)
                    })}
                    habit.images?.let { images -> put("images", JSONArray(images.map { image ->
                        JSONObject().apply {
                            put("image", image.image)
                            image.caption?.let { caption -> put("caption", caption) }
                            put("uploaded_at", image.uploadedAt)
                        }
                    }))}
                }.toString()

                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$baseUrl/habits/") // Endpoint para crear hábitos
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("Hábito creado correctamente")
                        val responseBody = response.body?.string()
                        val habitId = parseHabitId(responseBody)
                        habitId
                    } else {
                        Log.e("HabitsService", "Error creating habit: ${response.code}")
                        println("Cuerpo de la respuesta: ${response.body?.string()}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception creating habit", e)
                null
            }
        }
    }

    private fun parseHabitId(jsonString: String?): Int? {
        return try {
            val json = JSONObject(jsonString)
            json.getInt("id")
        } catch (e: Exception) {
            Log.e("HabitsService", "Error parsing habit ID from JSON", e)
            null
        }
    }

    suspend fun updateHabit(habit: Habit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Similar a createHabit, pero con el método PUT y el ID del hábito en la URL
                val jsonBody = JSONObject().apply {
                    put("name", habit.name)
                    put("description", habit.description)
                    put("frequency", habit.frequency)
                    habit.daysOfWeek?.let { put("days_of_week", JSONArray(it)) }
                    habit.timeOfDay?.let { put("time_of_day", it) }
                    habit.location?.let { put("location", JSONObject().apply {
                        put("latitude", it.latitude)
                        put("longitude", it.longitude)
                        it.name?.let { name -> put("name", name) }
                    })}
                    habit.audioNote?.let { put("audio_note", JSONObject().apply {
                        put("audio_file", it.audioFile)
                        it.title?.let { title -> put("title", title) }
                        put("recorded_at", it.recordedAt)
                    })}
                    habit.images?.let { images -> put("images", JSONArray(images.map { image ->
                        JSONObject().apply {
                            put("image", image.image)
                            image.caption?.let { caption -> put("caption", caption) }
                            put("uploaded_at", image.uploadedAt)
                        }
                    }))}
                }.toString()

                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$baseUrl/habits/${habit.id}/") // Endpoint para actualizar hábitos
                    .addHeader("Authorization", "Bearer $token")
                    .put(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("Hábito actualizado correctamente")
                        true
                    } else {
                        Log.e("HabitsService", "Error updating habit: ${response.code}")
                        println("Cuerpo de la respuesta: ${response.body?.string()}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception updating habit", e)
                false
            }
        }
    }

    suspend fun deleteHabit(habitId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/habits/$habitId/") // Endpoint para eliminar hábitos
                    .addHeader("Authorization", "Bearer $token")
                    .delete()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("Hábito eliminado correctamente")
                        true
                    } else {
                        Log.e("HabitsService", "Error deleting habit: ${response.code}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception deleting habit", e)
                false
            }
        }
    }

    suspend fun uploadImage(habitId: Int, imageUri: Uri, context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = uriToFile(imageUri, context)

                if (!file.exists()) {
                    Log.e("HabitsService", "Image file not found: ${file.absolutePath}")
                    return@withContext false
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image",
                        file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/habits/$habitId/add_image/") // Endpoint para subir imagen
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("Imagen subida correctamente")
                        true
                    } else {
                        Log.e("HabitsService", "Error uploading image: ${response.code}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception uploading image", e)
                false
            }
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val contentResolver = context.contentResolver
        val fileName = getFileName(uri, contentResolver)
        val file = File(context.cacheDir, fileName)

        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return file
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.e("HabitsService", "Error converting URI to file", e)
        }

        return file
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }
        return fileName ?: "default_file_name"
    }

    suspend fun uploadAudio(habitId: Int, audioUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(audioUri.path!!)

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "audio_file",
                        file.name,
                        file.asRequestBody("audio/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/habits/$habitId/add_note_audio/") // Endpoint para subir audio
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("Audio subido correctamente")
                        true
                    } else {
                        Log.e("HabitsService", "Error uploading audio: ${response.code}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("HabitsService", "Exception uploading audio", e)
                false
            }
        }
    }
}