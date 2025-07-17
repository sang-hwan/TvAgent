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
        Timber.tag(TAG).d("Initializing TR-069 Session UseCase (enableTr069=%b)", Config.enableTr069)
        if (Config.enableTr069) {
            val clazz = Class.forName("kr.co.aromit.tr069legacy.usecase.TR069SessionUseCase")
            val ctor = clazz.getConstructor(android.content.Context::class.java)
            val instance = ctor.newInstance(this)
            Timber.tag(TAG).i("TR069SessionUseCase instance created: %s", instance)
            instance
        } else {
            Timber.tag(TAG).i("TR-069 disabled; no SessionUseCase created")
            null
        }
    }

    private val handler by lazy {
        Timber.tag(TAG).d("Handler for ticker initialized on main looper")
        Handler(Looper.getMainLooper())
    }
    private val serviceJob = Job().apply { Timber.tag(TAG).d("Service Job created") }
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob).also {
        Timber.tag(TAG).d("Service CoroutineScope initialized")
    }
    private var tickerScheduled = false

    private val ticker = object : Runnable {
        override fun run() {
            Timber.tag(TAG).d("Ticker invoked — scheduling next after %dms", Config.PERIODIC_INTERVAL_MS)
            if (Config.enableTr069) {
                val m = tr069SessionUseCase!!.javaClass.getMethod("execute", String::class.java)
                m.invoke(tr069SessionUseCase, Config.EVENT_PERIODIC_CODED)
                Timber.tag(TAG).i("Ticker: PERIODIC Inform executed (event=%s)", Config.EVENT_PERIODIC_CODED)
            } else {
                Timber.tag(TAG).i("Ticker: TR-069 disabled; skipping periodic Inform")
            }
            handler.postDelayed(this, Config.PERIODIC_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Timber.tag(TAG).i("onCreate called: isRunning set to true")

        if (!Config.enableTr069) {
            Timber.tag(TAG).i("TR-069 disabled; stopping service immediately")
            stopSelf()
            return
        }

        if (!Config.validate()) {
            Timber.tag(TAG).e("Config validation failed; stopping service")
            stopSelf()
            return
        }

        Timber.tag(TAG).d("Creating notification channel")
        createNotificationChannel()

        Timber.tag(TAG).d("Building notification and starting foreground service")
        startForeground(NOTIF_ID, buildNotification())
        Timber.tag(TAG).i("Foreground service started with notification ID %d", NOTIF_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).i("onStartCommand called — flags=%d, startId=%d", flags, startId)

        if (!Config.enableTr069) {
            Timber.tag(TAG).i("TR-069 disabled; returning START_NOT_STICKY")
            return START_NOT_STICKY
        }

        val eventType = intent?.getStringExtra(EXTRA_EVENT_TYPE)
        if (eventType.isNullOrEmpty()) {
            Timber.tag(TAG).w("onStartCommand: no event type provided; skipping initial Inform")
            return START_STICKY
        }

        Timber.tag(TAG).i("Initial Inform event triggered: %s", eventType)
        serviceScope.launch {
            val m = tr069SessionUseCase!!.javaClass.getMethod("execute", String::class.java)
            m.invoke(tr069SessionUseCase, eventType)
            Timber.tag(TAG).i("Initial Inform completed for event=%s", eventType)
        }

        if (!tickerScheduled) {
            Timber.tag(TAG).d("Scheduling ticker with interval %dms", Config.PERIODIC_INTERVAL_MS)
            handler.postDelayed(ticker, Config.PERIODIC_INTERVAL_MS)
            tickerScheduled = true
            Timber.tag(TAG).i("Ticker scheduled successfully")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.tag(TAG).d("onBind called; this service does not support binding")
        return null
    }

    override fun onDestroy() {
        Timber.tag(TAG).i("onDestroy called: cleaning up service")
        handler.removeCallbacks(ticker)
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isRunning = false
        Timber.tag(TAG).i("Service destroyed: isRunning set to false")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        Timber.tag(TAG).d("createNotificationChannel called")
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_NAME
            Timber.tag(TAG).d("NotificationChannel created: id=%s, name=%s", CHANNEL_ID, CHANNEL_NAME)
        }
        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
        Timber.tag(TAG).i("Notification channel registered")
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(CHANNEL_NAME)
            .setContentText("TR-069 사이클 실행 중")
            .setSmallIcon(R.drawable.ic_agent_notification)
            .setOngoing(true)
            .also {
                Timber.tag(TAG).d("Notification built for foreground service")
            }
            .build()
}
