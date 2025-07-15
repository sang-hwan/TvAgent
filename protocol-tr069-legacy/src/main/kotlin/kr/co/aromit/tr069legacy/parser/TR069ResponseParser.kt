package kr.co.aromit.tr069legacy.parser

import kr.co.aromit.tr069legacy.parser.dto.TR069Message.InformResponse
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.StringReader

/**
 * ACS로부터 받은 SOAP/XML을 파싱하여
 * InformResponse DTO와 RPC 명령 모델로 변환합니다.
 */
object TR069ResponseParser {

    private const val TAG = "TR069ResponseParser"

    /**
     * XML 문자열을 파싱하기 위한 XmlPullParser를 생성하여 반환합니다.
     */
    private fun createParser(xml: String): XmlPullParser =
        XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
            .newPullParser().apply { setInput(StringReader(xml)) }

    /**
     * InformResponse XML에서 메시지 ID를 추출하여 InformResponse로 반환합니다.
     */
    fun parseInformResponse(xml: String): InformResponse {
        val parser = createParser(xml)
        var messageId: String? = null

        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "ID") {
                    messageId = parser.nextText()
                    break
                }
                parser.next()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "InformResponse 파싱 실패")
        }

        return InformResponse(messageId)
    }

    /**
     * RPC 명령(Get/Set/Reboot)을 포함한 응답 XML을 파싱하여
     * TR069Command 리스트로 반환합니다.
     */
    fun parseRpcResponse(xml: String): List<kr.co.aromit.tr069legacy.model.command.TR069Command> {
        val commands = mutableListOf<kr.co.aromit.tr069legacy.model.command.TR069Command>()
        val parser = createParser(xml)

        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "GetParameterValues" -> commands += parseGetParameterValues(parser)
                        "SetParameterValues" -> commands += parseSetParameterValues(parser)
                        "Reboot"              -> commands += kr.co.aromit.tr069legacy.model.command.TR069Command.Reboot
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "RPC 응답 파싱 실패")
        }

        return commands
    }

    /**
     * GetParameterValues 요소 내부의 <string> 태그를 모두 읽어 리스트로 반환합니다.
     */
    private fun parseGetParameterValues(parser: XmlPullParser): kr.co.aromit.tr069legacy.model.command.TR069Command.GetParameterValues {
        val names = mutableListOf<String>()
        parser.next()
        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "GetParameterValues")) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "string") {
                val raw = parser.nextText()
                // TR-098 경로를 TR-181 경로로 매핑
                val name = if (raw == "InternetGatewayDevice.DeviceInfo.SoftwareVersion")
                    "Device.DeviceInfo.SoftwareVersion" else raw
                names += name
            }
            parser.next()
        }
        return kr.co.aromit.tr069legacy.model.command.TR069Command.GetParameterValues(names)
    }

    /**
     * SetParameterValues 요소 내부의
     * <ParameterValueStruct>들을 읽어 Map으로 반환합니다.
     */
    private fun parseSetParameterValues(parser: XmlPullParser): kr.co.aromit.tr069legacy.model.command.TR069Command.SetParameterValues {
        val params = mutableMapOf<String, String>()
        parser.next()
        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "SetParameterValues")) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "ParameterValueStruct") {
                var nameRaw: String? = null
                var value: String? = null
                parser.next()
                while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "ParameterValueStruct")) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "Name"  -> nameRaw  = parser.nextText()
                            "Value" -> value = parser.nextText()
                        }
                    }
                    parser.next()
                }
                if (nameRaw != null && value != null) {
                    // TR-098 경로를 TR-181 경로로 매핑
                    val name = if (nameRaw == "InternetGatewayDevice.DeviceInfo.SoftwareVersion")
                        "Device.DeviceInfo.SoftwareVersion" else nameRaw
                    params[name] = value
                }
            }
            parser.next()
        }
        return kr.co.aromit.tr069legacy.model.command.TR069Command.SetParameterValues(params)
    }
}
