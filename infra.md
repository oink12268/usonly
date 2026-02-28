# Infrastructure & CI/CD

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
    - usonly.duckdns.org → 집 공유기 외부 IP
    - DuckDNS 서버가 응답 (cron으로 5분마다 최신 IP 유지)
        │
        ▼
[4] TCP 연결 (3-way handshake)
    - 목적지: 외부IP:443
        │
        ▼
[5] iptime 공유기 (포트 포워딩)
    - 외부 :443 → 내부 192.168.0.16:443 으로 패킷 전달
        │
        ▼
[6] Traefik (TLS 핸드셰이크)
    - Let's Encrypt 인증서 제시 → 암호화 터널 수립
        │
        ▼
[7] Traefik (TLS 종료 + 라우팅)
    - HTTPS 복호화 → 평문 HTTP로 변환
    - Host 헤더(usonly.duckdns.org) 보고 Ingress 규칙 매칭
    - → usonly-service:8080 으로 프록시 (ClusterIP, 외부 접근 불가)
        │
        ▼
[8] Spring Boot Pod
    - 요청 처리 (인증 필터 → 컨트롤러 → 서비스 → 레포지토리)
        │
        ▼
[9] MySQL Pod
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
    - usonly-service:8080 으로 연결 유지 (롱커넥션)
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
| Traefik ↔ Spring Boot | HTTP/WS (TCP 8080) | TLS 종료 후 내부 통신 |
| Spring Boot ↔ MySQL | MySQL 프로토콜 (TCP 3306) | DB 쿼리 |

- **Spring Boot는 외부에서 직접 접근 불가** → 반드시 Traefik 경유
- **같은 공유기 내부에서 테스트 불가** → hairpin NAT 미지원, 모바일 데이터로 테스트

---

## CI/CD Pipeline
```
GitHub Push → Jenkins (Build + Docker Image) → Docker Hub → Helm Chart tag update → ArgoCD Sync → k3s Pod
```

## Server PC (192.168.0.16) - Ubuntu 24.04
- AMD A6-9220, RAM 4GB + Swap 5.3GB
- k3s (lightweight Kubernetes)
- ArgoCD: https://192.168.0.16:32759 (admin)
- Jenkins: http://192.168.0.16:9090 (Docker container, k3s 바깥)
- ~~App NodePort: 30080~~ → Ingress + HTTPS로 전환
- DDNS: ~~usonly.iptime.org~~ → usonly.duckdns.org (iptime은 CAA 제한으로 Let's Encrypt 불가)
- SSH: ubuntu@192.168.0.16

## AWS EC2 (15.164.123.38) - Legacy
- Docker Compose (Spring Boot + MySQL)
- SSH key: C:\usonly\my-key.pem, user: ubuntu
- docker-compose v1 syntax required

## Docker Hub
- Image: oink12268/usonly
- Credentials registered in Jenkins as 'dockerhub'

## GitHub Repos
- Backend: github.com/oink12268/us-only
- Helm: github.com/oink12268/usonly-helm
- Credentials registered in Jenkins as 'github'

## Port Forwarding (iptime router)
- 22 → SSH
- 80 → Traefik (Let's Encrypt HTTP-01 challenge)
- 443 → Traefik (HTTPS)
- 9090 → Jenkins
- ~~30080 → k3s Spring Boot app~~ (Ingress로 대체)
- 32759 → ArgoCD

## k3s 내부 구조

```
Server PC (192.168.0.16)
├── Jenkins (Docker standalone, port 9090)  ← k3s 바깥
└── k3s Kubernetes
    ├── default namespace
    │   ├── usonly-app Pod       (Spring Boot, ClusterIP:8080)
    │   ├── mysql Pod            (MySQL 8.0)
    │   └── usonly-ingress       (Traefik → usonly-service:8080, TLS)
    ├── cert-manager namespace
    │   └── cert-manager Pods   (Let's Encrypt 자동 갱신)
    └── argocd namespace
        └── ArgoCD Pods          (NodePort 32759)
```

- Jenkins가 빌드 후 Docker Hub에 push → Helm Chart 태그 업데이트 → ArgoCD가 감지해서 k3s에 배포

## Kubernetes Secrets

| Secret 이름    | 내용                          | 마운트 위치             |
|---------------|-------------------------------|------------------------|
| mysql-secret  | MySQL password                | env: SPRING_DATASOURCE_PASSWORD |
| firebase-key  | Firebase + GCP service account JSON | /etc/firebase/serviceAccountKey.json |

- firebase-key는 Firebase Auth/Messaging + GCP Vision API 인증에 함께 사용
- `GOOGLE_APPLICATION_CREDENTIALS=/etc/firebase/serviceAccountKey.json` 으로 GCP SDK 인증

## Key Commands
```bash
sudo kubectl get pods                    # Pod 상태 (default namespace)
sudo kubectl get pods -A                 # 전체 네임스페이스 Pod 목록
sudo kubectl get pods -o wide            # 상세 정보 (IP, 노드 포함)
sudo kubectl logs <pod>                  # Pod 로그
sudo kubectl get svc                     # 서비스 확인
sudo kubectl get applications -n argocd  # ArgoCD 앱
sudo kubectl get certificate             # TLS 인증서 상태 확인
sudo kubectl get ingress                 # Ingress 확인
docker exec -u root jenkins chmod 666 /var/run/docker.sock  # 서버 재시작 후 필요
```

## HTTPS 구성
- cert-manager + Let's Encrypt + Traefik Ingress 조합
- ClusterIssuer: letsencrypt-prod (HTTP-01 challenge)
- 인증서 자동갱신: 만료 30일 전 cert-manager가 자동 처리
- Flutter: https://, wss:// 사용 (포트 없이 기본 443)

## Troubleshooting
- Jenkins 'docker not found': docker exec -u root jenkins으로 Docker CLI 설치
- Jenkins docker permission denied: chmod 666 /var/run/docker.sock
- ArgoCD CRD error: kubectl apply 다시 실행 후 rollout restart
- Spring Boot Error on first deploy: MySQL Pod보다 먼저 뜨면 발생, 자동 재시작으로 해결됨
- SSH에서 YAML 붙여넣기 실패: sudo tee /tmp/파일명.yaml로 파일 생성 후 kubectl apply -f
- Let's Encrypt CAA 오류: iptime.org 도메인은 CAA 제한으로 인증서 발급 불가 → DuckDNS 사용
- DuckDNS IP 자동갱신: crontab에 `*/5 * * * * curl -s "https://www.duckdns.org/update?domains=usonly&token=TOKEN&ip="` 등록
- HTTPS 접속 시 ERR_CONNECTION_REFUSED: 같은 공유기(내부망)에서 테스트하면 hairpin NAT 문제로 실패함 → 모바일 데이터로 테스트
- HTTPS 정상 여부 확인: 모바일에서 Whitelabel Error Page 뜨면 정상 (Spring Boot까지 도달한 것)
- http://usonly.iptime.org:30080 더 이상 안됨: NodePort→ClusterIP 전환으로 30080 포트 사라짐 (의도된 변화)
