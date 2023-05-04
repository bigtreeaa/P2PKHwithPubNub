package com.example.p2pkhwithpubnub

import kotlin.random.Random
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash.hash

class GenCoinBase {
    fun genCoinBase(address : String, gruut : Int) : String {
        val txid = hash((Random.nextBits(256)).toString().toByteArray())
        val hashedPubKey = Base58.decode(address)
        val message : String = """{"coin_base" : [
            |{
            |   "txid" : $txid,
            |   "address" : $address,
            |   "gruut" : $gruut,
            |   "script_code" : "76a9${hashedPubKey}88ac"
            |}]}
        """.trimMargin()
        return message
    }

}