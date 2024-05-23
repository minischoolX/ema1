package org.openedx.core.extension

import android.util.Base64

fun Long.encodeToString(): String {
    return Base64.encodeToString(this.toString().toByteArray(), Base64.DEFAULT)
}

fun String.encodeToString(): String {
    return Base64.encodeToString(this.toByteArray(), Base64.DEFAULT)
}

fun String.decodeToLong(): Long? {
    return try {
        Base64.decode(this, Base64.DEFAULT).toString(Charsets.UTF_8).toLong()
    } catch (ex: Exception) {
        null
    }
}

fun String.decodeToString(): String? {
    return try {
        Base64.decode(this, Base64.DEFAULT).toString(Charsets.UTF_8)
    } catch (ex: Exception) {
        null
    }
}
