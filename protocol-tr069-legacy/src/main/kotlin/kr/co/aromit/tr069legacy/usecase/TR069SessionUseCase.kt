package kr.co.aromit.tr069legacy.usecase

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.co.aromit.tvagent.network.TR069Client
import kr.co.aromit.tvagent.parser.TR069ResponseParser
import kr.co.aromit.tvagent.parser.dto.TR069Message.DeviceId
import kr.co.aromit.tvagent.parser.dto.TR069Message.Event
import kr.co.aromit.tvagent.parser.dto.TR069Message.InformRequest
import kr.co.aromit.tvagent.model.command.TR069Command
import kr.co.aromit.core.Config
import kr.co.aromit.tvagent.util.DateTimeUtil
import kr.co.aromit.core.DeviceInfoProvider
import timber.log.Timber

/**
 * TR-069 Inform 전송 및 RPC 명령 폴링·처리 사이클을 담당하는 유스케이스.
 * AgentService에서 지정된 이벤트 타입을 넘겨 호출합니다.
 */
class TR069SessionUseCase(private val context: Context) {

    companion object {
        private const val TAG = "TR069SessionUseCase"
    }

    private val client = TR069Client(endpoint = Config.ENDPOINT)

    /**
     * 지정된 coded 이벤트 타입으로 한 사이클의 Inform 전송 → RPC 명령 처리.
     *
     * @param eventType Config.EVENT_BOOTSTRAP_CODED, Config.EVENT_BOOT_CODED, Config.EVENT_PERIODIC_CODED 중 하나
     */
    suspend fun execute(eventType: String) = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("▶ execute 시작 eventType=$eventType")

        try {
            // 1) DeviceId 구성
            val androidId = DeviceInfoProvider.getAndroidId(context)
            val mac       = DeviceInfoProvider.getOrGenerateMac(context)
            val serial    = "$androidId-$mac"

            val deviceId = DeviceId(
                manufacturer = DeviceInfoProvider.getManufacturer(),
                oui          = DeviceInfoProvider.getOUI(context),
                productClass = DeviceInfoProvider.getProductClass(),
                serialNumber = serial
            )

            // 2) 현재 시간 (ISO8601+offset)
            val currentTime = DateTimeUtil.currentTimeNow()

            // 3) 이벤트 리스트: 넘겨받은 coded 문자열을 그대로 사용
            val events = listOf(Event(eventType, ""))

            // 4) 커스텀 파라미터 + 디바이스 정보
            val baseParams = buildCustomParams().toMutableMap()
            baseParams += mapOf(
                "Device.DeviceInfo.SpecVersion"     to DeviceInfoProvider.getSpecVersion(),
                "Device.DeviceInfo.HardwareVersion" to DeviceInfoProvider.getHardwareVersion(),
                "Device.DeviceInfo.SoftwareVersion" to DeviceInfoProvider.getSoftwareVersion()
            )

            // 5) InformRequest DTO 생성
            val payload = InformRequest(
                deviceId     = deviceId,
                events       = events,
                maxEnvelopes = 1,
                currentTime  = currentTime,
                retryCount   = 0,
                userId       = serial,
                password     = androidId,
                params       = baseParams
            )

            // 6) Inform 전송 → 응답 파싱
            Timber.tag(TAG).i("→ Inform 전송 시작")
            val responseXml = client.sendInform(payload)
            val messageId   = TR069ResponseParser.parseInformResponse(responseXml).messageId
            Timber.tag(TAG).i("✔ InformResponse 수신 (messageId=$messageId)")

            // 7) RPC 명령 Poll → 파싱 → 처리
            Timber.tag(TAG).i("→ RPC Poll 시작")
            val rpcXml   = client.pollEmpty()
            val commands = TR069ResponseParser.parseRpcResponse(rpcXml)
            Timber.tag(TAG).i("✔ RPC 명령 수신: count=${commands.size}")

            commands.forEach { handleCommand(it) }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "✘ TR-069 세션 실행 중 오류 발생")
        }
    }

    /**
     * 실제 커스텀 파라미터 로직을 구현하세요.
     */
    private fun buildCustomParams(): Map<String, String> =
        emptyMap()

    /**
     * 수신된 TR-069 RPC 명령을 처리합니다.
     */
    private fun handleCommand(command: TR069Command) {
        when (command) {
            is TR069Command.GetParameterValues -> {
                Timber.tag(TAG).i("GetParameterValues 명령: names=${command.names}")
            }
            is TR069Command.SetParameterValues -> {
                Timber.tag(TAG).i("SetParameterValues 명령: params=${command.params}")
            }
            is TR069Command.Reboot -> {
                Timber.tag(TAG).i("Reboot 명령 수신 → 시스템 재시작 처리 필요")
            }
        }
    }
}
