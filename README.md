# Kakao-style Chat Service (chatapp)

## 빠른 실행 (로컬 Docker)
```powershell
docker compose -p chatapp up --build
# frontend: http://localhost:3000 / backend: http://localhost:8080 / MySQL: localhost:3307
```

## EC2 배포
`docs/배포.md` 참고. `docker-compose.prod.yml` + `.env` 사용.

## 기본 계정
user1/1234, user2/1234, user3/1234. 오픈방 초대코드: OPEN-001
