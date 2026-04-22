package com.example.petcare.data.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcPayloadCodecTest {

    @Test
    fun `buildWriteRecords creates a single text record with Flutter-compatible payload`() {
        val jsonPayload = """{"petId":"pet-123","petName":"Luna"}"""

        val records = NfcPayloadCodec.buildWriteRecords(jsonPayload)

        assertEquals(1, records.size)
        assertTrue(records.single().isTextRecord)
        assertEquals(2, records.single().payload.first().toInt())
        assertEquals("en", records.single().payload.copyOfRange(1, 3).decodeToString())
        assertEquals(
            jsonPayload,
            records.single().payload.copyOfRange(3, records.single().payload.size).decodeToString()
        )
    }

    @Test
    fun `extractPetId resolves petId from a text payload`() {
        val record = NfcPayloadCodec.buildWriteRecords("""{"petId":"pet-456"}""").single()

        val petId = NfcPayloadCodec.extractPetId(sequenceOf(record))

        assertEquals("pet-456", petId)
    }

    @Test
    fun `extractPetId returns null for malformed json payload`() {
        val record = NfcPayloadCodec.buildWriteRecords("""not-json""").single()

        val petId = NfcPayloadCodec.extractPetId(sequenceOf(record))

        assertNull(petId)
    }

    @Test
    fun `extractPetId returns null when json payload does not include pet id`() {
        val record = NfcPayloadCodec.buildWriteRecords("""{"petName":"Luna"}""").single()

        val petId = NfcPayloadCodec.extractPetId(sequenceOf(record))

        assertNull(petId)
    }

    @Test
    fun `extractPetId rejects old mixed-format tags by using first readable payload only`() {
        val legacyUriRecord = NfcPayloadCodec.ContractRecord(
            payload = "petcare://pet/pet-legacy".toByteArray(Charsets.UTF_8),
            isTextRecord = false
        )
        val legacyTextRecord = NfcPayloadCodec.buildWriteRecords(
            """{"petId":"pet-legacy"}"""
        ).single()

        val petId = NfcPayloadCodec.extractPetId(sequenceOf(legacyUriRecord, legacyTextRecord))

        assertNull(petId)
    }
}
