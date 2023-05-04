package com.example.p2pkhwithpubnub

class GenUTXO {

    // data class for information of input and output
    data class InputData (var output_txid: Int, var output_index : Int, var input_index : Int)

    // pubKeyHash의 데이터 타입은 달라질 수 있음...
    // 코드 실행해보고 점검할 것
    data class OutputData (var address: String, var gruut : Int, var output_index : Int, var pubKeyHash: String)

    fun genUTXO(inputDataList: List<InputData>, outputDataList: List<OutputData>) : String {
        var message = """{"unspent_output" : [
            |{
            |   "input_number" : ${inputDataList.size}
            |   "inputs" : [
        """.trimMargin()

        for (data in inputDataList){
            message += inputInfo(data.output_txid, data.output_index, data.input_index)
            message += """,
                |
            """.trimMargin()
        }
        message.replace(".$".toRegex(), "")

        message += """
            |],
            |"output_number : ${outputDataList.size},
            |"outputs" : [
            |
        """.trimMargin()

        for (data in outputDataList){
            message += outputInfo(data.address, data.gruut, data.output_index, data.pubKeyHash)
            message += """,
                |
            """.trimMargin()
        }
        message.replace(".$".toRegex(), "")

        message += """
            |]
            |}
        """.trimMargin()

        return message
    }

    private fun inputInfo(outputTxid: Int, outputIndex: Int, inputIndex: Int): Any? {
        return """"{
            |output_txid" : $outputTxid,
            |"output_index" : $outputIndex,
            |"input_index" : $inputIndex
            |},
        """.trimMargin()
    }

    private fun outputInfo(address: String, gruut: Int, outputIndex: Int, pubKeyHash : String): String {
        return """{
            |"address" : $address,
            |"gruut" : $gruut,
            |"script_code" : "76a9${pubKeyHash}88ac",
            |"output_index" : $outputIndex
            |}
        """.trimMargin()
    }
}