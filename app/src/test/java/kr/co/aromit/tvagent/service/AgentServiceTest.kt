package kr.co.aromit.tvagent.service

import android.app.Service
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildService
import org.robolectric.RobolectricTestRunner
import kr.co.aromit.core.Config

@RunWith(RobolectricTestRunner::class)
class AgentServiceTest {

    @Test
    fun `onStartCommand skips TR069 when disabled`() {
        // Config.enableTr069 must be false
        assertFalse(Config.enableTr069)

        // 서비스 생성
        val controller = buildService(AgentService::class.java, Intent())
            .create()
        val service = controller.get()

        // enableTr069 == false 일 때 onStartCommand 호출
        val result = service.onStartCommand(Intent(), /*flags*/0, /*startId*/0)

        // START_NOT_STICKY 로 빠져나와야 테스트 통과
        assertEquals(Service.START_NOT_STICKY, result)
    }
}
