package kr.co.aromit.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import java.util.UUID

/**
 * DeviceInfoProvider는 장치 식별 및 TR-069에 필요한 디바이스 속성 정보를 제공하는 유틸리티 객체입니다.
 *
 * 주요 기능:
 * - Android ID 가져오기
 * - 에뮬레이터 환경에서 임의의 MAC 주소 생성 및 재사용
 * - 제조사, 제품 모델, OUI, SpecVersion, HardwareVersion, SoftwareVersion
 *
 * util 패키지에 위치시키는 이유:
 * - 다양한 모듈에서 재사용 가능한 범용 기능으로, 특정 도메인에 종속되지 않기 때문입니다.
 */
object DeviceInfoProvider {
    private const val PREFS_NAME = "device_prefs"
    private const val PREF_KEY_MAC = "emulator_mac"

    /**
     * 디바이스의 Android ID를 반환합니다.
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""

    /**
     * 에뮬레이터 환경에서 사용할 임의의 MAC 주소를 생성/재사용합니다.
     */
    fun getOrGenerateMac(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var mac = prefs.getString(PREF_KEY_MAC, null)
        if (mac.isNullOrEmpty()) {
            mac = UUID.randomUUID().toString().replace("-", "").substring(0, 12).uppercase()
            prefs.edit {
                putString(PREF_KEY_MAC, mac)
            }
        }
        return mac
    }

    /**
     * 제조사(Manufacturer) 정보를 반환합니다.
     */
    fun getManufacturer(): String = Build.MANUFACTURER ?: ""

    /**
     * 제품 모델(ProductClass) 정보를 반환합니다.
     */
    fun getProductClass(): String = Build.MODEL ?: ""

    /**
     * MAC 주소 기반 OUI(Organizationally Unique Identifier) 상위 6자리 반환
     */
    fun getOUI(context: Context): String {
        val mac = getOrGenerateMac(context)
        return if (mac.length >= 6) mac.substring(0, 6) else mac
    }

    /**
     * TR-069 SpecVersion 정보를 반환합니다.
     * (예: CPE가 지원하는 CWMP 버전)
     */
    fun getSpecVersion(): String = "1.0"

    /**
     * 하드웨어 버전(HardwareVersion)을 반환합니다.
     * (Android 디바이스 하드웨어 식별자)
     */
    fun getHardwareVersion(): String = Build.HARDWARE ?: ""

    /**
     * 소프트웨어 버전(SoftwareVersion)을 반환합니다.
     * (Android 플랫폼 릴리스 버전)
     */
    fun getSoftwareVersion(): String = Build.VERSION.RELEASE ?: ""

    /**
     * 앱 버전 정보를 반환합니다.
     */
    fun getAppVersion(context: Context): String = try {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        info.versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}
