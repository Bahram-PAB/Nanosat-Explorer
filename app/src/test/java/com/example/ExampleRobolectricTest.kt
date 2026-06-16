package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nanosat Explorer", appName)
  }

  fun sanitizeJson(jsonString: String): String {
    val lines = jsonString.split("\n")
    val cleanedLines = lines.map { line ->
      val trimmed = line.trim()
      val colonIdx = line.indexOf("\":\"")
      if (colonIdx != -1 && trimmed.startsWith("\"") && (trimmed.endsWith("\",") || trimmed.endsWith("\""))) {
        val keyPart = line.substring(0, colonIdx + 1)
        val valuePart = line.substring(colonIdx + 2)
        val trimmedVal = valuePart.trim()
        val hasComma = trimmedVal.endsWith(",")
        
        val startQuoteIdx = valuePart.indexOf("\"")
        val endQuoteIdx = if (hasComma) valuePart.lastIndexOf("\",") else valuePart.lastIndexOf("\"")
        
        if (startQuoteIdx != -1 && endQuoteIdx > startQuoteIdx) {
          val valueLeading = valuePart.substring(0, startQuoteIdx + 1)
          val valueTrailing = valuePart.substring(endQuoteIdx)
          val valueBody = valuePart.substring(startQuoteIdx + 1, endQuoteIdx)
          
          val escapedBody = StringBuilder()
          for (i in valueBody.indices) {
            val char = valueBody[i]
            if (char == '"') {
              if (i == 0 || valueBody[i - 1] != '\\') {
                escapedBody.append("\\\"")
              } else {
                escapedBody.append(char)
              }
            } else {
              escapedBody.append(char)
            }
          }
          keyPart + ":" + valueLeading + escapedBody.toString() + valueTrailing
        } else {
          line
        }
      } else {
        line
      }
    }
    return cleanedLines.joinToString("\n")
  }

  @Test
  fun `test parse satellite database json`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val satellites = mutableListOf<String>()
    var exception: Exception? = null
    var cleanedJson = ""
    try {
      context.assets.open("satellite-database.json").use { inputStream ->
        val jsonString = inputStream.reader().use { it.readText() }
        val sanitized = sanitizeJson(jsonString)
        cleanedJson = sanitized.replace(Regex(",\\s*([\\}\\]])"), "$1")
        val jsonArray = JSONArray(cleanedJson)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          satellites.add(obj.optString("Mission name", ""))
        }
      }
    } catch (e: Exception) {
      var shortErr = e.message ?: ""
      if (shortErr.length > 200) {
        shortErr = shortErr.take(150) + "..."
      }
      if (e is org.json.JSONException) {
        val msg = e.message ?: ""
        val pattern = Regex("character (\\d+)")
        val match = pattern.find(msg)
        if (match != null) {
          val idx = match.groupValues[1].toInt()
          val start = (idx - 150).coerceAtLeast(0)
          val end = (idx + 150).coerceAtMost(cleanedJson.length)
          val snippet = cleanedJson.substring(start, end)
          shortErr += " | SNIPPET AT $idx: >>>$snippet<<<"
        }
      }
      exception = Exception(shortErr)
    }

    if (exception != null) {
      throw AssertionError("ERR: " + exception.message)
    }
    assertTrue("Satellites should not be empty", satellites.isNotEmpty())
    println("Parsed size: ${satellites.size}")
  }
}
