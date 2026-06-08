# 인증 및 회원 관리 (Security & Portal) 작업 진행 보고서

* **작업자**: 이제훈 (팀장)
* **작업 일시**: 2026.06.08
* **목표**: 큐티켓 프로젝트의 보안 기반(JWT/Security)을 다지고, 화이트/블루 테마의 회원가입/로그인/마이페이지 UI 화면을 HTML/CSS 분리 기술로 구현합니다.

---

## 1. 백엔드 및 보안 아키텍처 작업 내역

### 1.1 JWT 및 시큐리티 필터 최적화
* **SecurityContext Principal 최적화**: 컨트롤러 단에서 `@AuthenticationPrincipal String userId`로 현재 로그인한 사용자의 UUID를 간단하게 활용할 수 있도록, `JwtAuthenticationFilter`에서 인증 객체 생성 시 `userId` 단일 문자열을 Principal로 저장하도록 개선했습니다.
* **JWT 검증 예외 전파**: 기존에 모든 예외를 삼키던 `JwtTokenProvider.validateToken` 메서드가 예외를 상위로 던지도록 수정했습니다.
* **상태별 에러 코드 연동**: `JwtAuthenticationFilter`에서 토큰 만료 시 `TOKEN_EXPIRED(A003)`, 위조/손상 시 `TOKEN_INVALID(A004)` 에러 코드를 Request 속성에 바인딩하여, `JwtAuthenticationEntryPoint`를 통해 사용자에게 커스텀 JSON 응답을 명확히 내려주도록 연동을 맞췄습니다.
* **SecurityConfig 뷰 페이지 개방**: `/login`, `/signup`, `/mypage` 뷰 라우트 및 `/css/**`, `/js/**` 정적 리소스에 대해 비인증 사용자 접근이 가능하도록 `permitAll()`을 설정했습니다.

### 1.2 회원가입(Signup) API 구현
* **SignupRequest DTO**: 이메일 형식 검증 및 비밀번호 길이 제한(4자~20자) 등의 validation 조건이 포함된 DTO를 구현했습니다.
* **중복 가입 방지 및 비밀번호 암호화**: `AuthService.signup`에서 이메일 중복 체크를 수행하여 중복 시 `BusinessException`을 발생시키고, 패스워드는 `PasswordEncoder(BCrypt)`를 통해 암호화하여 DB에 안전하게 반영하도록 구현했습니다.

### 1.3 마이페이지 API 및 에러 처리 고도화
* **유저 프로필 조회 예외 처리**: `UserController.getMe` API에서 사용자를 찾지 못할 경우 날것의 `NoSuchElementException` 대신 정의된 공통 에러인 `BusinessException(ErrorCode.USER_NOT_FOUND)`를 반환하도록 안전하게 매핑했습니다.
* **익명 사용자 조회 방어**: `SecurityUtils.getCurrentUserId()` 호출 시 비인가 익명 사용자일 경우 `"anonymousUser"` 문자열 대신 안전하게 `null`을 반환하도록 예외 방어 처리를 추가했습니다.

---

## 2. 프론트엔드 및 UI/UX 작업 내역 (화이트/블루 테마)

### 2.1 CSS 및 HTML 템플릿 분리
Thymeleaf 템플릿 내에 CSS를 매립하지 않고, 스프링 부트의 정적 서빙 경로에 맞춘 별도의 외부 CSS 구조를 확립했습니다.

* **CSS 파일**: `src/main/resources/static/css/auth.css`
  * 깔끔하고 세련된 화이트 및 블루(#2563eb, #3b82f6) 톤의 테마 스타일링.
  * 입력 박스 포커스 효과, 그라디언트 버튼 호버 모션, 에러 알림창 스타일 등 수록.
* **HTML 템플릿 파일**: `src/main/resources/templates/`
  * **login.html**: 이메일/비밀번호 입력 및 Ajax를 통한 JWT 발급 처리. 성공 시 `localStorage`에 토큰을 저장하고 메인으로 이동.
  * **signup.html**: 비밀번호 일치 검사 후 가입 완료 시 1.5초 뒤 로그인 창으로 자동 이동.
  * **mypage.html (Page Guard)**: 자바스크립트 가드가 내장되어 토큰이 없으면 즉시 로그인창으로 튕겨냄. 토큰이 있을 시 `/api/v1/users/me`를 비동기 호출하여 사용자 상세 정보를 그리드 형태로 렌더링. 로그아웃 시 토큰 폐기 처리.

---

## 3. 수정 및 생성된 파일 목록

* **Auth 관련 파일**:
  * [AuthPageController.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/auth/controller/AuthPageController.java) (추가)
  * [SignupRequest.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/auth/dto/SignupRequest.java) (추가)
  * [AuthService.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/auth/service/AuthService.java) (수정)
  * [AuthController.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/auth/controller/AuthController.java) (수정)
* **User 관련 파일**:
  * [UserController.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/user/controller/UserController.java) (수정)
* **Security 관련 파일**:
  * [SecurityConfig.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/global/config/SecurityConfig.java) (수정)
  * [JwtTokenProvider.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/global/security/JwtTokenProvider.java) (수정)
  * [JwtAuthenticationFilter.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/global/security/JwtAuthenticationFilter.java) (수정)
  * [SecurityUtils.java](file:///C:/CE/team5/team5-ticket-app/src/main/java/com/example/ticketing/global/security/SecurityUtils.java) (수정)
* **UI/UX 템플릿 및 리소스 파일**:
  * [auth.css](file:///C:/CE/team5/team5-ticket-app/src/main/resources/static/css/auth.css) (추가)
  * [login.html](file:///C:/CE/team5/team5-ticket-app/src/main/resources/templates/login.html) (추가)
  * [signup.html](file:///C:/CE/team5/team5-ticket-app/src/main/resources/templates/signup.html) (추가)
  * [mypage.html](file:///C:/CE/team5/team5-ticket-app/src/main/resources/templates/mypage.html) (추가)
