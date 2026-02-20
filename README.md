# US-ONLY

커플 전용 컴패니언 앱 백엔드 서버

## 기술 스택

- Java 17 / Spring Boot 3.5.9
- MySQL 8.0
- Firebase Cloud Storage
- WebSocket (STOMP)
- Docker

## 시작하기

### 사전 준비

- JDK 17
- MySQL 8.0 (로컬 개발 시)
- Firebase 서비스 계정 키 (`src/main/resources/serviceAccountKey.json`)

### 로컬 실행

```bash
# 빌드
./gradlew clean build -x test

# 실행 (application-local.yml 프로필 사용)
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

### Docker 실행

`docker-compose.yml`에서 `MYSQL_ROOT_PASSWORD`와 `SPRING_DATASOURCE_PASSWORD`를 수정한 후:

```bash
docker-compose up --build
```

## 주요 기능

| 기능 | 설명 |
|------|------|
| 소셜 로그인 | 카카오, 구글 로그인/회원가입 |
| 커플 연결 | 초대 코드로 두 사용자 연결 |
| 실시간 채팅 | WebSocket/STOMP 기반 메시징 |
| 앨범/아카이브 | 사진·영상 업로드 및 앨범 관리 |

## API

### 인증
- `POST /api/auth/login` — 소셜 로그인

### 커플
- `POST /api/couples/connect` — 초대 코드로 커플 연결

### 채팅
- `GET /api/chats` — 채팅 내역 조회
- WebSocket 연결: `/ws`
- 메시지 발행: `/pub/chat` / 구독: `/sub/chat`

### 아카이브
- `POST /api/archives/create` — 앨범 생성
- `POST /api/archives/upload` — 미디어 업로드
- `GET /api/archives/albums?userId={id}` — 앨범 목록
- `GET /api/archives/{albumId}?userId={id}` — 앨범 상세

## 프로젝트 구조

```
src/main/java/com/evho/usonly/
├── domain/
│   ├── member/       # 회원 인증 및 프로필
│   ├── couple/       # 커플 연결 관리
│   ├── chat/         # 실시간 채팅
│   └── archive/      # 앨범·미디어 관리
└── global/
    ├── config/       # WebSocket, CORS, 리소스 설정
    ├── file/         # Firebase 파일 스토리지
    └── utils/        # 유틸리티 (초대 코드 생성 등)
```

각 도메인은 `controller/`, `service/`, `model/`, `repository/`, `dto/` 하위 패키지로 구성됩니다.
