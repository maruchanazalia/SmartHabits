package com.example.events.data.api

import android.util.Log
import com.example.events.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

class UserService(private val token: String) {
    private val client = OkHttpClient()
    private val baseUrl = "http://192.168.1.93:3000" // Reemplaza con tu URL base

    suspend fun fetchAllUsers(): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/users/") // Ajusta el endpoint de usuarios
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let { parseUsers(it) } ?: emptyList()
                    } else {
                        Log.e("UserService", "Error fetching users: ${response.code} - ${response.message}")
                        emptyList()
                    }
                }
            } catch (e: IOException) {
                Log.e("UserService", "Network error fetching users", e)
                emptyList()
            } catch (e: Exception) {
                Log.e("UserService", "Unexpected error fetching users", e)
                emptyList()
            }
        }
    }

    private fun parseUsers(jsonString: String): List<User> {
        val jsonArray = JSONArray(jsonString)
        return (0 until jsonArray.length()).map { index ->
            val userJson = jsonArray.getJSONObject(index)
            User(
                id = userJson.getInt("id"),
                username = userJson.getString("username"),
                email = userJson.getString("email"),
                firstName = userJson.getString("firstName"), // Ajusta los nombres de los campos
                lastName = userJson.getString("lastName") // Ajusta los nombres de los campos
            )
        }
    }
}
