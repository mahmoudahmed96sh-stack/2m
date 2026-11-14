package com.example.data.firebase

import java.security.MessageDigest

object PinHasher {
  private const val SALT = "2m_store_luxury_pin_v1"

  fun hashPin(pin: String): String {
    val clean = pin.trim()
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("$SALT:$clean".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun verifyPin(pin: String, expectedHash: String): Boolean {
    if (pin.length != 4 || !pin.all { it.isDigit() }) return false
    return hashPin(pin) == expectedHash
  }
}
