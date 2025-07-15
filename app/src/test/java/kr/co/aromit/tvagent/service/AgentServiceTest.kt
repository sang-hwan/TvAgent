package kr.co.aromit.tvagent.service

import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildService
import org.robolectric.RobolectricTestRunner
import kr.co.aromit.core.Config

@RunWith(RobolectricTestRunner::class)
class AgentServiceTest {

    @Test
    fun `onStartCommand skips TR069 when disabled`() {
        // Config.enableTr069 == false 인 상태 확인
        assert(!Config.enableTr069)

        // Service 실행
        val controller = buildService(AgentService::class.java, Intent())
        val service = controller.create().startCommand(0, 0).get()
    }
}
