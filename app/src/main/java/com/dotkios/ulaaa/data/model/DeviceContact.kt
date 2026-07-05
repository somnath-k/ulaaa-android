package com.dotkios.ulaaa.data.model

/** A contact read from the device address book. */
data class DeviceContact(
    val name: String,
    val phoneRaw: String,
    val phoneKey: String,
)
