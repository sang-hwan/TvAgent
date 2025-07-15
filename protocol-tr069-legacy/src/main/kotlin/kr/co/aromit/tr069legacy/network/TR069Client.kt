package kr.co.aromit.tr069legacy.network

import kr.co.aromit.tvagent.parser.TR069RequestBuilder
import kr.co.aromit.tvagent.parser.dto.TR069Message.InformRequest
import kr.co.aromit.core.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * TR-069 RPC 통신을 담당하는 HTTP 클라이언트입니다.
 *
 * 단일 엔드포인트 "/Inform"를 통해 CPE ↔ ACS 간 모든
 * 메시지를 주고받으며, 요청 페이로드는 TR069RequestBuilder를
 * 통해 생성된 XML을 사용합니다.
 *
 * @param endpoint ACS 서버 기본 URL (포트 포함)
 */
class TR069Client(
    private val endpoint: String = Config.ENDPOINT
) {
    companion object {
        private const val TAG = "TR069Client"
        private val MEDIA_TYPE = "text/xml; charset=utf-8".toMediaType()
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Config.networkConnectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(Config.networkReadTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Inform 요청 데이터를 받아 SOAP/XML로 빌드한 뒤 ACS에 전송합니다.
     *
     * @param payload CPE에서 보낼 Inform 요청 페이로드
     * @return ACS가 반환한 InformResponse(ACK) 본문 문자열
     * @throws IOException 전송 실패 또는 HTTP 오류 시 발생
     */
    @Throws(IOException::class)
    suspend fun sendInform(payload: InformRequest): String = withContext(Dispatchers.IO) {
        val xml = TR069RequestBuilder.buildInform(payload)
        postRequest(xml)
    }

    /**
     * 빈 SOAP Body를 가진 폴링용 Inform 메시지를 전송하여
     * ACS에 대기 중인 RPC 명령이 있는지 확인합니다.
     *
     * @return ACS가 반환한 RPC 명령 응답(없으면 빈 Body)
     * @throws IOException 전송 실패 또는 HTTP 오류 시 발생
     */
    @Throws(IOException::class)
    suspend fun pollEmpty(): String = withContext(Dispatchers.IO) {
        val xml = TR069RequestBuilder.buildEmpty()
        postRequest(xml)
    }

    /**
     * RPC 처리 결과 XML을 ACS로 전송합니다.
     *
     * @param responseXml CPE에서 처리 결과로 보낼 SOAP/XML 문자열
     * @return ACS가 반환한 최종 ACK 응답 본문 문자열
     * @throws IOException 전송 실패 또는 HTTP 오류 시 발생
     */
    @Throws(IOException::class)
    suspend fun sendResponse(responseXml: String): String = withContext(Dispatchers.IO) {
        postRequest(responseXml)
    }

    /**
     * 단일 엔드포인트 "/Inform"에 대해 HTTP POST 요청을 실행합니다.
     *
     * @param bodyXml 전체 SOAP Envelope(XML) 요청 본문
     * @return ACS 응답 본문 문자열
     * @throws IOException HTTP 오류 또는 빈 본문일 때 발생
     */
    @Throws(IOException::class)
    private fun postRequest(bodyXml: String): String {
        val url = "$endpoint/Inform"
        val request = Request.Builder()
            .url(url)
            .post(bodyXml.toRequestBody(MEDIA_TYPE))
            .addHeader("Content-Type", "text/xml; charset=utf-8")
            .addHeader("SOAPAction", "")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.tag(TAG)
                    .e("HTTP 오류: code=${response.code}, message=${response.message}")
                throw IOException("HTTP 요청 실패: ${response.code} - ${response.message}")
            }
            return response.body?.string()
                ?: throw IOException("응답 본문이 비어 있습니다.")
        }
    }
}
