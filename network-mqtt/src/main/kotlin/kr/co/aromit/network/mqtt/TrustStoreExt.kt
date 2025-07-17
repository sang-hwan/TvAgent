package kr.co.aromit.network.mqtt

import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.TrustManagerFactory
import timber.log.Timber

/**
 * PEM 형식 인증서가 담긴 InputStream을 사용해 KeyStore를 초기화하고,
 * SSL/TLS 연결 시 사용할 TrustManagerFactory를 생성합니다.
 *
 * Timber 로그를 통해 프로세스 흐름을 추적합니다.
 *
 * 사용 예시:
 * val certStream = resources.openRawResource(R.raw.emqx_ca)
 * val trustManagerFactory = certStream.toTrustManagerFactory()
 *
 * @receiver PEM 형식 인증서 InputStream
 * @return 초기화된 TrustManagerFactory
 */
fun InputStream.toTrustManagerFactory(): TrustManagerFactory {
    val tag = "TrustStoreExt"
    Timber.tag(tag).d("toTrustManagerFactory 시작")
    val algorithm = TrustManagerFactory.getDefaultAlgorithm()
    Timber.tag(tag).d("TrustManagerFactory 알고리즘: %s", algorithm)

    val cf = CertificateFactory.getInstance("X.509").also {
        Timber.tag(tag).d("CertificateFactory 생성: %s", it)
    }

    val cert = cf.generateCertificate(this).also {
        Timber.tag(tag).d("인증서 로드 및 생성 완료: %s", it)
    }

    val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        Timber.tag(tag).d("빈 KeyStore 생성 및 로드: type=%s", KeyStore.getDefaultType())
        setCertificateEntry("ca", cert)
        Timber.tag(tag).d("인증서 KeyStore에 등록: alias='ca'")
    }

    val tmf = TrustManagerFactory.getInstance(algorithm).apply {
        init(ks)
        Timber.tag(tag).d("TrustManagerFactory 초기화 완료: aliases=%s", ks.aliases().toList())
    }

    Timber.tag(tag).i("toTrustManagerFactory 완료")
    return tmf
}
