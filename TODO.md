# TODO / 공부할 것들

## 캐싱
- [ ] **Redis 캐싱 공부 및 적용**
  - 현재 `CurrentMemberArgumentResolver`에서 `ConcurrentHashMap`으로 `firebaseUid → memberId` 인메모리 캐싱 중
  - 실서비스/스케일아웃 시 Redis로 전환 필요
  - Spring `@Cacheable` + `RedisCacheManager` + TTL 설정 방식 공부
  - 관련 파일: `global/resolver/CurrentMemberArgumentResolver.java`
