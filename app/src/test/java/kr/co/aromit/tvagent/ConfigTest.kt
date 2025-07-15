package kr.co.aromit.tvagent

import org.junit.Assert.assertFalse
import org.junit.Test
import kr.co.aromit.core.Config

class ConfigTest {
    @Test
    fun `enableTr069 is false by default`() {
        assertFalse("enableTr069 플래그가 false여야 합니다", Config.enableTr069)
    }
}
