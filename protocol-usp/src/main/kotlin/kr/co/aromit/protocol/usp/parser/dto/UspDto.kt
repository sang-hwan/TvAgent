package kr.co.aromit.protocol.usp.parser.dto

import kr.co.aromit.tvagent.network.mqtt.topics.usp.UspMsgProto

/**
 * Data Transfer Objects for USP messages.
 */
data class UspEnvelope(
    val msgId: String,
    val msgType: UspMsgProto.Header.MsgType,
    val body: UspBody
)

/**
 * Sealed class representing the body of a USP message.
 */
sealed class UspBody {
    data class Request(val request: UspMsgProto.Request) : UspBody()
    data class Response(val response: UspMsgProto.Response) : UspBody()
    data class Error(val error: UspMsgProto.Error) : UspBody()
}

/**
 * Converter between protobuf representations and DTOs.
 */
object UspProtoConverter {
    /**
     * Converts a protobuf Msg to a DTO envelope.
     */
    fun fromProto(proto: UspMsgProto.Msg): UspEnvelope {
        val header = proto.header
        val bodyProto = proto.body
        val body = when {
            bodyProto.hasRequest()  -> UspBody.Request(bodyProto.request)
            bodyProto.hasResponse() -> UspBody.Response(bodyProto.response)
            bodyProto.hasError()    -> UspBody.Error(bodyProto.error)
            else -> throw IllegalArgumentException("Unknown USP body type: $bodyProto")
        }
        return UspEnvelope(
            msgId = header.msgId,
            msgType = header.msgType,
            body = body
        )
    }

    /**
     * Converts a DTO envelope back to its protobuf Msg representation.
     */
    fun toProto(envelope: UspEnvelope): UspMsgProto.Msg {
        val headerBuilder = UspMsgProto.Header.newBuilder()
            .setMsgId(envelope.msgId)
            .setMsgType(envelope.msgType)

        val bodyBuilder = UspMsgProto.Body.newBuilder().apply {
            when (val b = envelope.body) {
                is UspBody.Request -> setRequest(b.request)
                is UspBody.Response -> setResponse(b.response)
                is UspBody.Error -> setError(b.error)
            }
        }

        return UspMsgProto.Msg.newBuilder()
            .setHeader(headerBuilder)
            .setBody(bodyBuilder)
            .build()
    }
}
