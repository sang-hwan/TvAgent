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
        Timber.tag(TAG).d("onReceive 호출됨 — action=%s", action)

        // SharedPreferences 로드
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bootstrapDone = prefs.getBoolean(KEY_BOOTSTRAP_DONE, false)
        Timber.tag(TAG).d("SharedPreferences loaded — bootstrapDone=%b", bootstrapDone)

        // 이벤트 타입 결정
        val eventType: String? = when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                val firstRun = !bootstrapDone
                Timber.tag(TAG).d("BOOT_COMPLETED 이벤트 처리 — firstRun=%b", firstRun)
                if (firstRun) {
                    prefs.edit { putBoolean(KEY_BOOTSTRAP_DONE, true) }
                    Config.EVENT_BOOTSTRAP_CODED.also { Timber.tag(TAG).i("Bootstrap event selected — type=%s", it) }
                } else {
                    Config.EVENT_BOOT_CODED.also { Timber.tag(TAG).i("Normal boot event selected — type=%s", it) }
                }
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.tag(TAG).i("PACKAGE_REPLACED 이벤트 처리 — update detected")
                Config.EVENT_BOOT_CODED.also { Timber.tag(TAG).i("Update event selected — type=%s", it) }
            }

            Intent.ACTION_PACKAGE_ADDED -> {
                val pkg = intent.data?.schemeSpecificPart
                Timber.tag(TAG).d("PACKAGE_ADDED 이벤트 처리 — pkg=%s", pkg)
                if (pkg == context.packageName) {
                    prefs.edit { putBoolean(KEY_BOOTSTRAP_DONE, true) }
                    Config.EVENT_BOOTSTRAP_CODED.also { Timber.tag(TAG).i("Post-install bootstrap event selected — type=%s", it) }
                } else {
                    Timber.tag(TAG).i("다른 앱 설치 스킵 — pkg=%s", pkg)
                    null
                }
            }

            else -> {
                Timber.tag(TAG).d("지원하지 않는 action, 스킵 — action=%s", action)
                null
            }
        }

        // 이벤트가 유효하면 AgentService 시작
        eventType?.let { type ->
            Timber.tag(TAG).i("이벤트 감지 — type=%s", type)
            if (!AgentService.isRunning) {
                Timber.tag(TAG).d("AgentService 비실행 상태, 시작 시도")
                startAgentService(context, type)
            } else {
                Timber.tag(TAG).i("AgentService 이미 실행 중 — 스킵")
            }
        } ?: Timber.tag(TAG).d("처리 대상 아님 — action=%s", action)
    }

    private fun startAgentService(context: Context, eventType: String) {
        Timber.tag(TAG).d("startAgentService 호출 — eventType=%s", eventType)
        val svcIntent = Intent(context, AgentService::class.java)
            .putExtra(EXTRA_EVENT_TYPE, eventType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.tag(TAG).d("ForegroundService로 시작 설정 — SDK=%d", Build.VERSION.SDK_INT)
            ContextCompat.startForegroundService(context, svcIntent)
        } else {
            Timber.tag(TAG).d("BackgroundService로 시작 설정 — SDK=%d", Build.VERSION.SDK_INT)
            context.startService(svcIntent)
        }
        Timber.tag(TAG).i("AgentService 시작 요청 완료 — event=%s", eventType)
    }
}
