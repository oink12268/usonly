# Infrastructure & CI/CD

> 2026-07-30 전면 재정리, **2026-08-07 갱신** (시크릿/`.env`, 파일 저장 구조, 채팅 암호화, Pinecone 등 이번에 직접 확인한 것 반영). 예전엔 k3s(Kubernetes) 기반이었으나 지금은 **k3s를 걷어내고 순수 Docker(단일 호스트)로 운영 중**. 아래 내용 중 "확인 필요"라고 표시된 항목은 아직 직접 확인 못 한 부분이니 다음에 볼 때 채워넣을 것.

## ⚡ 운영 핵심 요약 (급할 때 여기부터)

- **SSH**: `ssh homeserver` (계정 `suhwan`, 집 안에서는 이걸로만 됨 — 공인 DDNS는 hairpin NAT로 실패)
- **docker-compose / 시크릿**: `~/usonly/docker-compose.yml` + `~/usonly/.env` (서버에만 있음, git에 없음)
- **DB 접속**: `docker exec mysql mysql -uroot -p'rlatnghkS1@#' usonly` (컨테이너명 `mysql`, DB명 `usonly`)
- **로컬에서 운영 DB 붙기**: `ssh -f -N -L 3307:127.0.0.1:3306 homeserver` 로 터널 뚫고 `127.0.0.1:3307` 접속 (mysql은 3306이 호스트에 열려있음)
- **업로드 파일**: 서버 `/home/suhwan/usonly/uploads` → 컨테이너 `/home/ubuntu/uploads`. 커플별 폴더 `uploads/{coupleId}/`
- **로그**: `ssh homeserver 'docker logs usonly-app --since 30m'`
- **배포**: `main` 브랜치 push → GitHub Actions가 알아서 함 (수동 X)

## 🔐 시크릿 / 환경변수 (`~/usonly/.env`)

docker-compose가 `.env`에서 값을 읽어 `usonly-app` 컨테이너에 주입한다. **이 파일은 서버에만 있고 git에 없음.** 키:

| 키 | 용도 | 분실 시 영향 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | DB root (`rlatnghkS1@#`) | DB 접근 불가 |
| `GEMINI_API_KEY` | AI 채팅 검색·요약, 근무표 OCR | AI 기능 정지 |
| `PINECONE_API_KEY` / `PINECONE_INDEX_HOST` | 채팅 벡터 검색 (index host: `chat-messages-lfzp7zw.svc.aped-4627-b74a.pinecone.io`) | AI 검색 RAG 정지 |
| `CHAT_ENCRYPTION_KEY` | **채팅 메시지 AES-256-GCM 암호화 키** (base64 32바이트) | ⚠️ **분실하면 기존 채팅 전부 복호화 불가 = 영구 소실.** 절대 잃어버리면 안 됨. 별도 안전한 곳에 백업 필수 |
| `GRAFANA_TOKEN` | Alloy → Grafana Cloud 로그 전송 | 모니터링만 끊김 |

- Firebase 서비스계정 키는 `.env`가 아니라 파일 마운트: `~/usonly/firebase/serviceAccountKey.json` → 컨테이너 `/etc/firebase/serviceAccountKey.json` (프로젝트 `evho-2943a`)
- 외부 API 키(Gemini/Pinecone)는 **전 커플이 하나의 키를 공유** → 확장 시 rate limit/비용 공유 이슈 있음 (커플별 상한 없음)

## 📁 파일 저장 구조

- 마운트: 서버 `/home/suhwan/usonly/uploads` ↔ 컨테이너 `/home/ubuntu/uploads` (`application-prod.yml`의 `custom.file.dir=/home/ubuntu/uploads/`)
- **커플별 폴더 구조** (2026-08 도입):
  - `uploads/{coupleId}/` — 채팅 첨부, 앨범 사진 (+ `thumb_` 썸네일)
  - `uploads/{coupleId}/notes/` — 메모 이미지
  - `uploads/profiles/` — 프로필 사진 (커플 무관, 용량 한도 대상 아님)
- 서빙: `/images/**` 정적 핸들러 + Traefik. **인증 없음** — 파일명이 UUID라 추측 불가한 것에 의존
- **용량 한도**: 커플당 5GB (`custom.file.max-couple-bytes`). 초과 시 업로드 413. `couple.storage_used_bytes` 컬럼으로 추적
- 디스크: 총 264GB, 여유 ~203GB (2026-08 기준)
- **영상 업로드는 제거됨** (2026-08, 용량 문제). 사진만 지원. 기존 영상 파일/DB row는 남아있으나 앱에서 표시 안 됨

## 서버 구성 (물리 노트북 2대 → 1대로 축소됨)

원래 집 공유기 안에 노트북 2대(삼성, 샤오미)로 홈서버를 운영했는데, **삼성 노트북은 폐기**하고 지금은 **샤오미 노트북 1대만 운영 중**.

| | 상태 | 비고 |
|---|---|---|
| 삼성 노트북 (192.168.0.16, "Ubuntu PC") | **폐기됨** | k3s + Jenkins + ArgoCD가 올라가 있던 옛날 서버. 라우터에 남아있는 관련 포트포워딩 규칙(9090, 32759, 30080, 31306, 8088)은 전부 죽은 규칙 — 기기가 없으니 응답 자체가 없음(타임아웃). 삭제해도 됨 |
| 샤오미 노트북 (192.168.0.13, 호스트명 `five-kilo-server`) | **운영 중, 지금 유일한 홈서버** | usonly 포함 3개 프로젝트(five-kilo, usonly, hitup)가 이 한 대에 같이 떠 있음 |

## Server PC (192.168.0.13, five-kilo-server)

- **SSH 접속**: `ssh homeserver` (`~/.ssh/config`의 별칭, 계정은 `ubuntu`가 아니라 **`suhwan`**)
  - 공인 DDNS 주소(`usonly.duckdns.org` 등)로 직접 SSH 시도하면 집 안에서는 **hairpin NAT 때문에 "Network is unreachable"로 실패함** — 반드시 `homeserver` 별칭(내부 IP로 바로 연결) 사용할 것. 밖에서 접속할 땐 포트포워딩된 `five-kilo-ssh`(외부 2222 → 내부 22)나 DDNS 주소 사용
- **k3s/Jenkins/ArgoCD 전부 없음** — 순수 `docker` 컨테이너로 운영 (docker-compose 파일 위치: 확인 필요)
- **Docker 컨테이너 목록** (2026-07-30 `docker ps` 기준):

| 컨테이너 | 이미지 | 포트 | 비고 |
|---|---|---|---|
| `usonly-app` | `oink12268/usonly:latest` | 미공개 (Traefik 경유) | 이 프로젝트의 백엔드. 8주 전 생성, 6주째 Up |
| `mysql` | `mariadb:10.11` | `0.0.0.0:3306->3306` | **호스트에 3306으로 직접 노출됨.** 예전 k3s NodePort(31306) 얘기는 이제 무관 |
| `traefik` | `traefik:v2.11` | `0.0.0.0:80`, `0.0.0.0:443` | 리버스 프록시 + TLS 종료. ACME/인증서 갱신 방식은 확인 필요 (k8s cert-manager는 더 이상 안 씀) |
| `redis` | `redis:7-alpine` | 미공개 (내부망) | 채팅 pub/sub + Spring 캐시. pub/sub 채널은 **커플별** `chat:{type}:{coupleId}` (2026-08 격리 수정). 캐시: `member:providerId`, `member:coupleId`, `aiSearch::...` |
| `alloy` | `grafana/alloy:latest` | 미공개 | 신규 추가된 관측(observability) 에이전트. 예전 infra.md엔 없었음 |
| `osrm` | `ghcr.io/project-osrm/osrm-backend` | `0.0.0.0:5000` | usonly와 무관한 `five-kilo` 프로젝트(라우팅 엔진) |
| `syncsoul-backend` | `oink12268/syncsoul-backend:latest` | 미공개 | usonly와 무관한 별도 프로젝트 |

- MySQL 비밀번호: `rlatnghkS1@#` (`application-local.yml`과 동일), DB명 `usonly`. `docker inspect mysql`로 확인함
- docker-compose 파일 위치: `~/usonly/docker-compose.yml` (five-kilo-server, `deploy.yml`의 `cd ~/usonly` 참고)

## AWS EC2 (15.164.123.38) - Legacy

- 이번 정리 때 확인 안 함. 여전히 살아있는지 불명 — 다음에 확인 필요
- (기존 메모) Docker Compose (Spring Boot + MySQL), SSH key: `C:\usonly\my-key.pem`, user: `ubuntu`, docker-compose v1 syntax 필요

## DNS / DuckDNS

- **`usonly.duckdns.org` → `183.102.110.232`**: 정상, 지금 실제 운영 도메인. 내부적으로 `192.168.0.13`(five-kilo-server)로 포트포워딩됨
- **`five-kilo.duckdns.org`**: DuckDNS 계정 정리하다가 **실수로 삭제됨**. `deploy.yml`이 이 도메인을 참조하고 있어서 배포가 깨져 있었음 → **`usonly.duckdns.org`로 통일하는 쪽으로 수정 완료** (아래 CI/CD 참고). `~/.ssh/config`의 `homeserver` 별칭은 이미 `usonly.duckdns.org:2222`로 되어 있어서 안 건드림
- DuckDNS 계정에는 현재 `syncsoul`, `usonly` 두 도메인만 남아있음 (`five-kilo`는 복구 안 하기로 함)

## CI/CD Pipeline

~~GitHub Push → Jenkins (Build + Docker Image) → Docker Hub → Helm Chart tag update → ArgoCD Sync → k3s Pod~~ **← 더 이상 이 경로 아님. Jenkins/ArgoCD/k3s 자체가 없어짐.**

실제 배포는 `usonly/.github/workflows/deploy.yml` (GitHub Actions, `main` 브랜치 push 트리거):
```
GitHub Push (main) → Gradle bootJar → Docker Build & Push (oink12268/usonly:latest, :{run_number})
  → SSH로 five-kilo-server 접속 (usonly.duckdns.org:2222, user: suhwan) → docker compose pull/up usonly-app
```
- Docker Hub 로그인/서버 SSH 비밀번호는 GitHub Actions repo secrets(`DOCKERHUB_USERNAME`, `DOCKERHUB_PASSWORD`, `SERVER_PASSWORD`)로 관리됨
- `host:`가 삭제된 `five-kilo.duckdns.org`로 되어 있어서 깨져 있었던 걸 `usonly.duckdns.org`로 수정함 (2026-08-01)
- ⚠️ **`CHAT_ENCRYPTION_KEY`는 CI가 아니라 서버의 `.env`에만 있음.** 서버 재구축/이전 시 이 파일을 반드시 같이 옮겨야 채팅 복호화됨 (`docker-compose.yml`의 `environment:`에 `- CHAT_ENCRYPTION_KEY=${CHAT_ENCRYPTION_KEY}` 있음)

### 클라이언트 배포 (`usonly-client/.github/workflows/distribute.yml`)
`main` push → Flutter 빌드 → Firebase App Distribution (테스터: `oink12268@gmail.com`, `jinaoj@gmail.com`)
- **APK**: Firebase App Distribution 테스트 배포용
- **AAB**: Play Store 제출용 (2026-08 추가). 워크플로 아티팩트 `app-release-aab`로 업로드 → 수동으로 Play Console에 올림
- 릴리즈 서명: `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD` secret로 `release.jks` + `key.properties` 복원. 앱 패키지명 `com.evho.usonly` (namespace는 `com.example.usonly_client` 유지)
- Firebase 설정: `GOOGLE_SERVICES_JSON_B64`, `FIREBASE_OPTIONS_DART_B64`, `FIREBASE_APP_ID`, `FIREBASE_TOKEN` secret

### 앱 내 스케줄러 (usonly-app 컨테이너 안에서 도는 @Scheduled)
| 작업 | 주기 | 비고 |
|---|---|---|
| 일별 채팅 임베딩 | 매일 05:00 | 커플별로 어제 대화 요약→Pinecone |
| 임베딩 백필 | 매일 05:30 | 최근 30일 중 빠진 날짜 보완 |
| 고아 파일 정리 | 매주 일 03:00 | 현재 dry-run(로그만). ⚠️ findAll()로 전체 로드 — 커플 많아지면 개선 필요 |
| 쿠폰 만료 알림 | 매일 09:00 | |
| 기념일 알림 | 매일 09:00 | |
| 일정 리마인더 | 매시 정각 | 내일 일정 FCM |

### TODO — 다음에 처리할 것
1. 삼성 노트북(.16) 관련 죽은 포트포워딩 규칙 라우터에서 정리
2. Traefik의 TLS 인증서 갱신이 지금 실제로 어떻게 되고 있는지 확인 (k8s cert-manager는 없음)

## Docker Hub
- Image: oink12268/usonly
- ~~Jenkins에 dockerhub 크레덴셜 등록~~ (Jenkins 없음 — GitHub Actions repo secret `DOCKERHUB_USERNAME`/`DOCKERHUB_PASSWORD`로 확인됨)

## GitHub Repos
- Backend: github.com/oink12268/us-only
- ~~Helm: github.com/oink12268/usonly-helm~~ (k3s/Helm 안 쓰므로 더 이상 무관)

## Port Forwarding (iptime 공유기, ipTIME N604E)

**활성 (192.168.0.13, five-kilo-server 대상)**

| 규칙명 | 외부 포트 | 내부 포트 | 용도 |
|---|---|---|---|
| five-kilo-ssh | 2222 | 22 | SSH |
| five-kilo-osrm | 5000 | 5000 | OSRM (무관 프로젝트) |
| usonly | 80 | 80 | Traefik HTTP |
| usonly-https | 443 | 443 | Traefik HTTPS |
| hitup-http | 80 | 80 | 무관 프로젝트 |
| hitup-https | 443 | 443 | 무관 프로젝트 |

**죽은 규칙 (192.168.0.16, 삭제된 삼성 노트북 대상 — 정리 대상)**

| 규칙명 | 포트 | 비고 |
|---|---|---|
| Ubuntu PC (SSH) | 22 | 기기 없음 |
| jenkins | 9090 | 기기 없음 |
| argocd | 32759 | 기기 없음 |
| k3s-app | 30080 | 기기 없음 |
| k3s-mysql | 31306 | 기기 없음. `application-local.yml`이 아직 이 주소(`usonly.iptime.org:31306`)를 가리키고 있어서 로컬 개발 시 연결 안 됨 — `192.168.0.13:3306`으로 수정 필요 |
| agency-web... | 8088 | 기기 없음 |

## 버튼 클릭 → 백엔드 요청 네트워크 플로우

### REST API 요청

```
[1] 사용자가 앱에서 버튼 클릭
        │
        ▼
[2] Flutter (Dio)
    - HTTPS 요청 생성
    - 예: POST https://usonly.duckdns.org/api/couples/schedule
        │
        ▼
[3] DNS 조회
    - usonly.duckdns.org → 집 공유기 외부 IP (183.102.110.232)
    - DuckDNS 서버가 응답 (cron으로 5분마다 최신 IP 유지)
        │
        ▼
[4] TCP 연결 (3-way handshake)
    - 목적지: 외부IP:443
        │
        ▼
[5] iptime 공유기 (포트 포워딩)
    - 외부 :443 → 내부 192.168.0.13:443 으로 패킷 전달 (five-kilo-server)
        │
        ▼
[6] Traefik 컨테이너 (TLS 핸드셰이크)
    - 인증서 제시 → 암호화 터널 수립 (갱신 방식 확인 필요)
        │
        ▼
[7] Traefik (TLS 종료 + 라우팅)
    - HTTPS 복호화 → 평문 HTTP로 변환
    - Host 헤더(usonly.duckdns.org) 보고 라우팅
    - → usonly-app 컨테이너로 프록시 (도커 내부망, 외부에 포트 미공개)
        │
        ▼
[8] Spring Boot (usonly-app 컨테이너)
    - 요청 처리 (인증 필터 → 컨트롤러 → 서비스 → 레포지토리)
        │
        ▼
[9] MySQL (mariadb 컨테이너, 3306)
    - 쿼리 실행, 결과 반환
        │
        ▼
[10] 응답이 역순으로 Flutter까지 전달
     MySQL → Spring Boot → Traefik(재암호화) → 공유기 → 앱
```

### WebSocket 연결 (채팅)

```
[1] Flutter가 WSS 연결 시도
    - wss://usonly.duckdns.org/ws/...
        │
        ▼
[2~6] DNS → 공유기 → Traefik 까지는 REST와 동일
        │
        ▼
[7] Traefik
    - HTTP Upgrade 헤더 감지 → WebSocket 업그레이드 처리
    - wss:// → ws:// (내부 평문 WebSocket) 으로 변환
    - usonly-app 컨테이너로 연결 유지 (롱커넥션)
        │
        ▼
[8] Spring Boot (STOMP over WebSocket)
    - /ws 엔드포인트에서 STOMP 핸드셰이크
    - 이후 SUBSCRIBE/SEND 메시지 실시간 양방향 통신
```

### 구간별 프로토콜

| 구간 | 프로토콜 | 비고 |
|------|---------|------|
| 앱 ↔ DuckDNS | DNS (UDP 53) | 도메인 → IP 변환 |
| 앱 ↔ 공유기 외부 | HTTPS/WSS (TCP 443) | 암호화 |
| 공유기 ↔ Traefik | HTTPS/WSS (TCP 443) | 포트 포워딩 |
| Traefik ↔ Spring Boot | HTTP/WS | 도커 내부 네트워크, TLS 종료 후 평문 |
| Spring Boot ↔ MySQL | MySQL 프로토콜 (TCP 3306) | 같은 호스트, 도커 내부망 |

- **Spring Boot는 외부에서 직접 접근 불가** → 반드시 Traefik 경유
- **같은 공유기 내부에서 공인 DDNS 주소로 테스트 불가** → hairpin NAT 미지원. HTTPS는 모바일 데이터로, SSH 등 내부 접속은 `192.168.0.13` 내부 IP(또는 `homeserver` alias)로 우회

## Key Commands (Docker 기준으로 전면 교체)
```bash
docker ps                          # 컨테이너 상태
docker logs <container>            # 로그 확인
docker logs -f <container>         # 실시간 로그
docker inspect <container>         # 환경변수/설정 확인
docker exec -it <container> sh     # 컨테이너 진입
docker compose -f <경로> logs -f <서비스명>   # docker-compose 기반 서비스 로그 (다른 프로젝트 예시: hitup-tennis)
```
~~kubectl 관련 명령어 전부 무의미 (k3s 없음)~~

## Troubleshooting
- SSH/HTTPS 접속 시 "Network is unreachable" (집 안에서 공인 DDNS로 접속 시도): hairpin NAT 문제 → 내부 IP(`192.168.0.13`) 또는 `homeserver` SSH 별칭 사용, 외부에서는 모바일 데이터로 테스트
- `192.168.0.16` 관련 아무 포트도 응답 없음: 그 서버(삼성 노트북) 자체가 폐기됨. 정상. `.13`으로 접속할 것
- 로컬 개발 시 DB 연결 안 됨: `application-local.yml`이 아직 죽은 주소(`usonly.iptime.org:31306`)를 가리키고 있음 — `192.168.0.13:3306` (같은 공유기 내부에서) 또는 SSH 터널로 접속
- HTTPS 정상 여부 확인: 모바일에서 Whitelabel Error Page 뜨면 정상 (Spring Boot까지 도달한 것)
- DuckDNS IP 자동갱신: crontab에 `*/5 * * * * curl -s "https://www.duckdns.org/update?domains=usonly&token=TOKEN&ip="` 등록되어 있을 것 (five-kilo용 crontab 항목은 도메인 삭제로 인해 무의미해졌을 수 있음 — 확인 필요)
- ~~Jenkins/ArgoCD/k3s 관련 트러블슈팅 전부 삭제~~ (해당 시스템 자체가 없어짐)
