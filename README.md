# WalkingMate

> 프로젝트명: 나의 일상 속 워킹 메이트  
> 프로그램: 한이음 드림업(ICT 멘토링)  
> 기간: 2024.04.01 ~ 2024.10.31

산책 기록, 메이트 모집, 피드, 채팅, 지도 기능을 하나의 앱에서 제공하는 Android 프로젝트입니다.

## 프로젝트 소개
WalkingMate는 일상 속 걷기 활동을 더 꾸준하고 즐겁게 만들기 위해 기획한 서비스입니다.  
사용자는 산책을 기록하고, 관심사가 맞는 메이트를 찾고, 피드와 채팅으로 소통할 수 있습니다.

## 데모
### 주요 화면
> 아래 파일 경로에 이미지 넣으면 README에 바로 표시됩니다.

![로그인 화면](./assets/screens/login.png)
![홈 화면](./assets/screens/home.png)
![메이트 모집 화면](./assets/screens/mate.png)
![산책 기록 화면](./assets/screens/walk-record.png)

### 주요 흐름 GIF
![로그인부터 메인 진입](./assets/gif/login-flow.gif)
![산책 기록 생성 흐름](./assets/gif/walk-flow.gif)

## 핵심 기능
- 일반 로그인 / 네이버 로그인
- 피드 작성 및 조회
- 메이트 모집 글 작성 / 신청
- 채팅
- 걷기 기록 및 지도 기반 위치 기능
- 일정 관리
- 음악 기능

## 기술 스택
- Language: Java
- Platform: Android
- Build: Gradle
- Backend/DB: Firebase (Firestore, Storage, Realtime Database)
- Map: Naver Maps SDK
- Network: Retrofit, Volley

## 프로젝트 구조
```text
WalkingMate/
  app/
  assets/
    screens/
    gif/
  gradle/
  scripts/
  build.gradle
  settings.gradle
  gradlew
```

## 실행 방법
1. Android Studio에서 프로젝트 열기
2. `local.properties` 생성 또는 `local.properties.example` 복사
3. API 키/시크릿 값 입력
4. 에뮬레이터 또는 실기기 연결
5. `app` 모듈 실행

## local.properties 예시
```properties
NAVER_OAUTH_CLIENT_ID=YOUR_CLIENT_ID
NAVER_OAUTH_CLIENT_SECRET=YOUR_CLIENT_SECRET
NAVER_OAUTH_CLIENT_NAME=WalkingMate
NAVER_MAP_CLIENT_ID=YOUR_MAP_CLIENT_ID
OPEN_WEATHER_API_KEY=YOUR_OPEN_WEATHER_API_KEY
NAVER_SEARCH_API_KEY_ID=YOUR_NAVER_SEARCH_API_KEY_ID
NAVER_SEARCH_API_KEY=YOUR_NAVER_SEARCH_API_KEY
```

## 개선 및 안정화 작업
- 패키지 구조 정리 (`mate` -> `mate` 명칭 정리 포함)
- 게시판 화면 중첩 이슈 수정
- 자동 로그인 동작 안정화
- 비밀번호 저장 방식 개선 (해시 기반 검증 + 기존 계정 마이그레이션)
- 메인 레이아웃 일부 디바이스 대응 보정

## 향후 계획
- 기능별 화면 캡처 및 데모 GIF 보강
- 테스트 코드 보강
- 사용자 온보딩 UX 개선

## GitHub About 추천 문구
- Description: 나의 일상 속 워킹 메이트 - 산책 기록, 메이트 모집, 피드, 채팅을 지원하는 Android 앱
- Topics: android, java, firebase, naver-maps, walking, social, gradle
