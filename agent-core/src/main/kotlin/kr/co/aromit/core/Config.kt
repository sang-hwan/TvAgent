package kr.co.aromit.core

import timber.log.Timber

/**
 * 에이전트의 공통 설정값을 관리하는 객체입니다.
 *
 * - MQTT 브로커 정보
 * - 네트워크 타임아웃
 * - USP/ACS 엔드포인트
 * - TR-069 이벤트 코드
 * - 디바이스 UUID 등
 */
object Config {
    /** gradle.properties의 enableTr069 값 */
    val enableTr069: Boolean = BuildConfig.ENABLE_TR069

    // ===== 컴파일 타임 상수 (const val) =====
    /** MQTT 브로커 접속 호스트 */
    const val MQTT_BROKER_HOST = "tcp://aromit.iptime.org:18083"
    /** MQTT 인증 ID */
    const val MQTT_USERNAME = "admin"
    /** MQTT 인증 PW */
    const val MQTT_PASSWORD = "aromit123"

    // ===== TR-069 (ACS) 관련 설정값 =====
    /** ACS 서버 엔드포인트 URL */
    const val ENDPOINT = "http://192.168.0.24:28080/tr-069"
    /** CWMP 네임스페이스 */
    const val CWMP_NAMESPACE = "urn:dslforum-org:cwmp-1-0"
    /** 정기 보고 주기 (밀리초) */
    const val PERIODIC_INTERVAL_MS: Long = 30 * 60 * 1000L
    /** BOOTSTRAP 이벤트 */
    const val EVENT_BOOTSTRAP = "BOOTSTRAP"
    /** BOOTSTRAP 이벤트 (코드) */
    const val EVENT_BOOTSTRAP_CODED = "0 BOOTSTRAP"
    /** BOOT 이벤트 */
    const val EVENT_BOOT = "BOOT"
    /** BOOT 이벤트 (코드) */
    const val EVENT_BOOT_CODED = "1 BOOT"
    /** PERIODIC 이벤트 */
    const val EVENT_PERIODIC = "PERIODIC"
    /** PERIODIC 이벤트 (코드) */
    const val EVENT_PERIODIC_CODED = "2 PERIODIC"

    // ===== 런타임 프로퍼티 (val/var) =====
    /** 각 연결마다 고유한 MQTT Client ID 반환 */
    val mqttClientId: String
        get() = "TvAgent_${System.currentTimeMillis()}"

    /**
     * 이 Agent의 고유 디바이스 UUID
     * 런타임에서 반드시 실제 값으로 세팅 필요
     */
    var deviceUuid: String = ""

    /** 네트워크 연결(Connect) 타임아웃 (ms) */
    var networkConnectTimeoutMs: Long = 10_000L
    /** 네트워크 읽기(Read) 타임아웃 (ms) */
    var networkReadTimeoutMs: Long = 10_000L
    /** 네트워크 유지 핑(Keep-alive) 간격 (ms) */
    var networkKeepAliveIntervalMs: Long = 60_000L

    /**
     * 현재 설정값의 유효성을 검사합니다.
     *
     * @return 모든 주요 설정이 올바르면 true
     */
    fun validate(): Boolean {
        Timber.i("Config.validate() 호출")
        val endpointOk = ENDPOINT.startsWith("http://") || ENDPOINT.startsWith("https://")
        val timeoutOk  = networkConnectTimeoutMs in 1_000..60_000 &&
                networkReadTimeoutMs    in 1_000..60_000 &&
                networkKeepAliveIntervalMs in 1_000..300_000
        Timber.d("endpoint 유효: $endpointOk, 타임아웃 유효: $timeoutOk")
        return endpointOk && timeoutOk
    }
}
