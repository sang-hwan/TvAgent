# Android TV Agent ― **백그라운드 지식 & README**

*(최종 동기화 : 2025‑07‑18)*

> **본 문서 목적**
> Android TV Agent 프로젝트를 **쉽게 온보딩·유지보수·확장**할 수 있도록
> 필수 배경지식과 현재 설계 상태를 한눈에 볼 수 있게 정리한 README입니다.
> 변경 상세 내역은 별도 `plan_history.md` 파일에 유지하고,
> 이 README에는 **“현재 기준 사실”** 만 서술합니다. 

---

## 목차

- [Android TV Agent ― **백그라운드 지식 \& README**](#androidtvagent--백그라운드-지식--readme)
  - [목차](#목차)
  - [프로젝트 개요](#프로젝트-개요)
  - [아키텍처](#아키텍처)
  - [통신 스택](#통신-스택)
  - [모듈 구조](#모듈-구조)
  - [주요 클래스 \& 데이터 흐름](#주요-클래스--데이터-흐름)
  - [개발 환경](#개발-환경)
  - [Android 플랫폼 포인트](#android-플랫폼-포인트)
  - [빠른 시작 (로컬 설치 \& 부트스트랩)](#빠른-시작로컬-설치--부트스트랩)
    - [신규 설치 \& 1차 부트스트랩 확인](#신규-설치1차-부트스트랩-확인)
    - [앱 업데이트 흐름 (PACKAGE\_REPLACED)](#앱-업데이트-흐름-package_replaced)
    - [에뮬레이터 재부팅 흐름 (BOOT\_COMPLETED)](#에뮬레이터-재부팅-흐름-boot_completed)
  - [USP ↔ TR‑069 용어집](#usp--tr069-용어집)
  - [테스트 \& 데모](#테스트--데모)
  - [유지보수 체크리스트](#유지보수-체크리스트)
  - [README 업데이트 방법](#readme-업데이트-방법)

---

## 프로젝트 개요

| 항목          | 내용                                                                                                |
| ----------- | ------------------------------------------------------------------------------------------------- |
| **최종 목표**   | Android TV 단말(CPE)을 **MQTT 기반 TR‑369(USP)** 로 모니터링·제어. TR‑069 코드는 *legacy* 로만 보존하며 런타임에서 완전 비활성화. |
| **현재 단계**   | **5단계** — MQTT/USP 핵심 기능 완료, TR‑069 클래스 `@Disabled("legacy")` 처리.                                 |
| **개발용 브로커** | `mqtts://aromit.iptime.org:8883` (EMQX 5.7, TLS)                                                  |
| **상태 아이콘**  | ✅ 완료  🚧 진행  ⬜ 예정                                                                                 |

---

## 아키텍처

```
ACS / Controller
       ▲
       │  (MQTT QoS2 + TLS)
       ▼
┌───────────────────────────┐
│  MQTT Broker (EMQX 5.7)   │
└───────────────────────────┘
       ▲          ▲
       │ USP Msg  │ OTA
       ▼          ▼
┌───────────────────────────┐
│ Android TV Agent (앱)      │
│  ├─ :agent‑core           │
│  ├─ :network‑mqtt         │
│  ├─ :protocol‑usp         │
│  └─ :protocol‑tr069‑legacy│ «컴파일 전용» │
└───────────────────────────┘
       ▲
       │ Binder / Intent
       ▼
 (선택) Companion App
```

*Agent 는 **Foreground Service** 로 MQTT 세션을 유지하며, TR‑069 모듈은 DI 바인딩에서 제거되어 빌드만 수행합니다.*

---

## 통신 스택

| 계층  | **TR‑369 (USP)**                   | TR‑069 (CWMP – Legacy) |
| --- | ---------------------------------- | ---------------------- |
| 전송  | **MQTT 5.0 + TLS**                 | HTTP(S) + SOAP/XML     |
| 메시지 | USP Record (`Inform`, `Operate` …) | CWMP RPC               |
| 상태  | **주요 구현 대상**                       | @Disabled              |

**MQTT 토픽**

| 목적    | 패턴                               |
| ----- | -------------------------------- |
| 상태 보고 | `usp/agent/{deviceUuid}/inform`  |
| 명령 수신 | `usp/agent/{deviceUuid}/command` |

> `MqttManager`는 연결 직후 `command` 토픽을 자동 구독합니다. 

---

## 모듈 구조

| 모듈                         | 책임                         |   |
| -------------------------- | -------------------------- | - |
| **:agent‑core**            | 서비스 라이프사이클, DI 루트, 공통 로깅   |   |
| **:network‑mqtt**          | `MqttManager`, 재연결, TLS 설정 |   |
| **:protocol‑usp**          | USP 스키마·파서, 세션 로직          |   |
| **:protocol‑tr069‑legacy** | `Tr069*` 클래스 → **컴파일만**    |   |
| **:companion** (옵션)        | 보조 앱 / 단말 측 인터페이스          |   |

---

## 주요 클래스 & 데이터 흐름

| 클래스                        | 기능                                 |
| -------------------------- | ---------------------------------- |
| **`MqttManager`**          | TLS 연결·재연결, QoS2 세션 관리             |
| **`UspSessionUseCase`**    | BOOT·Periodic Inform, OTA/앱 제어 진입점 |
| **`UspMessageDispatcher`** | MQTT 수신 → 명령 라우팅                   |
| **`UspTr069Converter`**    | TR‑069 ↔ USP 매핑(호환 테스트)            |
| **`DeviceStatsProvider`**  | CPU·RAM·스토리지 수집                    |
| **`OtaManager`**           | 펌웨어 다운로드→검증→설치                     |

**데이터 흐름 예시 (BOOT Inform)**

1. `AgentService` 기동 → `Config.initDeviceUuid()` 로 UUID 확보
2. `UspSessionUseCase.sendBootInform()` 호출
3. `MqttManager.publish()` 로 *inform* 토픽 송신
4. ACS 응답(*command*) → `UspMessageDispatcher` 에서 처리 

---

## 개발 환경

| 항목     | 값                                              |
| ------ | ---------------------------------------------- |
| IDE    | Android Studio **Meerkat FD** 2024.3.2 Patch 1 |
| 언어     | Kotlin · Java 21 (CI sanity : Java 11)         |
| Gradle | 8.14.2 (Kotlin DSL)                            |
| SDK    | minSdk 29 / targetSdk 36                       |
| 난독화    | R8 + ProGuard                                  |

> 전역 상수는 `gradle.properties`, 모듈별 설정은 각 `build.gradle.kts`. Gradle Sync 오류 시 **Invalidate Caches / Restart** → **Rebuild** 순서로 해결. 

---

## Android 플랫폼 포인트

* **Foreground Service** – MQTT 세션 유지, 낮은 우선순위 알림 표시
* **BroadcastReceiver** – `BOOT_COMPLETED` 수신 → Agent 자동 기동
* **코루틴 Dispatcher**

  * `IO` : 네트워크·디스크
  * `Default` : 파싱·계산
  * `Main` : (보조 앱) UI 콜백 

---

## 빠른 시작 (로컬 설치 & 부트스트랩)

로컬 Android Emulator(또는 실제 단말)에 *tvagent* 앱을 배포하고 **BOOT → PACKAGE\_REPLACED → BOOT\_COMPLETED** 시나리오를 검증할 때 사용하는 **ADB one‑liner** 모음입니다.
아래 명령은 모두 `adb` 가 `$PATH` 에 잡혀 있고, 대상 기기(에뮬레이터)와 `adb devices` 로 연결이 확인된 상태를 전제로 합니다.

### 신규 설치 & 1차 부트스트랩 확인

```bash
adb uninstall kr.co.aromit.tvagent
adb install path/to/app-debug.apk
adb shell am start-foreground-service \
  -n kr.co.aromit.tvagent/.service.AgentService
```

> *처음 설치 후* `AgentService` 를 Foreground Service 로 직접 기동하여 **BOOTSTRAP Inform** 시퀀스를 확인합니다.

### 앱 업데이트 흐름 (PACKAGE\_REPLACED)

```bash
adb install -r path/to/app-debug.apk
```

> `-r` 플래그는 *APK 교체 설치* 로 **PACKAGE\_REPLACED** 브로드캐스트를 트리거합니다.
> `AgentService` 가 자동 재시작되고, USP Inform 타입이 *BOOT* → *PERIODIC* 으로 전환되는지 확인합니다.

### 에뮬레이터 재부팅 흐름 (BOOT\_COMPLETED)

```bash
adb reboot
```

> 에뮬레이터 재시작 후 `BOOT_COMPLETED` 브로드캐스트를 통해 `BroadcastReceiver` 가 정상적으로 `AgentService` 를 재기동하는지 검증합니다.

---

## USP ↔ TR‑069 용어집

| 용어                | 정의                                       |   |
| ----------------- | ---------------------------------------- | - |
| **USP (TR‑369)**  | CWMP 후속 프로토콜, 바이너리 프레임·전송 독립적            |   |
| **CWMP / TR‑069** | SOAP‑over‑HTTP 원격 관리 프로토콜                |   |
| **ACS**           | Auto Configuration Server                |   |
| **CPE**           | Customer Premises Equipment (Android TV) |   |
| **Inform**        | CPE → ACS 상태 보고                          |   |

---

## 테스트 & 데모

1. **단위 테스트** — `./gradlew :*testDebug` *(MockK 기반)*
2. **통합 테스트** — `docker-compose up` *(EMQX + USP Harness)* → `./gradlew integrationTest`
3. **데모 스크립트** — `scripts/run_demo.sh` 실행 → **BOOT → OTA** 시퀀스 자동 시연

---

## 유지보수 체크리스트

* **TLS 의무화** : `network_security_config.xml` 로 clear‑text 차단
* **R8 난독화** : legacy 클래스 제외, USP 관련 클래스 *keep*
* **targetSdk 36 대응** : Foreground Service·권한 정책 최신화 모니터링
* **멀티‑모듈 플러그인 관리** : `/plugins` JAR + `ServiceLoader` 구조 유지
* **프로토콜 확장** : 추후 **TR‑369 over QUIC/WebSocket** 지원 대비 `:network‑mqtt` 추상화

---

## README 업데이트 방법

1. **작은 수정** : 해당 항목만 직접 고친 뒤 커밋
2. **새 기능** : 적절한 위치에 `### 새 기능명` 헤더 추가 + *추가 날짜* 주석
3. **폐기 기능** : <s>취소선</s> 처리 후 사유 서술
4. **새 용어** : “USP ↔ TR‑069 용어집” 가나다순으로 추가
5. **대규모 리팩터링** : “아키텍처” 및 관련 표 갱신, 상세 변경 내역은 `plan_history.md` 기록

---
