package kr.co.aromit.network.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kr.co.aromit.core.Config
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.nio.ByteBuffer
import javax.net.ssl.TrustManagerFactory


/**
 * MQTT 연결 관리: TLS 조건부 설정, 자동 재연결, Publish/Subscribe
 *
 * @param trustStoreFactory PEM 인증서 InputStream으로부터 TrustManagerFactory를 생성하는 함수
 * @param cfg 설정 정보
 */
class MqttManager(
    private val trustStoreFactory: () -> TrustManagerFactory,
    private val cfg: Config = Config
) {
    companion object {
        private const val TAG = "MqttManager"
    }

    private val client: Mqtt5AsyncClient

    // 내부로 들어온 Publish를 Flow로 브릿지
    private val _incoming = MutableSharedFlow<Mqtt5Publish>(extraBufferCapacity = 64)
    val incoming = _incoming.asSharedFlow() // 외부에 read-only 제공

    init {
        Timber.tag(TAG).d("Initializing MQTT Manager (useTls=%b)", cfg.MQTT_USE_TLS)

        // 빌더 초기화
        val builder: Mqtt5ClientBuilder = MqttClient.builder()
            .useMqttVersion5()
            .identifier(cfg.deviceUuid)
            .serverHost(cfg.MQTT_BROKER_HOST)
            .serverPort(cfg.brokerPort)
        Timber.tag(TAG).d("MQTT v5 builder configured: host=%s, port=%d, clientId=%s", cfg.MQTT_BROKER_HOST, cfg.brokerPort, cfg.deviceUuid)

        // TLS 설정이 활성화된 경우에만 TrustStore 설정
        if (cfg.MQTT_USE_TLS) {
            Timber.tag(TAG).d("Applying TLS configuration")
            builder.sslConfig()
                .trustManagerFactory(trustStoreFactory())
                .applySslConfig()
            Timber.tag(TAG).i("TLS configuration applied successfully")
        } else {
            Timber.tag(TAG).i("TLS disabled; connecting without SSL/TLS")
        }

        // 최종 클라이언트 생성 (자동 재연결 포함)
        client = builder
            .automaticReconnectWithDefaultConfig()
            .buildAsync()
        Timber.tag(TAG).i("MQTT client built and ready (instance: %s)", client)

        // 모든 PUBLISH 메시지를 _incoming으로 전달하면서 로그 기록
        client.publishes(MqttGlobalPublishFilter.ALL) { pub ->
            val bytes = pub.getPayloadAsBytes()
            val payload = if (bytes.isNotEmpty()) String(bytes) else "<empty>"
            Timber.tag(TAG).d("Received PUBLISH - topic=%s, payload=%s", pub.topic.toString(), payload)
            _incoming.tryEmit(pub)
        }
    }

    /**
     * MQTT 브로커에 연결하고, 명령 토픽을 구독합니다.
     */
    suspend fun connect() {
        Timber.tag(TAG).i("Connecting to MQTT broker at %s:%d", cfg.MQTT_BROKER_HOST, cfg.brokerPort)
        client.connectWith()
            .simpleAuth()
            .username(cfg.MQTT_USERNAME)
            .password(ByteBuffer.wrap(cfg.MQTT_PASSWORD.toByteArray()))
            .applySimpleAuth()
            .keepAlive(60)
            .cleanStart(false)
            .send()
        Timber.tag(TAG).i("MQTT CONNECT sent")

        // Command 토픽 구독 (QoS2)
        val commandTopic = MqttTopics.command(cfg.deviceUuid)
        client.subscribeWith()
            .topicFilter(commandTopic)
            .qos(MqttQos.EXACTLY_ONCE)
            .send()
        Timber.tag(TAG).i("Subscribed to command topic: %s", commandTopic)
    }

    /** USP Inform 메시지 발행 */
    fun publishInform(payload: ByteArray) {
        val informTopic = MqttTopics.inform(cfg.deviceUuid)
        Timber.tag(TAG).i("Publishing Inform to topic: %s", informTopic)
        client.publishWith()
            .topic(informTopic)
            .payload(ByteBuffer.wrap(payload))
            .qos(MqttQos.EXACTLY_ONCE)
            .send()
        Timber.tag(TAG).i("Inform published successfully")
    }

    /** 연결 해제 */
    fun disconnect() {
        Timber.tag(TAG).i("Disconnecting MQTT client")
        client.disconnect()
        Timber.tag(TAG).i("MQTT client disconnected")
    }
}
