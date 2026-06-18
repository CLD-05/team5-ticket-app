# team5-ticket-app

동시성 상황을 고려한 공연 티켓 예매 서비스입니다.  
사용자는 공연을 조회하고, 대기열을 통과한 뒤 좌석을 선택하고 예매를 요청할 수 있습니다.

이 서비스는 티켓 오픈 순간처럼 많은 사용자가 동시에 몰리는 상황에서 **공정한 진입 순서**, **좌석 중복 선택 방지**, **안정적인 예매 처리**를 제공하는 것을 목표로 합니다.

## 서비스 소개

`team5-ticket-app`은 공연 티켓 예매 플랫폼입니다.

일반적인 예매 서비스처럼 공연 조회, 좌석 선택, 예매 기능을 제공하는 것에서 더 나아가, 실제 티켓팅 서비스에서 자주 발생하는 문제를 해결하는 데 초점을 둡니다.

- 티켓 오픈 순간의 대규모 동시 접속
- 동일 좌석에 대한 중복 선택
- 예매 요청 폭주로 인한 DB 부하
- 사용자에게 공정한 예매 진입 순서 제공

## 핵심 기능

### 회원 기능

- 회원가입
- 로그인
- JWT 기반 인증
- 마이페이지
- 예매 내역 조회

### 공연 기능

- 공연 목록 조회
- 공연 상세 조회
- 공연별 좌석 정보 조회

### 대기열 기능

공연별 대기열을 통해 사용자의 좌석 선택 진입 순서를 제어합니다.

```
사용자 접속
  ↓
대기열 진입
  ↓
대기 순번 확인
  ↓
입장 가능 여부 확인
  ↓
좌석 선택 페이지 진입
```

### 좌석 선점 (Seat Hold)

사용자가 좌석을 선택하면 일정 시간 동안 해당 좌석을 임시 선점(HOLD) 상태로 유지합니다.

```text
좌석 선택
    ↓
좌석 HOLD
    ↓
제한 시간 내 예매 요청
    ↓
예매 성공 또는 HOLD 만료
```

좌석 임시 선점은 여러 사용자가 동일한 좌석을 동시에 선택하는 상황을 줄이기 위한 1차 방어 장치입니다.

---

### 비동기 예매 처리

예매 요청은 즉시 최종 저장하지 않고 메시지 기반 비동기 처리로 진행됩니다.

```text
예매 요청
    ↓
요청 접수
    ↓
처리중 상태 반환
    ↓
Worker 예매 처리
    ↓
예매 성공/실패 저장
```

트래픽이 집중되는 상황에서도 요청을 안정적으로 처리할 수 있도록 설계했습니다.

---

### 동시성 제어

중복 예매를 방지하기 위해 여러 단계의 방어 전략을 적용했습니다.

#### 1차 방어

* Redis 기반 공연 대기열
* 공연별 입장 인원 제어

#### 2차 방어

* 좌석 임시 선점(HOLD)
* 동일 좌석 동시 선택 충돌 완화

#### 3차 방어

* DB 트랜잭션
* Unique Constraint
* 최종 예매 정합성 보장

Redis와 분산 락은 충돌을 줄이는 역할을 수행하며, 최종 데이터 정합성은 데이터베이스에서 보장합니다.

---

## 기술 스택

| 구분                    | 기술                                     |
| --------------------- | -------------------------------------- |
| Language              | Java 17                                |
| Framework             | Spring Boot 3.2.3                      |
| Template Engine       | Thymeleaf                              |
| Security              | Spring Security, JWT                   |
| Database              | MySQL                                  |
| ORM                   | Spring Data JPA                        |
| Cache / Queue Control | Redis                                  |
| Distributed Lock      | Redisson                               |
| Messaging             | AWS SQS, Spring Cloud AWS              |
| Monitoring            | Spring Actuator, Micrometer Prometheus |
| Build Tool            | Maven                                  |
| Container             | Docker                                 |

---

## 프로젝트 구조

```text
team5-ticket-app
├── src
│   ├── main
│   │   ├── java/com/example/ticketing
│   │   │   ├── auth
│   │   │   ├── booking
│   │   │   ├── queue
│   │   │   ├── seat
│   │   │   ├── show
│   │   │   ├── user
│   │   │   └── global
│   │   └── resources
│   │       ├── templates
│   │       ├── static
│   │       ├── application-local.yml
│   │       └── application-docker.yml
│   └── test
├── mysql-init
├── localstack-init
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 주요 패키지

| 패키지              | 설명                      |
| ---------------- | ----------------------- |
| auth             | 회원가입, 로그인, JWT 발급       |
| user             | 사용자 정보 관리               |
| show             | 공연 목록 및 상세 조회           |
| queue            | 공연별 대기열                 |
| seat             | 좌석 조회 및 임시 선점           |
| booking          | 예매 요청 및 비동기 처리          |
| global.config    | Redis, SQS, Security 설정 |
| global.security  | JWT 인증 및 대기열 검증         |
| global.exception | 공통 예외 처리                |

---

## 브랜치 전략

Git Flow 기반 브랜치 전략을 사용합니다.

| 브랜치        | 설명       |
| ---------- | -------- |
| main       | 운영 배포    |
| develop    | 개발 통합    |
| feature/*  | 기능 개발    |
| fix/*      | 버그 수정    |
| hotfix/*   | 운영 긴급 수정 |
| refactor/* | 리팩토링     |
| docs/*     | 문서 작업    |

예시

```text
feature/queue-system
feature/booking-worker
fix/seat-hold-expiration
docs/readme-update
```

---

## 커밋 컨벤션

형식

```text
type: subject
```

예시

```text
feat: 공연별 대기열 기능 추가
fix: 좌석 선점 만료 처리 오류 수정
refactor: 예매 처리 로직 분리
docs: README 문서 수정
```

| Type     | 설명         |
| -------- | ---------- |
| feat     | 기능 추가      |
| fix      | 버그 수정      |
| refactor | 리팩토링       |
| docs     | 문서 수정      |
| test     | 테스트        |
| chore    | 설정 및 기타 작업 |
| style    | 코드 스타일 수정  |
| perf     | 성능 개선      |

---

## Pull Request 규칙

PR은 develop 브랜치를 대상으로 생성합니다.

### 작업 내용

* 구현한 기능 또는 수정 내용

### 변경 사항

* 주요 변경 파일 및 변경 이유

### 테스트

* 실행한 테스트 및 검증 내용

### 참고 사항

* 리뷰어 참고 내용
* 후속 작업
* 영향 범위

예시

```text
feat: Redis 기반 공연 대기열 기능 추가
fix: SQS 예매 처리 실패 시 상태 저장 오류 수정
docs: README 서비스 문서 정리
```

---

## 코드 리뷰 기준

* 기능 요구사항 충족 여부
* 동시성 문제 발생 가능성
* 예외 처리 적절성
* 서비스 계층 책임 분리 여부
* 보안 정보 하드코딩 여부

---

## 협업 규칙

* 기능 개발은 별도 브랜치에서 진행
* 작업 전 develop 최신 내용 반영
* 하나의 PR에는 하나의 주요 기능만 포함
* 충돌 발생 시 담당자와 확인 후 반영
* Secret 및 Access Key 커밋 금지
* 동시성 관련 변경은 영향 범위를 PR에 명시

---

## 서비스 핵심 가치

* 공정한 대기열 관리
* 안정적인 좌석 선점
* 중복 예매 방지
* 비동기 예매 처리
* 트래픽 폭주 대응

본 프로젝트는 단순 예매 CRUD가 아닌, 실제 티켓팅 서비스에서 발생하는 동시성 문제를 해결하는 것을 목표로 합니다.
