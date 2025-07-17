package kr.co.aromit.tvagent.application

import android.app.Application
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.aromit.tvagent.R
import kr.co.aromit.tvagent.BuildConfig
import kr.co.aromit.core.Config
import kr.co.aromit.network.mqtt.MqttManager
import kr.co.aromit.network.mqtt.toTrustManagerFactory

/**
 * TvAgentApplication은 앱 프로세스 생성 시 전역 초기화를 수행합니다.
 */
class TvAgentApplication : Application() {

    private val appScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "TvAgentApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("Application onCreate 시작")
        initLogger()             // 로그 초기화
        validateConfig()         // 설정 검증

        // 1) 디바이스 UUID 초기화 (MQTT clientId로 사용)
        Config.initDeviceUuid(this)
        Timber.tag(TAG).d("Device UUID initialized: %s", Config.deviceUuid)

        // 무거운 초기화 작업은 IO 스레드에서 수행
        appScope.launch {
            // 2) raw 리소스에서 PEM 인증서 열기
            Timber.tag(TAG).d("Loading CA certificate from raw resource: R.raw.emqx_ca=%d", R.raw.emqx_ca)
            val certStream = resources.openRawResource(R.raw.emqx_ca)
            Timber.tag(TAG).i("CA certificate InputStream obtained successfully")

            // 3) TrustManagerFactory 생성 함수를 MqttManager에 주입
            Timber.tag(TAG).d("Injecting TrustManagerFactory into MqttManager")
            val mqttManager = MqttManager(
                trustStoreFactory = certStream::toTrustManagerFactory,
                cfg = Config
            )
            Timber.tag(TAG).i("MqttManager initialized: %s", mqttManager)
            // TODO: 필요시 mqttManager.connect() 등 추가 호출
        }

        // Legacy(TR‑069) 활성화 여부 확인
        Timber.tag(TAG).d("Config.enableTr069 flag value: %b", Config.enableTr069)
        if (Config.enableTr069) {
            Timber.tag(TAG).i("TR‑069 enabled; starting client test")
            testTr069Client()
        } else {
            Timber.tag(TAG).i("TR‑069 disabled; client test skip")
        }
        Timber.tag(TAG).d("Application onCreate 완료")
    }

    /**
     * Timber를 디버그 모드에서만 초기화하고, 시작 로그를 남깁니다.
     */
    private fun initLogger() {
        Timber.tag(TAG).d("initLogger 호출 (DEBUG=%b)", BuildConfig.DEBUG)
        if (!BuildConfig.DEBUG) return
        Timber.plant(Timber.DebugTree())
        Timber.tag(TAG).i("앱 시작: onCreate 호출됨 (디버그 모드)")
    }

    /**
     * Config.validate() 결과에 따라 INFO 또는 ERROR 로그를 남깁니다.
     */
    private fun validateConfig() {
        Timber.tag(TAG).d("validateConfig 호출")
        if (Config.validate()) {
            Timber.tag(TAG).i("ACS 설정 정상: endpoint=${Config.ENDPOINT}")
        } else {
            Timber.tag(TAG).e("ACS 설정 오류: endpoint=${Config.ENDPOINT}")
        }
    }

    /**
     * 디버그 모드에서만 TR-069 클라이언트를 생성해 보고,
     * 성공/실패 여부를 로그로 남깁니다.
     */
    private fun testTr069Client() {
        Timber.tag(TAG).d("testTr069Client 호출 (DEBUG=%b, enableTr069=%b)", BuildConfig.DEBUG, Config.enableTr069)
        if (!BuildConfig.DEBUG || !Config.enableTr069) return
        appScope.launch {
            runCatching {
                // 모듈이 존재할 때만 reflection 으로 인스턴스화
                val clazz = Class.forName(
                    "kr.co.aromit.tr069legacy.network.TR069Client"
                )
                val ctor = clazz.getConstructor(String::class.java)
                ctor.newInstance(Config.ENDPOINT)
            }.onSuccess {
                Timber.tag(TAG).i("TR-069 클라이언트 초기화 성공")
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "TR-069 클라이언트 초기화 실패")
            }
        }
    }
}
