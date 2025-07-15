package kr.co.aromit.tr069legacy.parser.dto

/**
 * TR-069 메시지용 DTO를 하나의 파일에 통합한 sealed class입니다.
 */
sealed class TR069Message {

    /**
     * 장치 식별 정보 (DeviceId 블록).
     *
     * @property manufacturer 제조사
     * @property oui OUI 코드
     * @property productClass 제품 클래스
     * @property serialNumber 일련번호
     */
    data class DeviceId(
        val manufacturer: String,
        val oui: String,
        val productClass: String,
        val serialNumber: String
    ) : TR069Message()

    /**
     * Inform 메시지의 이벤트 구조체.
     *
     * @property code TR-069 EventCode (예: "0 BOOTSTRAP", "1 BOOT", "2 PERIODIC")
     * @property commandKey CommandKey (보통 빈 문자열 사용)
     */
    data class Event(
        val code: String,
        val commandKey: String
    ) : TR069Message()

    /**
     * Inform 요청 페이로드 데이터 클래스.
     *
     * @property deviceId 장치 식별 정보(DeviceId)
     * @property events 이벤트 리스트
     * @property maxEnvelopes 최대 Envelope 개수 (기본값: 1)
     * @property currentTime 현재 시간 (예: "2025-07-07T09:32:11")
     * @property retryCount 재시도 횟수 (기본값: 0)
     * @property userId Inform 사용자 ID
     * @property password Inform 비밀번호
     * @property params 나머지 ParameterList
     */
    data class InformRequest(
        val deviceId: DeviceId,
        val events: List<Event>,
        val maxEnvelopes: Int = 1,
        val currentTime: String,
        val retryCount: Int = 0,
        val userId: String,
        val password: String,
        val params: Map<String, String>
    ) : TR069Message()

    /**
     * Inform 응답 메시지 데이터 클래스.
     *
     * @property messageId 메시지 ID (nullable)
     */
    data class InformResponse(
        val messageId: String?
    ) : TR069Message()
}
