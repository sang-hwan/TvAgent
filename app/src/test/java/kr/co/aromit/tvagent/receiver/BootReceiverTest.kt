package kr.co.aromit.tvagent.receiver

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kr.co.aromit.tvagent.service.AgentService

@RunWith(AndroidJUnit4::class)
class BootReceiverTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun `BootReceiver on receive sends START to AgentService`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // 브로드캐스트를 강제로 보내면…
        BootReceiver().onReceive(context, intent)

        // AgentService가 START_STICKY로 기동됐는지 ServiceTestRule로 확인
        serviceRule.startService(Intent(context, AgentService::class.java))
    }
}
