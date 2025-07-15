package kr.co.aromit.network.mqtt

import kr.co.aromit.core.Config

/**
 * MQTT 토픽 네이밍 규칙
 *
 * MQTT 토픽 구조 기준:
 *  - 상태 보고  : usp/agent/{deviceUuid}/inform
 *  - 명령 수신  : usp/agent/{deviceUuid}/command
 */
object MqttTopics {
    private const val PREFIX = "usp/agent"

    /** Agent → ACS 상태 보고용 Inform 토픽 */
    fun informTopic(deviceUuid: String = Config.deviceUuid): String =
        "$PREFIX/$deviceUuid/inform"

    /** ACS → Agent 명령 수신용 Command 토픽 */
    fun commandTopic(deviceUuid: String = Config.deviceUuid): String =
        "$PREFIX/$deviceUuid/command"
}
