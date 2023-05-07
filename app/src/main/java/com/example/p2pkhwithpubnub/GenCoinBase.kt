package com.example.p2pkhwithpubnub

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash.hash

class GenCoinBase {
    @Serializable
    data class CoinBase(val type : String, val txid : String, val address: String, val gruut: Int, val script : String)

    fun genCoinBase(address: String, gruut: Int): String {
        val txid = hash((Random.nextBits(256)).toString().toByteArray()).toString()
        val hashedPubKey = Base58.decode(address)
        val coinBaseMessage = CoinBase("coin_base", txid, address, gruut, script = "76a9${hashedPubKey}88ac")
        return Json.encodeToString(coinBaseMessage)
    }
}