package com.example.petcare.data.nfc

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object NfcPayloadCodec {

    private const val LANGUAGE_CODE = "en"
    private val LANGUAGE_BYTES = LANGUAGE_CODE.toByteArray(Charsets.US_ASCII)
    private val PET_ID_KEYS = listOf("petId", "pet_id", "id", "_id")

    data class ContractRecord(
        val payload: ByteArray,
        val isTextRecord: Boolean
    )

    fun buildWriteRecords(jsonPayload: String): List<ContractRecord> {
        return listOf(
            ContractRecord(
                payload = buildTextPayload(jsonPayload),
                isTextRecord = true
            )
        )
    }

    fun extractPetId(records: Sequence<ContractRecord>): String? {
        val rawPayload = extractTextPayload(records) ?: return null
        return extractPetIdFromRawPayload(rawPayload)
    }

    fun extractTextPayload(records: Sequence<ContractRecord>): String? {
        return records
            .mapNotNull(::decodeRecordText)
            .firstOrNull { it.isNotBlank() }
    }

    fun buildTextPayload(text: String): ByteArray {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + LANGUAGE_BYTES.size + textBytes.size)
        payload[0] = LANGUAGE_BYTES.size.toByte()
        System.arraycopy(LANGUAGE_BYTES, 0, payload, 1, LANGUAGE_BYTES.size)
        System.arraycopy(textBytes, 0, payload, 1 + LANGUAGE_BYTES.size, textBytes.size)
        return payload
    }

    fun extractPetIdFromRawPayload(rawPayload: String): String? {
        val trimmedPayload = rawPayload.trim()
        if (trimmedPayload.isEmpty()) return null

        val parsedElement = runCatching {
            JsonParser().parse(trimmedPayload)
        }.getOrNull() ?: return null
        if (!parsedElement.isJsonObject) return null
        val jsonObject = parsedElement.asJsonObject

        return PET_ID_KEYS
            .asSequence()
            .mapNotNull { key -> jsonObject.readString(key) }
            .firstOrNull()
    }

    fun decodeRecordText(record: ContractRecord): String? {
        if (record.payload.isEmpty()) return null

        val decoded = if (record.isTextRecord) {
            decodeTextPayload(record.payload)
        } else {
            decodeUtf8(record.payload)
        }

        return decoded.takeIf { it.isNotBlank() }
    }

    private fun decodeTextPayload(payload: ByteArray): String {
        val statusByte = payload.first().toInt() and 0xFF
        val languageCodeLength = statusByte and 0x3F
        if (payload.size <= languageCodeLength + 1) {
            return decodeUtf8(payload)
        }

        return decodeUtf8(payload.copyOfRange(languageCodeLength + 1, payload.size))
    }

    private fun decodeUtf8(bytes: ByteArray): String {
        return bytes.decodeToString(throwOnInvalidSequence = false)
    }

    private fun JsonObject.readString(key: String): String? {
        if (!has(key)) return null
        val value = get(key)
        if (value == null || value.isJsonNull) return null
        val text = runCatching { value.asString.trim() }.getOrDefault("")
        return text.ifEmpty { null }
    }
}
