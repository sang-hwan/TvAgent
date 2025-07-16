package kr.co.aromit.tvagent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kr.co.aromit.tvagent.R
import kr.co.aromit.tvagent.receiver.BootReceiver
import kr.co.aromit.core.Config
import timber.log.Timber

/**
 * AgentService는 포그라운드에서 TR-069 사이클을 처리합니다.
 */
class AgentService : Service() {

    companion object {
        private const val TAG = "AgentService"
        private const val CHANNEL_ID = "agent_service_channel"
        private const val CHANNEL_NAME = "Agent Service"
        private const val NOTIF_ID = 1001
        const val EXTRA_EVENT_TYPE = BootReceiver.EXTRA_EVENT_TYPE
        @Volatile
        var isRunning: Boolean = false
    }

    private val tr069SessionUseCase: Any? by lazy {
        if (Config.enableTr069) {
            val clazz = Class.forName(
                "kr.co.aromit.tr069legacy.usecase.TR069SessionUseCase"
            )
            val ctor = clazz.getConstructor(android.content.Context::class.java)
            ctor.newInstance(this)
        } else null
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var tickerScheduled = false

    private val ticker = object : Runnable {
        override fun run() {
            if (Config.enableTr069) {
                val m = tr069SessionUseCase!!.javaClass.getMethod("execute", String::class.java)
                m.invoke(tr069SessionUseCase, Config.EVENT_PERIODIC_CODED)
                Timber.tag(TAG).i("Ticker: PERIODIC Inform 완료")
            } else {
                Timber.tag(TAG).i("TR-069 disabled; skipping periodic Inform")
            }
            handler.postDelayed(this, Config.PERIODIC_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Timber.tag(TAG).i("AgentService onCreate: isRunning=true")

        // TR-069 자체가 꺼져 있으면 validation/foreground 진입 모두 스킵
        if (!Config.enableTr069) {
            Timber.tag(TAG).i("TR-069 disabled; skipping service startup")
            stopSelf()
            return
        }

        if (!Config.validate()) {
            Timber.tag(TAG).e("Config 유효성 검사 실패, 서비스 종료")
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        Timber.tag(TAG).i("AgentService onCreate: 포그라운드 서비스 시작")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TR-069 비활성화 시 바로 종료
        if (!Config.enableTr069) {
            Timber.tag(TAG).i("TR-069 disabled; skipping service start")
            return START_NOT_STICKY
        }

        val eventType = intent?.getStringExtra(EXTRA_EVENT_TYPE)
        if (eventType == null) {
            Timber.tag(TAG).w("onStartCommand: 이벤트 타입 없음, 초기 Inform 스킵")
            return START_STICKY
        }

        Timber.tag(TAG).i("onStartCommand 호출됨 — eventType=$eventType")
        serviceScope.launch {
            val m = tr069SessionUseCase!!.javaClass.getMethod("execute", String::class.java)
            m.invoke(tr069SessionUseCase, eventType)
            Timber.tag(TAG).i("Initial $eventType Inform 완료")
        }

        if (!tickerScheduled) {
            handler.postDelayed(ticker, Config.PERIODIC_INTERVAL_MS)
            tickerScheduled = true
            Timber.tag(TAG).i("Ticker 시작: ${Config.PERIODIC_INTERVAL_MS}ms 후")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isRunning = false
        Timber.tag(TAG).i("AgentService onDestroy: isRunning=false, 서비스 종료")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = CHANNEL_NAME }

        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(CHANNEL_NAME)
            .setContentText("TR-069 사이클 실행 중")
            .setSmallIcon(R.drawable.ic_agent_notification)
            .setOngoing(true)
            .build()
}
