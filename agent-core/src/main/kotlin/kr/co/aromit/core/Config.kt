package kr.co.aromit.core

import android.content.Context
import timber.log.Timber
import java.util.UUID

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
    /** MQTT 브로커 접속 URI (TLS) */
    const val MQTT_BROKER_HOST = "ssl://aromit.iptime.org:58883"
    /** MQTT 인증 ID */
    const val MQTT_USERNAME = "admin"
    /** MQTT 인증 PW */
    const val MQTT_PASSWORD = "aromit123"

    // ===== TR-069 (ACS) - Legacy 관련 설정값 =====
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

    // SharedPreferences 키
    private const val PREF_NAME = "agent_prefs"
    private const val KEY_UUID   = "device_uuid"

    /**
     * 이 Agent의 고유 디바이스 UUID
     * initDeviceUuid() 호출로 초기화됩니다.
     */
    var deviceUuid: String = ""
        private set

    /**
     * SharedPreferences에서 UUID를 불러오거나, 없으면 신규 생성하여 저장합니다.
     */
    fun initDeviceUuid(ctx: Context) {
        val sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        deviceUuid = sp.getString(KEY_UUID, null)
            ?: UUID.randomUUID().toString().also {
                sp.edit().putString(KEY_UUID, it).apply()
            }
        Timber.i("Device UUID initialized: $deviceUuid")
    }

    /** 네트워크 연결(Connect) 타임아웃 (ms) */
    var networkConnectTimeoutMs: Long = 10_000L
    /** 네트워크 읽기(Read) 타임아웃 (ms) */
    var networkReadTimeoutMs: Long = 10_000L
    /** 네트워크 유지 핑(Keep-alive) 간격 (ms) */
    var networkKeepAliveIntervalMs: Long = 60_000L

    /**
     * 현재 설정값의 유효성을 검사합니다.
     *
     * @return 타임아웃과 MQTT URI 스킴이 올바르면 true
     */
    fun validate(): Boolean {
        Timber.i("Config.validate() 호출")
        val timeoutOk = networkConnectTimeoutMs in 1_000..60_000 &&
                networkReadTimeoutMs    in 1_000..60_000 &&
                networkKeepAliveIntervalMs in 1_000..300_000

        // MQTT URI 검증 (ssl:// 스킴)
        val mqttUriOk = MQTT_BROKER_HOST.startsWith("ssl://")

        Timber.d(
            "타임아웃 유효: $timeoutOk, " +
                    "MQTT URI 유효: $mqttUriOk"
        )
        return timeoutOk && mqttUriOk
    }
}
