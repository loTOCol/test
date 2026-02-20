# WalkingMate

> 나의 일상 속 워킹 메이트  
> 한이음 드림업(ICT 멘토링) 프로젝트

WalkingMate는 걷기 기록, 메이트 모집, 피드, 채팅을 한 앱에서 제공하는 Android 서비스입니다.

## 주요 기능
- 산책 기록(거리/시간/경로)
- 메이트 모집 글 작성 및 신청/수락 관리
- 피드 업로드 및 조회
- 1:1 및 그룹 채팅
- 지도 기반 경로 표시
- 음악 추천/재생 기능

## 화면 및 기능 설명
### 인증
한 줄 요약: 로그인/회원가입과 네이버 간편 로그인을 통해 빠르게 앱에 진입합니다.

| 로그인 화면 | 회원가입 화면 |
|---|---|
| <img src="./assets/screens/login-screen.jpg" width="180" /> | <img src="./assets/screens/signup-screen.jpg" width="180" /> |

### 메인 내비게이션
한 줄 요약: 홈, 게시판, 캘린더, 채팅/랭킹을 하단 탭으로 이동하며 핵심 기능을 탐색합니다.

| 워킹 홈 대시보드 | 메이트 게시판 목록 | 기록 캘린더 | 채팅 탭 | 랭킹 탭 |
|---|---|---|---|---|
| <img src="./assets/screens/screen01.jpg" width="170" /> | <img src="./assets/screens/screen02.jpg" width="170" /> | <img src="./assets/screens/screen03.jpg" width="170" /> | <img src="./assets/screens/screen04.jpg" width="170" /> | <img src="./assets/screens/screen05.jpg" width="170" /> |

### 트래킹/메이트
한 줄 요약: 실시간 산책 기록, 종료 처리, 메이트 신청/신청자 관리를 한 흐름으로 제공합니다.

| 산책 트래킹 지도 | 산책 종료 다이얼로그 | 메이트 게시글 상세 | 피드 목록 | 신청 목록 관리 |
|---|---|---|---|---|
| <img src="./assets/screens/screen06.jpg" width="170" /> | <img src="./assets/screens/screen07.jpg" width="170" /> | <img src="./assets/screens/screen08.jpg" width="170" /> | <img src="./assets/screens/screen09.jpg" width="170" /> | <img src="./assets/screens/screen10.jpg" width="170" /> |

### 채팅/프로필
한 줄 요약: 1:1 채팅과 참가자 확인, 사용자 프로필 확인 및 상호작용을 지원합니다.

| 1:1 채팅 화면 | 채팅 참가자 목록 | 사용자 프로필 |
|---|---|---|
| <img src="./assets/screens/screen11.jpg" width="180" /> | <img src="./assets/screens/screen12.jpg" width="180" /> | <img src="./assets/screens/screen13.jpg" width="180" /> |

### 음악 추천/템포 워킹
한 줄 요약: 음악 유사도 추천과 BPM/템포 기반 재생으로 걷기 페이스를 조절합니다.

| 목표 걸음 설정 | 음악 추천 메인 | 추천 상세 분석 | 인터벌 트레이닝 |
|---|---|---|---|
| <img src="./assets/screens/screen14.jpg" width="170" /> | <img src="./assets/screens/screen15.jpg" width="170" /> | <img src="./assets/screens/screen16.jpg" width="170" /> | <img src="./assets/screens/screen17.jpg" width="170" /> |

| LOW 템포 재생 | MAX 템포 재생 |
|---|---|
| <img src="./assets/screens/screen18.jpg" width="170" /> | <img src="./assets/screens/screen21.jpg" width="170" /> |

## 기술 스택
- Language: Java
- Platform: Android
- Build: Gradle
- Backend/DB: Firebase (Firestore, Realtime Database, Storage)
- Map: Naver Maps SDK
- Network: Retrofit, Volley

## 프로젝트 구조
```text
WalkingMate/
  app/
  assets/
    screens/
  gradle/
  build.gradle
  settings.gradle
  gradlew
```
