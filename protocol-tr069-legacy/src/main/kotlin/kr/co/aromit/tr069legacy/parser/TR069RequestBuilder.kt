package kr.co.aromit.tr069legacy.parser

import kr.co.aromit.tr069legacy.parser.dto.TR069Message.InformRequest
import kr.co.aromit.tr069legacy.parser.dto.TR069Message.Event
import kr.co.aromit.core.Config
import kr.co.aromit.tvagent.util.DateTimeUtil

/**
 * TR-069 Inform 및 Empty 요청용 SOAP/XML 페이로드를 생성하는 빌더 객체입니다.
 */
object TR069RequestBuilder {

    /**
     * Inform 요청용 SOAP/XML 문자열을 생성합니다.
     *
     * @param payload InformRequest DTO
     * @return 생성된 Inform SOAP 메시지 문자열
     */
    fun buildInform(payload: InformRequest): String {
        val messageId = DateTimeUtil.messageIdNow()

        // DeviceId 블록 생성
        val deviceIdXml = """
            <cwmp:DeviceId>
              <Manufacturer xsi:type="xsd:string">${payload.deviceId.manufacturer}</Manufacturer>
              <OUI            xsi:type="xsd:string">${payload.deviceId.oui}</OUI>
              <ProductClass   xsi:type="xsd:string">${payload.deviceId.productClass}</ProductClass>
              <SerialNumber   xsi:type="xsd:string">${payload.deviceId.serialNumber}</SerialNumber>
            </cwmp:DeviceId>
        """.trimIndent()

        // Event 리스트 XML 생성
        val eventsXml = buildEventsXml(payload.events)

        // MaxEnvelopes, CurrentTime, RetryCount XML
        val envelopesXml = "<cwmp:MaxEnvelopes>${payload.maxEnvelopes}</cwmp:MaxEnvelopes>"
        val timeXml      = "<cwmp:CurrentTime>${payload.currentTime}</cwmp:CurrentTime>"
        val retryXml     = "<cwmp:RetryCount>${payload.retryCount}</cwmp:RetryCount>"

        // 파라미터 키 정규화: TR-098 모델 경로를 TR-181 경로로 변환
        val normalizedCustomParams = payload.params.mapKeys { (name, _) ->
            if (name == "InternetGatewayDevice.DeviceInfo.SoftwareVersion")
                "Device.DeviceInfo.SoftwareVersion"
            else
                name
        }

        // ParameterList: UserId, Password + 정규화된 커스텀 파라미터
        val allParams = mutableMapOf<String, String>().apply {
            put("Device.UserId", payload.userId)
            put("Device.Password", payload.password)
            putAll(normalizedCustomParams)
        }
        val paramsListXml = buildParamsListXml(allParams)

        return """
            |<soap-env:Envelope
            |    xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"
            |    xmlns:cwmp="${Config.CWMP_NAMESPACE}"
            |    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            |    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            |  <soap-env:Header>
            |    <cwmp:ID soap-env:mustUnderstand="1">$messageId</cwmp:ID>
            |  </soap-env:Header>
            |  <soap-env:Body>
            |    <cwmp:Inform>
            |      $deviceIdXml
            |      $eventsXml
            |      $envelopesXml
            |      $timeXml
            |      $retryXml
            |      $paramsListXml
            |    </cwmp:Inform>
            |  </soap-env:Body>
            |</soap-env:Envelope>
        """.trimMargin()
    }

    /**
     * 빈 폴링용 SOAP/XML 문자열을 생성합니다.
     *
     * @return 생성된 빈 SOAP 메시지 문자열
     */
    fun buildEmpty(): String {
        val messageId = DateTimeUtil.messageIdNow()
        return """
            |<soap-env:Envelope
            |    xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"
            |    xmlns:cwmp="${Config.CWMP_NAMESPACE}"
            |    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            |    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            |  <soap-env:Header>
            |    <cwmp:ID soap-env:mustUnderstand="1">$messageId</cwmp:ID>
            |  </soap-env:Header>
            |  <soap-env:Body/>
            |</soap-env:Envelope>
        """.trimMargin()
    }

    /**
     * EventStruct 리스트를 CWMP Event 섹션 XML로 변환합니다.
     */
    private fun buildEventsXml(events: List<Event>): String =
        if (events.isEmpty()) ""
        else buildString {
            appendLine("""<cwmp:Event soap-env:arrayType="cwmp:EventStruct[${events.size}]">""")
            events.forEach { ev ->
                appendLine("""
                <EventStruct>
                  <EventCode>${ev.code}</EventCode>
                  <CommandKey>${ev.commandKey}</CommandKey>
                </EventStruct>
            """.trimIndent())
            }
            appendLine("</cwmp:Event>")
        }

    /**
     * 파라미터 맵을 CWMP ParameterList 구조의 XML로 변환합니다.
     */
    private fun buildParamsListXml(params: Map<String, String>): String = buildString {
        appendLine("""<cwmp:ParameterList soap-env:arrayType="cwmp:ParameterValueStruct[${params.size}]">""")
        params.forEach { (name, value) ->
            appendLine("""
                <ParameterValueStruct>
                  <Name>$name</Name>
                  <Value xsi:type="xsd:string">$value</Value>
                </ParameterValueStruct>
            """.trimIndent())
        }
        appendLine("</cwmp:ParameterList>")
    }
}
