package kr.co.aromit.tvagent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kr.co.aromit.tvagent.service.AgentService
import kr.co.aromit.core.Config
import timber.log.Timber

/**
 * BootReceiver는 부팅, 설치/업데이트 시점을 감지해
 * BOOTSTRAP vs BOOT 이벤트를 구분하여 AgentService를 시작합니다.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "agent_prefs"
        private const val KEY_BOOTSTRAP_DONE = "bootstrap_done"
        const val EXTRA_EVENT_TYPE = "EXTRA_EVENT_TYPE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.tag(TAG).i("onReceive 호출됨 — action=$action")

        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val eventType: String? = when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                val firstRun = !prefs.getBoolean(KEY_BOOTSTRAP_DONE, false)
                if (firstRun) {
                    prefs.edit { putBoolean(KEY_BOOTSTRAP_DONE, true) }
                    Config.EVENT_BOOTSTRAP_CODED
                } else {
                    Config.EVENT_BOOT_CODED
                }
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Config.EVENT_BOOT_CODED
            }

            Intent.ACTION_PACKAGE_ADDED -> {
                val pkg = intent.data?.schemeSpecificPart
                if (pkg == context.packageName) {
                    prefs.edit { putBoolean(KEY_BOOTSTRAP_DONE, true) }
                    Config.EVENT_BOOTSTRAP_CODED
                } else {
                    Timber.tag(TAG).i("다른 앱 설치 스킵 — pkg=$pkg")
                    null
                }
            }

            else -> null
        }

        eventType?.let { type ->
            Timber.tag(TAG).i("이벤트 감지 — type=$type")
            if (!AgentService.isRunning) {
                startAgentService(context, type)
            } else {
                Timber.tag(TAG).i("AgentService 이미 실행 중 — 스킵")
            }
        } ?: Timber.tag(TAG).d("처리 대상 아님 — action=$action")
    }

    private fun startAgentService(context: Context, eventType: String) {
        val svcIntent = Intent(context, AgentService::class.java)
            .putExtra(EXTRA_EVENT_TYPE, eventType)

        // 포그라운드 서비스 호출 분기: 테스트 환경에서 startService 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, svcIntent)
        } else {
            context.startService(svcIntent)
        }
        Timber.tag(TAG).i("AgentService 시작 요청 (event=$eventType)")
    }
}