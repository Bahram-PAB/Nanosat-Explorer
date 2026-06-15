package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiSatelliteResult(
    val name: String,
    val unitSize: String, // e.g. "1U", "3U", "6U", "12U", "Other"
    val weightKg: Double,
    val launchCountry: String,
    val launchAgency: String,
    val launchDate: String, // YYYY-MM-DD
    val status: String,    // "Orbiting", "De-orbited", "Decayed", "Launch Failure"
    val description: String,
    val missionObjective: String,
    val imageUrl: String
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Checks if the API key is configured and not the default placeholder.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Queries Gemini to search for satellite information by name.
     * Returns parsed GeminiSatelliteResult or throws an Exception.
     */
    suspend fun querySatelliteInfo(satelliteName: String): GeminiSatelliteResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("API key is not configured in Secrets.")
        }

        val prompt = """
            Search for the satellite named '$satelliteName' based on real orbital databases like nanosats.eu database.
            Provide detailed specifications for it.
            You must reply ONLY with a valid JSON block containing the following keys and values. No markdown wrapping (like ```json), no intro, no explanation, just raw JSON.
            
            JSON Structure:
            {
              "name": "Exact Name",
              "unitSize": "1U" or "2U" or "3U" or "6U" or "12U" or "Other" (Must be exactly one of these),
              "weightKg": 1.2, (A double value, estimate if not sure, e.g. 1.0, 4.5, etc.)
              "launchCountry": "Country Name",
              "launchAgency": "Launching Agency or Operator",
              "launchDate": "YYYY-MM-DD", (Use approximate or actual launch date)
              "status": "Orbiting" or "De-orbited" or "Decayed" or "Launch Failure" (Choose the closest status, defaults to 'Orbiting'),
              "description": "A comprehensive 2-3 sentence paragraph about the satellite's history, developers, and hardware.",
              "missionObjective": "1-sentence summary of the main goal of the mission.",
              "imageUrl": "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80" (Choose this default Unsplash space URL or a similar high-quality space/launch image URL)
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a precise space and CubeSat satellite scientific catalog parser. You export database-friendly raw JSON only."))
            )
        )

        val response = service.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("The model did not return any response.")

        // Clean response text from any markdown code blocks
        val cleanJson = responseText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val adapter = moshi.adapter(GeminiSatelliteResult::class.java)
        return adapter.fromJson(cleanJson) ?: throw Exception("Failed to parse JSON response: $responseText")
    }
}
