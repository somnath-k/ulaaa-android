package com.dotkios.ulaaa.data.repository

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.dotkios.ulaaa.data.model.DeviceContact
import com.dotkios.ulaaa.util.normalizePhone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Reads device contacts (name + phone), deduped by normalized number. Requires READ_CONTACTS. */
    suspend fun readContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val byKey = LinkedHashMap<String, DeviceContact>()
        val projection = arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER)
        context.contentResolver.query(
            Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${Phone.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(Phone.NUMBER)
            if (nameIdx < 0 || numberIdx < 0) return@use
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx)?.trim().orEmpty()
                val raw = cursor.getString(numberIdx)?.trim().orEmpty()
                if (name.isBlank() || raw.isBlank()) continue
                val key = normalizePhone(raw)
                if (key.length >= 7 && !byKey.containsKey(key)) {
                    byKey[key] = DeviceContact(name = name, phoneRaw = raw, phoneKey = key)
                }
            }
        }
        byKey.values.toList()
    }
}
