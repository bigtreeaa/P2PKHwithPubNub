package com.example.p2pkhwithpubnub

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Button
import java.security.*
import java.security.cert.Certificate
import java.security.spec.ECGenParameterSpec
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash.hash
import org.bitcoinj.core.Utils.sha256hash160

import com.example.p2pkhwithpubnub.BuildConfig
import com.fasterxml.jackson.databind.ser.Serializers
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.UserId
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.models.consumer.PNBoundedPage
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNFetchMessageItem
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import kotlinx.serialization.Serializable


class MainActivity : AppCompatActivity() {

    // for user configuration
    private lateinit var keyStore : KeyStore
    private lateinit var aliceAddress : String
    private lateinit var bobAddress : String
    private val aliasAlice : String = "aliceKey"
    private val aliasBob : String = "bobKey"
    private  var aliceUtxo : MutableList<String> = mutableListOf()
    private  var bobUtxo : MutableList<String> = mutableListOf()


    // for PubNub
    private var channelId = "PubNubDemoChannel"
    private lateinit var receiveMessage : JsonElement
    private lateinit var sendMessage : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // button to generate key store and keypair
        val btnGenKey = findViewById<Button>(R.id.btnGenKey)
        btnGenKey.setOnClickListener{
            // Generate key store
            keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }

            // Set Key spec for Alice
            val paramSpec = KeyGenParameterSpec.Builder(
                aliasAlice,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).run {
                setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                setDigests(
                    KeyProperties.DIGEST_SHA512,
                    KeyProperties.DIGEST_SHA256
                )
                setUserAuthenticationRequired(false)
                build()
            }


            val kpg : KeyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )

            kpg.initialize(paramSpec)

            // Generate Key Pair for Alice
            val publicKeyPair = kpg.genKeyPair()

            // Set Key spec for Bob
            val paramSpec2 = KeyGenParameterSpec.Builder(
                aliasBob,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).run {
                setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                setDigests(
                    KeyProperties.DIGEST_SHA512,
                    KeyProperties.DIGEST_SHA256
                )
                setUserAuthenticationRequired(false)
                build()
            }

            kpg.initialize(paramSpec2)

            // Generate Key Pair for Bob
            val publicKeyPair2 = kpg.genKeyPair()
        }

        // #########################################################################################

        // button to generate address for user
        // generate Alice & Bob address
        val btnGenAddress = findViewById<Button>(R.id.btnGenAddress)
        btnGenAddress.setOnClickListener {
            // Generate Address
            aliceAddress = genAddress(aliasAlice)
            bobAddress = genAddress(aliasBob)
        }

        // #########################################################################################

        // Setting PubNub Service
        // initialize PubNub
        val pubNub = PubNub(
            PNConfiguration(userId = UserId(value = "FirstUser")).apply {
                // BuildConfig is created after compiling
                publishKey = BuildConfig.PUBLISH_KEY
                subscribeKey = BuildConfig.SUBSCRIBE_KEY
                // Logcat Verbosity
                logVerbosity = PNLogVerbosity.BODY
            }
        )

        // Subscribe Channel
        // Basic usage with no options
        pubNub.subscribe(
            channels = listOf(channelId)
        )

        // history setting for retrieving past messages
        pubNub.history(
            channel = channelId,
            reverse = true,
            includeTimetoken = true,
            includeMeta = true,
            count = 100
        ).async { result, status ->  }

        val btnFetch = findViewById<Button>(R.id.btnRetrieveMessage)
        btnFetch.setOnClickListener {
            pubNub.fetchMessages(
                channels = listOf(channelId),
                page = PNBoundedPage(limit = 100),
                includeMessageActions = true
            ).async { result, status ->
                if (!status.error) {
                    result!!.channels.forEach { (channel, messages) ->
                        Log.v("fetch_message","Channel: $channel")
                        messages.forEach { messageItem: PNFetchMessageItem ->
                            Log.v("fetch_message", messageItem.message.toString()) // actual message payload
                            Log.v("fetch_message", messageItem.timetoken.toString()) // included by default
                            messageItem.actions?.forEach { (actionType, map) ->
                                Log.v("fetch_message", "Action type: $actionType")
                                map.forEach { (actionValue, publishers) ->
                                    Log.v("fetch_message","Action value: $actionValue")
                                    publishers.forEach { publisher ->
                                        Log.v("fetch_message", "UUID: ${publisher.uuid}")
                                        Log.v("fetch_message", "Timetoken: ${publisher.actionTimetoken}")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // handle error
                    status.exception?.printStackTrace()
                }
            }
        }

        // Add Listener of a channel to pubNub
        pubNub.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                Log.v("Status", "${pnStatus.category}")
                // PNConnectedCategory, PNReconnectedCategory, PNDisconnectedCategory
                Log.v("Status", "${pnStatus.operation}")
                // PNSubscribeOperation, PNHeartbeatOperation
                Log.v("Status", "${pnStatus.error}")
                // true or false
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
                Log.v("Presence", "Presence event: ${pnPresenceEventResult.event}")
                Log.v("Presence", "Presence channel: ${pnPresenceEventResult.channel}")
                Log.v("Presence", "Presence uuid: ${pnPresenceEventResult.uuid}")
                Log.v("Presence", "Presence timeToken: ${pnPresenceEventResult.timetoken}")
                Log.v("Presence", "Presence occupancy: ${pnPresenceEventResult.occupancy}")
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                Log.v("Message", "Message payload: ${pnMessageResult.message}")
                Log.v("Message", "Message channel: ${pnMessageResult.channel}")
                Log.v("Message", "Message publisher: ${pnMessageResult.publisher}")
                Log.v("Message", "Message timeToken: ${pnMessageResult.timetoken}")

                // Deliver a message to predefined variable
                receiveMessage = pnMessageResult.message

                if (aliceAddress in receiveMessage.toString()) {
                    aliceUtxo.add(receiveMessage.asString)
                }
                if (bobAddress in receiveMessage.toString()) {
                    bobUtxo.add(receiveMessage.asString)
                }
            }
        })

        // #########################################################################################

        // button to generate coin base message
        // 실험 1
        val btnGenCoinBase = findViewById<Button>(R.id.btnGenCoinBase)
        btnGenCoinBase.setOnClickListener{
            // 임시적으로 주어지는 gruut는 5로 설정하였다.
            val coinBaseMessage =  GenCoinBase().genCoinBase(aliceAddress, 5)
            publishing(pubNub, coinBaseMessage)
        }

        // #########################################################################################
        // button to generate utxo message
        // 실험 2
        val btnGenUTXO = findViewById<Button>(R.id.btnGenUTXO)
        btnGenUTXO.setOnClickListener {
            // 임시적으로 utxo를 생성할 데이터는 dummy로 만들었다.
            // 추후 앱 디자인 구현 시, 해당 값들을 받아오는 구조를 만들어야 한다.
            val inputData : MutableList<GenUTXO.Input> = mutableListOf()
            val outputData : MutableList<GenUTXO.Output> = mutableListOf()

            val jsonObject = JsonParser.parseString(aliceUtxo[0]).asJsonObject
            if (jsonObject.get("type").asString == "coin_base") {
                inputData.add(GenUTXO.Input(jsonObject.get("txid").asString, 0,0))
                outputData.add(GenUTXO.Output(bobAddress, jsonObject.get("gruut").asInt,
                    script = "76a9${Base58.decode(bobAddress)}88ac",0))
            }

            val utxoMessage = GenUTXO().genUTXO(inputData, outputData)
            publishing(pubNub, utxoMessage)
        }

        // #########################################################################################
        // button to use utxo transaction
        // 실험 3
        val btnUseUTXO = findViewById<Button>(R.id.btnUseUTXO)
        btnUseUTXO.setOnClickListener {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            val privateKeyEntry = ks.getEntry(aliasBob, null) as KeyStore.PrivateKeyEntry
            val privateKey = privateKeyEntry.privateKey
            val signature : ByteArray = Signature.getInstance("SHA256withECDSA").run {
                initSign(privateKey)
                update(bobUtxo[0].toByteArray())
                sign()
            }
            val publicKey = ks.getCertificate(aliasBob).publicKey
            val useUTXOMessage = UseUTXO().useUTXO(bobUtxo[0], signature, publicKey.toString())
            publishing(pubNub, useUTXOMessage)
//            verify code!
//            val verifier = Signature.getInstance("SHA256withECDSA")
//            verifier.initVerify(publicKey)
//            verifier.update(bobUtxo[0].toByteArray())
//            val isSignatureValid = verifier.verify(signature)
//            Log.v("verify", isSignatureValid.toString())
        }

        // button to send message through PubNub
//        val btnSendMessage = findViewById<Button>(R.id.btnSendMessage)
//        btnSendMessage.setOnClickListener {
//            val ks = KeyStore.getInstance("AndroidKeyStore").apply {
//                load(null)
//            }
//            val keyEntry = ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry
//            val cert : Certificate = keyEntry.certificate
//            sendMessage = genTranx(input_data, output_data, cert)
//            publishing(pubNub, sendMessage)
//        }

    } // end of onCreate function

    // #############################################################################################
    // functions for PubNub & P2PKH

    // function for generate address
    fun genAddress(alias : String) : String {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val keyEntry = ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        val cert : Certificate = keyEntry.certificate
//        val publicKey : ByteArray = cert.publicKey.encoded
        val address = Base58.encode(sha256hash160(hash(cert.encoded)))
        Log.v("Address", address)
        return address
    }

    // Publish Message to a channel
    // Basic usage of publishing a message to a channel
    fun publishing(pubNub: PubNub, message : String) {
        pubNub.publish(
            message = message,
            channel = channelId,
            shouldStore = true,
            ttl = 24
        ).async { result, status ->
            if (!status.error) {
                Log.v("Publishing", "Publish timeToken ${result!!.timetoken}")
            }
            Log.v("Publishing", "Status code ${status.statusCode}")
        }
    }

//    // functions for Script
//
//    // function for P2PKH script
//    fun P2PKH(sign : ByteArray, cert: Certificate, PK_HASH : String, address: String) : Boolean {
//        val hashedPubKey : String = OP_HASH160(cert.publicKey)
//        val eqalVerify : Boolean = OP_EQUALVERIFY(PK_HASH, hashedPubKey)
//        val checkSig : Boolean = OP_CHECKSIG(cert, sign, address)
//
//        return eqalVerify and checkSig
//    }
//
//    // OP_DUP function
//    private fun OP_DUP(pubKey: PublicKey) : PublicKey{
//        return pubKey
//    }
//
//    // OP_HASH_160 function
//    // Use base58 encoding, Convert to String
//    private fun OP_HASH160(pubKey: PublicKey) : String {
//        return Base58.encode(sha256hash160(hash(pubKey.encoded)))
//    }
//    // OP_EQUALVERIFY
//    private fun OP_EQUALVERIFY(PK_HASH1 : String, PK_HASH2 : String) : Boolean {
//        return PK_HASH1.contentEquals(PK_HASH2)
//    }
//
//    // OP_CHECKSIG
//    // transaction은 임의로 ByteArray type으로 해두었다.
//    private fun OP_CHECKSIG(cert: Certificate, sign: ByteArray, address : String) : Boolean {
//        val valid: Boolean = Signature.getInstance("SHA256withECDSA").run {
//            initVerify(cert)
//            update(address.toByteArray())
//            verify(sign)
//        }
//        return valid
//    }
//
//
//
//    // deliver input information
//
//    fun genPubKeyHash(alias : String, keyStore: KeyStore) : String {
//        // get information of certain key pair
//        val cert : Certificate = keyStore.getCertificate(alias)
//        val pubKey = cert.publicKey
//        return OP_HASH160(pubKey)
//    }
//
//    fun genSign(address: String, alias: String) : ByteArray {
//        val ks = KeyStore.getInstance("AndroidKeyStore").apply {
//            load(null)
//        }
//        val entry = ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry
//        val signature  = Signature.getInstance("SHA256withECDSA").run {
//            initSign(entry.privateKey)
//            update(address.toByteArray())
//            sign()
//        }
//
//        return signature
//    }
//
//    fun genBlock(size : Int, version : Int, previousBlockHash : ByteArray, dataList: MutableList<ByteArray>) : String {
//        val merkle_root = merkleRoot(dataList)
//        var block : String = """{
//            | "size" : $size,
//            | "version" : $version,
//            | "previous_block_hash" : $previousBlockHash,
//            | "merkle_root" : $merkle_root,
//            | "transactions" : [
//            |
//        """.trimMargin()
//        for (info in dataList) {
//            block += """
//                "$info",
//                """.trimIndent()
//        }
//        block.replace(".$".toRegex(), "")
//        block += """]}""".trimMargin()
//        return block
//    }
//
//    fun merkle(dataList: MutableList<ByteArray>) : MutableList<ByteArray> {
//        val merkle : MutableList<ByteArray> = hashData(dataList)
//        return merkleRoot(merkle)
//    }
//
//    fun merkleRoot(tranxList : MutableList<ByteArray>) : MutableList<ByteArray> {
//        var hashedTranx = hashData(tranxList)
//        if (hashedTranx.size%2 == 0) { // transaction의 개수가 짝수일 때
//            while (hashedTranx.size > 1) {
//                hashedTranx = merkle(hashedTranx)
//            }
//        } else { // transaction의 개수가 홀수일 때
//            tranxList.add(hashedTranx[hashedTranx.size - 1])
//            while (hashedTranx.size > 1) {
//                hashedTranx = merkle(hashedTranx)
//            }
//        }
//        // tmp return
//        return hashedTranx
//    }
//
//    fun hashData(tranxList: MutableList<ByteArray>) : MutableList<ByteArray> {
//        val hashedTranx : MutableList<ByteArray> = mutableListOf()
//        for (tran in tranxList){
//            hashedTranx.add(hash(tran))
//        }
//        return hashedTranx
//    }
}