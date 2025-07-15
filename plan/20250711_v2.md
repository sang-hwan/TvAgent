## 🚩 Android TV Agent 세부 개발 계획서 (**MQTT / TR‑369 전용, TR‑069 Legacy 비활성화**)

> **최종 수정 : 2025‑07‑11**
> 본 버전은 *TR‑069 코드를 “legacy”로 보존하되 기본 빌드·실행에서는 **완전히 제외*** 하여, **MQTT TR‑369(USP)** 로만 동작하도록 정리한 문서입니다.

---

### 📌 프로젝트 최종 목표

* **IP 디바이스**를 **MQTT 기반 TR‑369(USP)** 로 모니터링·제어한다.
* TR‑069 스택은 *컴파일만 가능* (`:protocol‑tr069‑legacy`)하고 **런타임에서는 호출되지 않음**.

---

### 📌 진행 상태 표시

* ✅ 완료 | 🚧 진행 중 | ⬜ 예정

---

## ✅ 단계 1 ~ 4 — **TR‑069 Legacy 골격 구축(더 이상 실행 안 함)**

| 단계 | 내용                                                                                         | 상태 |
| -- | ------------------------------------------------------------------------------------------ | -- |
| 1  | Android Studio Meerkat, Kotlin/Java 21, Gradle 8.14.2 세팅                                   | ✅  |
| 2  | `:app` → `:agent‑core`로 모듈명 변경, 기본 패키지 레이아웃 완성                                             | ✅  |
| 3  | **LEGACY** `Tr069Client`, `AcsConnectionManager` 등 구현 → `:protocol‑tr069‑legacy`로 이동·주석 처리 | ✅  |
| 4  | BOOT/BOOTSTRAP Inform 로직 작성 → **@Deprecated** 주석 후 빌드 제외                                   | ✅  |

> **TIP** : `gradle.properties`에 `enableTr069=false` 기본 설정 (필요 시 true 로 롤백 테스트 가능)

---

## 🚧 단계 5 — **MQTT USP 핵심 도입 & TR‑069 완전 비활성화**

| 분류         | 파일·모듈                                              | 주요 작업                        |
| ---------- | -------------------------------------------------- | ---------------------------- |
| **신규 모듈**  | `:network‑mqtt`, `:protocol‑usp`                   | EMQX 클라이언트 + Protobuf 스키마    |
| **핵심 클래스** | `MqttManager.kt`                                   | 연결·재연결·QoS2 지속 세션            |
|            | `UspSessionUseCase.kt`                             | Inform 전송, Command 처리 진입점    |
|            | `UspMessageDispatcher.kt`                          | MQTT PUBLISH 수신 → 라우팅        |
| **설정**     | `Config.kt`                                        | `initDeviceUuid()` 로 UUID 보장 |
| **변환기**    | `UspTr069Converter.kt`                             | *데이터 모델 매핑* 전용(호환 테스트 목적)    |
| **주요 변경점** | `Tr069*` 클래스 전부 `@Disabled("legacy")` 또는 DI 바인딩 제거 |                              |

#### 🔗 MQTT 브로커 (개발)

* **URL** : `mqtts://aromit.iptime.org:8883` (TLS, EMQX 5.7)
* **계정** : `admin / aromit123`

#### 📡 토픽 규격

| 목적    | 토픽                               |
| ----- | -------------------------------- |
| 상태 보고 | `usp/agent/{deviceUuid}/inform`  |
| 명령 수신 | `usp/agent/{deviceUuid}/command` |

---

## ⬜ 단계 6 — Agent 리소스 정보 보고 (USP Periodic)

* `DeviceStatsProvider.kt` : CPU·RAM·Storage·App 용량 수집
* `AgentService` ticker → `UspSessionUseCase.execute(EVENT_PERIODIC)`
* `DeviceStatsMapper.kt` : 리소스 → USP ParameterList 변환

---

## ⬜ 단계 7 — OTA(펌웨어) 업데이트 (USP Download / Install)

* `UspOtaCommand.kt` (Download·Install)
* `OtaManager.kt`, `OtaJob.kt`, `OtaRepository.kt` : 다운로드 → 검증 → 설치 로그 관리
* `CommandExecutor.kt` 분기에서 `OtaManager.enqueue()` 호출

---

## ⬜ 단계 8 — Android TV 앱 정보 연동

* `TvAppController.kt` : 설치 앱 목록·버전·Enable 여부·Intent 제어
* `AppInfoMapper.kt` : USP `Device.App.*` 매핑
* USP Notify 로 주기적 앱 정보 보고

---

## ⬜ 단계 9 — Intent 기반 타 앱 제어

* `IntentCommand.kt` (타입‑세이프 모델)
* `TvAppController.sendCommand()` ↔ `CommandBroadcastReceiver.kt` 양방향 교신

---

## ⬜ 단계 10 — 보조 앱 (**:companion**) 개발

* `CompanionService` 자동 실행·상태/설정 API
* Agent ↔ Companion : 공통 `CommandBroadcastReceiver` 재사용
* 라이브러리 형태로 분리, 독립 배포

---

## ⬜ 단계 11 — Controller MQTT Command 처리 확장

* USP RPC `Add`, `Delete`, `Operate`, `Set`, `Get` 전부 지원
* `CommandExecutor.kt` : 단일 디스패처 → OtaManager, TvAppController 등 연결

---

## ⬜ 단계 12 — 단위 테스트

* `MqttManagerTest.kt`, `UspParserTest.kt`, `CommandExecutorTest.kt`
* `io.mockk:mockk:1.13.11` : 코루틴·비동기 Mock
* Legacy 테스트는 `@Disabled("legacy")`

---

## ⬜ 단계 13 — 통합 테스트·디버깅

* `docker-compose` → EMQX + USP Test Harness 컨테이너 기동
* BOOT → PERIODIC → OTA → 앱 제어 End‑to‑End 검증

---

## ⬜ 단계 14 — 시연·목업

* `/demo/usp_samples/*.bin` : 최신 USP 스키마 기반 예제
* `scripts/run_demo.sh` : EMQX 기동 + BOOT 이벤트 시뮬레이션
* `docs/demo_scenario.md` : 단계별 화면·로그 캡처 가이드

---

### 📌 향후 고려·보완 사항

* **멀티모듈 분리** : `:agent‑core`, `:network‑mqtt`, `:protocol‑usp`, `:protocol‑tr069‑legacy`
* **R8 난독화** + ProGuard 최신 규칙
* **TLS 의무화** : `network_security_config.xml` clear‑text 차단
* **targetSdk 36 권한 대응** (포그라운드 Service 제한 등)
* **벤더‑별 플러그인 아키텍처** + 성능 최적화
