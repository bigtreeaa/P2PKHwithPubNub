package com.example.p2pkhwithpubnub

import com.google.gson.JsonParser
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.bitcoinj.core.Base58
import java.security.PublicKey

class UseUTXO {
    @Serializable
    data class UsedUTXO(val type : String, val txid : String, val input_number : Int, val inputs : List<GenUTXO.Input>,
                        val output_number : Int, val outputs : List<UsedOutput>)
    @Serializable
    data class UsedOutput(val address : String, val gruut : Int, val signature: ByteArray,
                          val pubKey: String, val script : String, val output_index: Int)


    fun useUTXO(utxo : String, signature: ByteArray, pubKey : String) : String {
        val used = JsonParser.parseString(utxo).asJsonObject
        val usedList = Json.decodeFromString<GenUTXO.UTXO>(utxo)
        val usedOutputs : MutableList<UsedOutput> = mutableListOf()
        usedOutputs.add(UsedOutput(usedList.outputs[0].address, usedList.outputs[0].gruut,
        signature, pubKey, usedList.outputs[0].script, usedList.outputs[0].output_index))
        val usedUTXOMessage = UsedUTXO(type = "used_utxo", used.get("txid").toString(), used.get("input_number").asInt,
            usedList.inputs, used.get("output_number").asInt, usedOutputs)
        return Json.encodeToString(usedUTXOMessage)
    }
}