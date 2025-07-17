## 🚩 Android TV Agent 세부 개발 계획서  
(**MQTT / TR‑369 전용 ― TR‑069 Legacy 비활성화**)

> **최종 수정 : 2025‑07‑17**  
> TR‑069 코드는 `:protocol‑tr069‑legacy` 모듈로 **보존**하지만  
> **기본 빌드·실행에는 완전히 제외**하고,  
> **MQTT TR‑369(USP)** 로만 동작하도록 통합·정리한 문서입니다.

---

### 📌 프로젝트 최종 목표
* **IP 디바이스**를 **MQTT TR‑369(USP)** 로 모니터링·제어  
* TR‑069 스택은 컴파일만 가능 (`:protocol‑tr069‑legacy`) 하고 **런타임 호출 금지**  
* **CI 파이프라인**에서 TR‑069 호출 여부 차단 및 USP 기능 정상 여부 **자동 검증**

---

### 📌 진행 상태 표시
* ✅ 완료 | 🚧 진행 중 | ⬜ 예정·대기

---

## ✅ 단계 1 ~ 4 ― **TR‑069 Legacy 골격 구축 (비활성화 완료)**

| 단계 | 내용                                                                                                               | 상태 |
| ---- | ---------------------------------------------------------------------------------------------------------------- | ---- |
| 1    | 개발 환경 세팅 : Android Studio Meerkat, Kotlin/Java 21, Gradle 8.14.2                                             | ✅   |
| 2    | Gradle 멀티모듈 분리 (`:app`, `:agent-core`, `:network-mqtt`, `:protocol-usp`, `:protocol-tr069-legacy`)           | ✅   |
| 3    | LEGACY 코드(`Tr069Client`, `AcsConnectionManager`) 이동·주석 처리                                                  | ✅   |
| 4    | BOOT/BOOTSTRAP Inform 로직 Deprecated & 빌드 제외, `Config.enableTr069 = false` 기본화                             | ✅   |

---

## 🚧 단계 5 ― **MQTT USP 핵심 도입 & TR‑069 완전 비활성화**

| 분류          | 파일 및 모듈                               | 주요 작업                                             |
|---------------|-----------------------------------------|---------------------------------------------------|
| 관련 모듈     | `:network-mqtt`, `:protocol-usp`        | EMQX 클라이언트 및 Protobuf 스키마 적용                      |
| 핵심 클래스   | `MqttManager.kt`                        | TLS 지원, MQTT 연결·재연결, QoS2 지속 세션 관리               |
|               | `UspSessionUseCase.kt`                  | USP Inform 메시지 발행 및 Command 진입점 처리               |
|               | `UspMessageDispatcher.kt`               | MQTT PUBLISH 메시지 라우팅                                |
| 설정 및 UUID  | `Config.kt`                             | `initDeviceUuid()`를 이용한 고정 UUID 설정                  |
| Boot → USP    | `AgentService.kt`                       | 부팅 시 TR-069 Inform 대신 USP Inform 메시지 발행으로 변경   |
| MQTT 토픽     | 상태: `usp/agent/{uuid}/inform`<br>명령: `usp/agent/{uuid}/command` | 상태 및 명령 토픽 구조 명확화     |
| 브로커 주소   | **`tcp://aromit.iptime.org:18083`**<br>(EMQX 5.7, 계정 `admin/aromit123`) | EMQX 브로커 연결 정보 명시  |

### 포트 매핑 정보
- `51883` → `1883` (MQTT/TCP)
- `58883` → `8883` (MQTT/TLS)

---

## ⬜ 단계 6 ― **리소스 정보 & Periodic Inform**
* `DeviceStatsProvider.kt` : CPU/RAM/Storage/App 용량 수집  
* `DeviceStatsMapper.kt` : 수집값 → USP `ParameterList` 변환  
* `AgentService` ticker → 주기적(`EVENT_PERIODIC`) 호출  
* Periodic Stats 전송 스케줄링  

---

## ⬜ 단계 7 ― **OTA (펌웨어/앱) Download & Install**
* `UspOtaCommand.kt` : USP Download / Install RPC 처리  
* `OtaManager.kt`, `OtaJob.kt`, `OtaRepository.kt` : 다운로드→검증→설치→로그  
* `CommandExecutor.kt` 분기 처리  

---

## ⬜ 단계 8 ― **Android TV 앱 정보 연동**
* `TvAppController.kt` : 설치 앱 목록·버전·Enable 상태·Intent 제어  
* `AppInfoMapper.kt` : USP `Device.App.*` 데이터 매핑  
* 주기적 USP Notify 앱 정보 보고  

---

## ⬜ 단계 9 ― **Intent 기반 외부 앱 제어**
* `IntentCommand.kt` : 타입‑세이프 모델 정의  
* `TvAppController` ↔ `CommandBroadcastReceiver` Intent 기반 양방향 교신  

---

## ⬜ 단계 10 ― **Controller Command 처리 확장**
* USP RPC(`Add`, `Delete`, `Operate`, `Set`, `Get`) 지원  
* `CommandExecutor.kt` : 단일 디스패처 (OTA, App 컨트롤, Stats 연결)  

---

## ⬜ 단계 11 ― **보조 (Companion) 앱 (선택 사항)**
* 별도 모듈 `:companion` 라이브러리화  
* `CompanionService` : 자동 실행, 상태/설정 API 제공  
* Agent ↔ Companion 간 `CommandBroadcastReceiver` 공통화  

---

## ⬜ 단계 12 ― **단위 테스트 & 기본 CI 구성**
* 각 모듈 단위 테스트  
  * `MqttManagerTest.kt` (재연결/TLS/토픽 매핑)  
  * `UspParserTest.kt` (Protobuf ↔ DTO)  
  * `CommandExecutorTest.kt` (명령 라우팅)  
* Mocking : `io.mockk:mockk:1.13.11`  
* GitHub Actions CI : 빌드 · 단위 테스트  

---

## ⬜ 단계 13 ― **통합 테스트(Integration) & 디버깅**
* `docker-compose` : EMQX + USP Test Harness 환경 구성  
* End‑to‑End 검증 : BOOT → PERIODIC → OTA → 앱 제어  
* Instrumented Test : `AgentService` 부팅 확인  
* GitHub CI : 통합 테스트 워크플로 `integration.yml`  

---

## ⬜ 단계 14 ― **시연 및 목업 준비**
* USP 메시지 예제·시나리오 기반 데모 환경 구축  
* 시연용 스크립트 `run_demo.sh`, 문서 `demo_scenario.md` 작성  

---

## ⬜ 단계 15 ― **하드닝 및 정식 릴리즈**
* TLS 필수 적용 (`network_security_config.xml`)  
* targetSdk 36 대응, R8 및 빌드 최적화  
* 벤더 플러그인 아키텍처 도입  
* CI 파이프라인으로 배포 자동화 (Play Console / OEM 포털)  

---

### 📌 향후 고려·보완 사항
* USP 모델 자동 코드 생성 (Proto → Kotlin)  
* 멀티 브로커 페일오버, 관리 UI (Electron) 구축  
* OpenTelemetry 분산 트레이싱 적용  
