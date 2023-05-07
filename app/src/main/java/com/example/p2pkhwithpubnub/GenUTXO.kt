package com.example.p2pkhwithpubnub

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bitcoinj.core.Sha256Hash
import kotlin.random.Random

class GenUTXO {
    @Serializable
    data class UTXO(val type : String, val txid : String, val input_number : Int, val inputs : List<Input>,
                    val output_number : Int, val outputs : List<Output>)
    @Serializable
    data class Input(val output_txid : String, val output_index : Int, val input_index : Int)
    @Serializable
    data class Output(val address : String, val gruut : Int, val script : String, val output_index: Int)

    fun genUTXO(inputs: List<Input>, outputs: List<Output>) : String {
        val txid = Sha256Hash.hash((Random.nextBits(256)).toString().toByteArray()).toString()
        val utxoMessage = UTXO("utxo", txid, inputs.size, inputs, outputs.size, outputs)
        return Json.encodeToString(utxoMessage)
    }
}