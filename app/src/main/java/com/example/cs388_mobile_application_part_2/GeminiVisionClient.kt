package com.example.cs388_mobile_application_part_2

import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

object GeminiVisionClient {
	private const val MODEL_NAME = "gemini-3.1-flash-lite-preview"
	private const val PROMPT =
		"Identify the board game shown in this image. Return only the board game title. If you think it is not a board game image, return UNKNOWN."

	suspend fun detectBoardGameName(imageBytes: ByteArray): String? {
		if (imageBytes.isEmpty()) return null
		val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null

		return runCatching {
			val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(MODEL_NAME)
			val response = model.generateContent(
				content {
					text(PROMPT)
					image(imageBitmap)
				}
			)

			val rawText = response.text.orEmpty().trim()
			Log.d("GeminiVisionClient", "Raw response: '$rawText'")
			if (rawText.isBlank()) return null
			val firstLine = rawText.lineSequence().firstOrNull()?.trim().orEmpty()
			if (firstLine.equals("UNKNOWN", ignoreCase = true)) return null
			firstLine.trim('"', '\'', '`', '.', ',', '!', '?', ' ')
		}.getOrNull()
	}
}

