package kr.co.aromit.tvagent.application

import android.app.Application
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.aromit.tvagent.BuildConfig
import kr.co.aromit.core.Config

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
        initLogger()             // 로그 초기화
        validateConfig()         // 설정 검증
        // Legacy(TR‑069)가 활성화된 경우에만 테스트
        if (Config.enableTr069) {
            testTr069Client()
        } else {
            Timber.tag(TAG).i("TR‑069 disabled; client test skip")
        }
    }

    /**
     * Timber를 디버그 모드에서만 초기화하고, 시작 로그를 남깁니다.
     */
    private fun initLogger() {
        if (!BuildConfig.DEBUG) return
        Timber.plant(Timber.DebugTree())
        Timber.tag(TAG).i("앱 시작: onCreate 호출됨 (디버그 모드)")
    }

    /**
     * Config.validate() 결과에 따라 INFO 또는 ERROR 로그를 남깁니다.
     */
    private fun validateConfig() {
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
