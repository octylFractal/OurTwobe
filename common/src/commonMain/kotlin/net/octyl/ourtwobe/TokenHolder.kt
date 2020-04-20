package net.octyl.ourtwobe

@Export
interface TokenHolder {
    val token: String
}

data class TokenHolderImpl(override val token: String) : TokenHolder
