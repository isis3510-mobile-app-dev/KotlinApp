package com.example.petcare.data.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface NfcReadInspection {
    data class PetCareTag(val petId: String) : NfcReadInspection
    object NonPetCareTag : NfcReadInspection
}

sealed interface NfcWriteInspection {
    object ReadyToWrite : NfcWriteInspection
    object ReadOnlyTag : NfcWriteInspection
    object IncompatibleTag : NfcWriteInspection
    data class CapacityTooSmall(val requiredBytes: Int, val availableBytes: Int) : NfcWriteInspection
}

class NfcManager(private val activity: Activity) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    val isNfcSupported: Boolean get() = adapter != null
    val isNfcEnabled: Boolean get() = adapter?.isEnabled == true

    /**
     * Call from Activity.onResume().
     * Ensures NFC intents are delivered to THIS activity while it is visible, instead of launching a separate app chooser.
     */
    fun enableForegroundDispatch() {
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        adapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    /**
     * Call from Activity.onPause().
     */
    fun disableForegroundDispatch() {
        adapter?.disableForegroundDispatch(activity)
    }

    suspend fun writeTag(
        tag: Tag,
        petId: String,
        jsonPayload: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ndefMessage = buildNdefMessage(petId, jsonPayload)

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                // Tag is already NDEF-formatted
                ndef.connect()
                when {
                    !ndef.isWritable ->
                        return@withContext Result.failure(Exception("Tag is read-only"))
                    ndef.maxSize < ndefMessage.toByteArray().size ->
                        return@withContext Result.failure(Exception("Tag capacity too small"))
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
            } else {
                // Blank tag — format it first
                val formatable = NdefFormatable.get(tag)
                    ?: return@withContext Result.failure(
                        Exception("Tag is not NDEF-compatible")
                    )
                formatable.connect()
                formatable.format(ndefMessage)
                formatable.close()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun inspectTagForRead(tag: Tag): NfcReadInspection {
        val petId = readPetIdFromTag(tag)
        return if (petId.isNullOrBlank()) {
            NfcReadInspection.NonPetCareTag
        } else {
            NfcReadInspection.PetCareTag(petId)
        }
    }

    fun inspectTagForWrite(tag: Tag, petId: String, jsonPayload: String): NfcWriteInspection {
        val requiredBytes = buildNdefMessage(petId, jsonPayload).toByteArray().size
        val ndef = Ndef.get(tag)

        if (ndef != null) {
            return try {
                ndef.connect()
                val result = when {
                    !ndef.isWritable -> NfcWriteInspection.ReadOnlyTag
                    ndef.maxSize < requiredBytes -> NfcWriteInspection.CapacityTooSmall(
                        requiredBytes = requiredBytes,
                        availableBytes = ndef.maxSize
                    )
                    else -> NfcWriteInspection.ReadyToWrite
                }
                ndef.close()
                result
            } catch (_: Exception) {
                NfcWriteInspection.IncompatibleTag
            }
        }

        return if (NdefFormatable.get(tag) != null) {
            NfcWriteInspection.ReadyToWrite
        } else {
            NfcWriteInspection.IncompatibleTag
        }
    }

    fun writeErrorMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("read-only", ignoreCase = true) ->
                "This NFC tag is read-only."
            message.contains("capacity", ignoreCase = true) ||
                message.contains("too small", ignoreCase = true) ->
                "This NFC tag does not have enough space for PetCare data."
            message.contains("ndef-compatible", ignoreCase = true) ->
                "This NFC tag is not compatible with PetCare."
            else ->
                "We couldn't write to this NFC tag. Please try again with a compatible tag."
        }
    }


    fun readPetIdFromTag(tag: Tag): String? {
        return try {
            val ndef = Ndef.get(tag) ?: return null
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()

            message?.records
                ?.asSequence()
                ?.mapNotNull { record ->
                    // NdefRecord.toUri() handles TNF_WELL_KNOWN+RTD_URI
                    // and TNF_ABSOLUTE_URI transparently
                    record.toUri()?.toString()
                }
                ?.firstOrNull { it.startsWith("petcare://pet/") }
                ?.removePrefix("petcare://pet/")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convenience: extracts the Tag from an Intent delivered by the system,
     * then calls [readPetIdFromTag].
     */
    fun readPetIdFromIntent(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return null
        return readPetIdFromTag(tag)
    }

    /**
     * Returns the raw Tag from an NFC Intent, or null.
     * Use this when you need the tag for writing.
     */
    fun getTagFromIntent(intent: Intent): Tag? =
        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildNdefMessage(petId: String, jsonPayload: String): NdefMessage {
        // Record 1 — URI (deep-link)
        val uriRecord = NdefRecord.createUri("petcare://pet/$petId")

        // Record 2 — Text (UTF-8, language "en")
        val langBytes  = "en".toByteArray(Charsets.US_ASCII)
        val textBytes  = jsonPayload.toByteArray(Charsets.UTF_8)
        val textPayload = ByteArray(1 + langBytes.size + textBytes.size).also { buf ->
            buf[0] = langBytes.size.toByte()                              // status byte
            System.arraycopy(langBytes, 0, buf, 1, langBytes.size)
            System.arraycopy(textBytes, 0, buf, 1 + langBytes.size, textBytes.size)
        }
        val textRecord = NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            textPayload
        )

        return NdefMessage(arrayOf(uriRecord, textRecord))
    }
}
