package kr.co.aromit.network.mqtt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import kr.co.aromit.core.Config
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber

/**
 * MQTT 연결 및 송수신 관리 싱글톤
 *
 * • init/connect/disconnect
 * • publish/subscribe/unsubscribe
 * • 네트워크 단절 시 exponential back-off 재연결
 * • 콜백 인터페이스 지원
 */
object MqttManager {

    @SuppressLint("StaticFieldLeak")
    private lateinit var client: MqttAndroidClient
    private var isConnected = false
    private var retryCount = 0
    private val listeners = mutableListOf<MqttEventListener>()

    /** 앱 시작 시 1회만 호출 */
    fun init(context: Context) {
        Timber.i("MqttManager.init()")
        val appCtx = context.applicationContext
        client = MqttAndroidClient(appCtx, Config.MQTT_BROKER_HOST, Config.mqttClientId)

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                isConnected = true
                retryCount = 0
                Timber.i("MQTT 연결 완료 (reconnect=$reconnect)")
                listeners.forEach { it.onConnected(reconnect) }
            }
            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                Timber.w(cause, "MQTT 연결 끊김")
                listeners.forEach { it.onDisconnected(cause) }
                scheduleReconnect()
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                Timber.d("메시지 수신 (topic=$topic)")
                listeners.forEach { it.onMessage(topic, message.payload) }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Timber.d("메시지 발행 완료")
            }
        })
    }

    /** 브로커에 연결 */
    fun connect() {
        if (isConnected) return
        val opts = MqttConnectOptions().apply {
            isCleanSession    = false
            userName          = Config.MQTT_USERNAME
            password          = Config.MQTT_PASSWORD.toCharArray()
            connectionTimeout = (Config.networkConnectTimeoutMs / 1000).toInt()
            keepAliveInterval = (Config.networkKeepAliveIntervalMs / 1000).toInt()
        }
        Timber.i("MQTT 브로커 연결 시도")
        client.connect(opts, null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) {
                Timber.i("MQTT 연결 성공")
            }
            override fun onFailure(token: IMqttToken?, ex: Throwable?) {
                Timber.e(ex, "MQTT 연결 실패 → 재시도 예약")
                scheduleReconnect()
            }
        })
    }

    /**
     * 메시지 발행
     * @param topic   토픽 (informTopic 등)
     * @param payload 본문 바이트
     * @param qos     0~2
     * @param retained retain 플래그
     */
    fun publish(topic: String, payload: ByteArray, qos: Int = 1, retained: Boolean = false) {
        if (!isConnected) {
            Timber.w("Publish ignored: not connected (topic=$topic)")
            return
        }
        client.publish(topic, payload, qos, retained, null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) { Timber.d("Publish 성공 (topic=$topic)") }
            override fun onFailure(token: IMqttToken?, ex: Throwable?) { Timber.e(ex, "Publish 실패 (topic=$topic)") }
        })
    }

    /** 토픽 구독 */
    fun subscribe(topic: String, qos: Int = 1) {
        if (!isConnected) {
            Timber.w("Subscribe ignored: not connected (topic=$topic)")
            return
        }
        client.subscribe(topic, qos, null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) { Timber.i("Subscribe 성공 (topic=$topic)") }
            override fun onFailure(token: IMqttToken?, ex: Throwable?) { Timber.e(ex, "Subscribe 실패 (topic=$topic)") }
        })
    }

    /** 토픽 구독 해제 */
    fun unsubscribe(topic: String) {
        if (!this::client.isInitialized || !isConnected) return
        client.unsubscribe(topic, null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) { Timber.i("Unsubscribe 성공 (topic=$topic)") }
            override fun onFailure(token: IMqttToken?, ex: Throwable?) { Timber.e(ex, "Unsubscribe 실패 (topic=$topic)") }
        })
    }

    /** 연결 해제 */
    fun disconnect() {
        if (!isConnected) return
        client.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(token: IMqttToken?) {
                isConnected = false
                Timber.i("Disconnect 성공")
            }
            override fun onFailure(token: IMqttToken?, ex: Throwable?) {
                Timber.e(ex, "Disconnect 실패")
            }
        })
    }

    /** 지수적 back-off 재연결 예약 */
    private fun scheduleReconnect() {
        val delaySec = Math.min(60, Math.pow(2.0, retryCount.toDouble()).toLong())
        val delayMs  = delaySec * 1_000L
        Timber.i("Reconnect in ${delaySec}s (retry=$retryCount)")
        retryCount++
        Handler(Looper.getMainLooper()).postDelayed({ connect() }, delayMs)
    }

    /** 외부 이벤트 수신용 리스너 */
    interface MqttEventListener {
        fun onConnected(reconnect: Boolean)
        fun onDisconnected(cause: Throwable?)
        fun onMessage(topic: String, payload: ByteArray)
    }

    /** 리스너 등록/해제 */
    fun addListener(l: MqttEventListener)    = listeners.add(l)
    fun removeListener(l: MqttEventListener) = listeners.remove(l)
}
