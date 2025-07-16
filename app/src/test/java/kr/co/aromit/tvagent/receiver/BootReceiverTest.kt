package kr.co.aromit.tvagent.receiver

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import kr.co.aromit.tvagent.service.AgentService

@RunWith(RobolectricTestRunner::class)
class BootReceiverTest {

    @Test
    fun `onReceive should start AgentService`() {
        // given
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // when
        BootReceiver().onReceive(context, bootIntent)

        // then: ShadowApplication이 가로챈 다음 시작된 서비스 Intent 검사
        val started = Shadows.shadowOf(context).nextStartedService
        assertEquals(
            AgentService::class.java.name,
            started.component?.className
        )
    }
}
