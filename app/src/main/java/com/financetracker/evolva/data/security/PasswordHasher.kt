package com.financetracker.evolva.data.security

import java.security.MessageDigest

object PasswordHasher {
    fun hash(password: String): String {
        if (password.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun matches(password: String, storedHash: String): Boolean =
        hash(password) == storedHash
}
