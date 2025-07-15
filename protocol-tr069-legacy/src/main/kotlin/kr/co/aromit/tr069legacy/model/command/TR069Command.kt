package kr.co.aromit.tr069legacy.model.command

/**
 * TR-069 RPC 명령을 타입 안전하게 표현하는 sealed 클래스입니다.
 *
 * ACS → CPE로 전달된 XML 명령을
 * [TR069ResponseParser.parseRpcResponse]를 통해 파싱하여
 * 각 하위 타입으로 변환합니다.
 *
 * @see kr.co.aromit.tvagent.network.TR069ResponseParser
 */
sealed class TR069Command {

    /**
     * 지정된 파라미터들의 현재 값을 조회하도록 요청합니다.
     *
     * @param names 조회할 파라미터 이름 목록
     */
    data class GetParameterValues(val names: List<String>) : TR069Command()

    /**
     * 특정 파라미터들의 값을 설정하도록 요청합니다.
     *
     * @param params 파라미터 이름과 설정할 값을 매핑한 Map
     */
    data class SetParameterValues(val params: Map<String, String>) : TR069Command()

    /**
     * 기기의 원격 재부팅을 요청합니다.
     *
     * ※ 보안 및 승인된 흐름 내에서만 처리되어야 합니다.
     */
    object Reboot : TR069Command()
}
