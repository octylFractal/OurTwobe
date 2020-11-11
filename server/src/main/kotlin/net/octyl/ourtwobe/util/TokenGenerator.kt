package net.octyl.ourtwobe.util

import java.math.BigInteger
import java.security.SecureRandom

class TokenGenerator {
    private val rng = SecureRandom()

    fun newToken(): String {
        val array = ByteArray(32)
        rng.nextBytes(array)
        return BigInteger(array).abs().toString(36)
    }
}
