**Android TV Agent ― 종합 Sprint 설계안 (2025‑Q3 ~ Q4)**
*(모든 업로드 파일·문서·코드 분석 결과)*  

---

## 1 . 현재 품질선(Cut‑line) 진단

| 영역               | 확인 사항                                                                            | 증거 |
| ---------------- | -------------------------------------------------------------------------------- | -- |
| **앱·서비스 기동**     |  `BootReceiver`가 부팅/설치 이벤트를 구분해 `AgentService` Foreground Service를 자동 기동         |    |
| **TR‑069 동작 여부** |  `TR069SessionUseCase`가 여전히 호출되고 있어 *런타임에서 TR‑069 사이클이 실행*                       |    |
| **MQTT 스택**      |  `MqttManager`·`MqttTopics` 뼈대 완비, TLS·QoS2·자동 재연결 로직 일부 구현                      |    |
| **USP/Protobuf** |  `UspProtoConverter` 초안만 존재, `UspSessionUseCase`·Dispatcher 부재                   |    |
| **설정·보안**        |  `Config` 상에 clear‑text HTTP ACS·MQTT 설정 + `network_security_config.xml`으로 임시 허용 |    |
| **플랫폼 Hook**     |  Manifest 권한 및 Foreground Service 선언 정상                                          |    |
| **계획·문서화**       |  단계 5 진행 중(모듈 분리·TR‑069 비활성화 미완료)로 README/Plan에 명시                               |    |

**결론** : *“USP 전용 Agent”* 로 전환하려면 **① 모듈 분리 → ② TR‑069 런타임 제거 → ③ USP 세션 완성** 이 가장 급합니다.

---

## 2 . 백로그 재정렬 (Epic → Story)

| Epic ID                           | 목표                                                                                 | 핵심 Story (Done = 검수 기준)                                                         |
| --------------------------------- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| **E1** 모듈 분리 & Legacy 차단          | 코드베이스를 `:agent‑core / :network‑mqtt / :protocol‑usp / :protocol‑tr069‑legacy` 로 분리 | *GRADLE* 멀티모듈 빌드 성공 & `enableTr069=false` 로 실행 시 TR‑069 클래스가 **선언만** 컴파일되는 것 확인 |
| **E2** MQTT + USP Core            | 기동‑종료‑재연결, Boot/Periodic Inform, Command 토픽 수신                                     | Dev EMQX → Agent 간 BOOT Inform Round‑trip(log 확인)                               |
| **E3** Periodic Stats             | CPU·RAM·Storage 수집 후 USP Inform                                                    | EMQX Record에 Stats JSON/Proto 반영                                                |
| **E4** OTA (Download/Install)     | USP Download, 검증, Silent Install                                                   | OTA 이미지 SHA256 검증 & 설치 로그 수집                                                    |
| **E5** TV App Control & Companion |  앱 목록 보고, Intent 원격 제어, 보조앱 통신                                                     |  Controller에서 앱 실행/종료 RPC 성공률 95 % 이상                                           |
| **E6** Command Executor & Tests   | USP RPC (Add/Delete/Operate/Set/Get) 매트릭스 처리 + 단위·통합 테스트                           | 80 %+ 코드 커버리지 & docker‑compose E2E 통과                                           |
| **E7** 하드닝 & 배포                   | R8 난독화, TLS only, targetSdk 36                                                     | Play Console Pre‑launch report 0 critical                                       |

---

## 3 . 6‑주 Sprint 로드맵 (2‑주 타임‑박스 × 3)

### ◇ Sprint 1 ( 7/15 – 7/28 )

| #               | Story / Task                                                            | 담당     | 예상 SP |
| --------------- | ----------------------------------------------------------------------- | ------ | ----- |
| 1               | Gradle 멀티모듈 스켈레톤 (`settings.gradle.kts`, `buildSrc` 버전 catalog 공통화)     | BE     | 3     |
| 2               | `:protocol‑tr069‑legacy` 이동 + `@Disabled("legacy")` 어노 & DI 바인딩 제거      | BE     | 3     |
| 3               | `MqttManager.connect()` TLS 옵션·QoS2 Clean Session=F 구현 (재연결 지수‑Backoff) | BE     | 5     |
| 4               | `Config.initDeviceUuid()` → SharedPref 저장 & Manifest Provider Auto‑init | BE     | 2     |
| 5               | `UspSessionUseCase`‑Skeleton : Boot Inform Protobuf 직렬화 → `publish()`   | BE     | 5     |
| 6               | CI 파이프라인 (Java 11 sanity + Java 21)·Static‑Analyser(Detekt 1.23)        | DevOps | 3     |
| **Velocity 목표** | **~21 SP / 2 주**                                                       |        |       |

**Sprint‑1 Definition‑of‑Done** : `./gradlew assembleDebug` 성공 시 APK 설치 → MQTT Broker로 **BOOT Inform 1건 발행**되고 TR‑069 로그가 더 이상 찍히지 않아야 한다.

---

### ◇ Sprint 2 ( 7/29 – 8/11 )

| #               | Story / Task                                                     | 예상 SP |
| --------------- | ---------------------------------------------------------------- | ----- |
| 7               | `UspMessageDispatcher` : `command` 토픽 수신 → Flow/Channel emit     | 5     |
| 8               | `UspTr069Converter` (양방향) → 호환 테스트 스텁 작성                         | 3     |
| 9               | `DeviceStatsProvider` + `DeviceStatsMapper` → Periodic Ticker 접목 | 5     |
| 10              | `MqttManagerTest`, `UspParserTest` (MockK + Turbine)             | 5     |
| 11              | `network_security_config.xml` clear‑text 제거, TLS 테스트             | 2     |
| **Velocity 목표** | **~20 SP / 2 주**                                                |       |

*Done 조건* : PERIODIC Inform 이 5 분 주기로 Broker에 적재되고, Dispatcher가 Echo Command(Test) 를 받고 로그에 출력.

---

### ◇ Sprint 3 ( 8/12 – 8/25 )

| #               | Story / Task                                           | 예상 SP |
| --------------- | ------------------------------------------------------ | ----- |
| 12              | `OtaManager` : 다운로드‑검증‑Install 워크플로 + `UspOtaCommand`  | 8     |
| 13              | `TvAppController` + `IntentCommand` (앱 목록/제어)          | 5     |
| 14              | **:companion** 모듈 scaffold → Binder/Broadcast 테스트      | 5     |
| 15              | `CommandExecutor` matrix & 통합 테스트(docker‑compose EMQX) | 5     |
| 16              | R8 keep 규칙·타깃 SDK 36 Foreground 제한 점검                  | 3     |
| **Velocity 목표** | **~26 SP / 2 주**                                      |       |

*Done 조건* : OTA .bin 업로드 → Agent 가 설치 후 재부팅 Flag 보고, 원격 Intent 로 YouTube 앱 구동 성공. Play‑Install checkpass.

---

## 4 . 공통 Definition of Ready

1. **Protobuf IDL** 확정 & 버전 태그
2. Acceptance Criteria 명시 (Request/Response 예제 포함)
3. 로그 태그·에러 처리 정책 정의 (`Timber.tag/E/w`)
4. Story Point ≤ 8 (초과 시 쪼갬)

---

## 5 . 리스크 & 완화책

| 리스크                              | 영향                   | 대응                                                          |
| -------------------------------- | -------------------- | ----------------------------------------------------------- |
| **TR‑069 의존 코드 제거 중 사이드‑이펙트**    | 빌드 실패·런타임 NPE        |  먼저 *모듈 이동만* 하고 DI 바인딩을 단계적으로 교체 (feature flag)             |
| MQTT QoS2 대용량 OTA 전송 지연          | 업그레이드 실패             | 1 GB 이상 파일은 HTTPS pre‑signed URL fallback 옵션 설계             |
| Android 14 Foreground Service 정책 | Service Kill → 연결 끊김 | WorkManager + FGS restart token 적용, targetSdk 36 대응 Task 확보 |
| EMQX 버전 갱신 시 호환성                 | 메시지 삭제, ACL 변경       | docker‑compose 로 현재 EMQX 5.7 이미지 고정, 차기 버전 nightly 테스트      |

---

## 6 . 역할·책임 (RACI)

| 영역                       | Responsible |  Consulted / Informed |
| ------------------------ | ----------- | --------------------- |
| **Core Kotlin 개발**       | 내부 BE 2명    | Android Lead          |
| **MQTT Broker & DevOps** | DevOps 1명   | BE, QA                |
| **OTA 이미지 & 인증**         | FW 팀        | BE                    |
| **테스트 스크립트**             | QA 1명       | DevOps, BE            |
| **문서·데모 시나리오**           | PM (사용자 본인) | 모든 팀                  |

---

## 7 . 다음 단계 액션(오늘‑내일)

1. **Git main → develop 브랜치 분기** 후 *Sprint‑1 branch rule* 등록
2. `gradle.properties` → `enableTr069=false` 초기 커밋
3. EMQX Dev Broker TLS Cert 갱신 & 팀 공유
4. JIRA 백로그에 Epic/Story 입력 & Story Point Poker 세션 예약

---

### 🎯 핵심 메시지

*“**Sprint‑1 목표는 ‘TR‑069 로그가 완전히 사라진 상태에서 MQTT BOOT Inform 1건을 성공적으로 보내는 것’**입니다. 이 기준을 달성하면 이후 모든 기능은 USP 트랙 안에서 안전하게 확장‑검증‑배포할 수 있습니다.”*

---

*(본 설계안은 업로드된 모든 소스·문서 검토 후 작성되었습니다. 향후 변경 시 `plan_history.md` 에 diff 로그를 남겨 주세요.)*
