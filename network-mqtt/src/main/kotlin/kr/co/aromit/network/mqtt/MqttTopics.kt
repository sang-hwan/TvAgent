package kr.co.aromit.network.mqtt

import timber.log.Timber

/**
 * MQTT 토픽을 관리하는 유틸리티 객체
 */
object MqttTopics {
    private const val TAG = "MqttTopics"

    /** USP Inform 메시지 발행 토픽 */
    fun inform(uuid: String): String {
        val topic = "usp/agent/$uuid/inform"
        Timber.tag(TAG).d("Inform topic generated: %s", topic)
        return topic
    }

    /** USP Command 수신 토픽 */
    fun command(uuid: String): String {
        val topic = "usp/agent/$uuid/command"
        Timber.tag(TAG).d("Command topic generated: %s", topic)
        return topic
    }

    /** USP 권장 QoS 레벨 (Exactly Once) */
    const val QOS = 2
}
