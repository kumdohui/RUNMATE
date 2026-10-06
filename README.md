# RUNMATE

RUNMATE는 러닝 기록, 코스 공유, 커뮤니티와 AI 러닝 상담 기능을 제공하는 웹 서비스입니다.

## 주요 기능

- 회원가입, 로그인 및 소셜 로그인
- 러닝 기록과 코스가 포함된 게시글 작성
- 게시글 검색, 필터, 댓글, 좋아요, 북마크와 알림
- 프로필 및 관리자 기능
- Gemini 기반 러닝 전문 챗봇과 대화 기록 관리

## 프로젝트 구성

```text
RUNMATE/
├─ runmate-client/   Nuxt 3 + Vue 3 프론트엔드
└─ runmate-server/   Spring Boot + MyBatis 백엔드
```

외부 서비스와 저장소는 다음과 같이 사용합니다.

- MySQL: 회원, 게시글, 러닝 기록 및 챗봇 대화 저장
- Redis: 로그인 세션 저장
- AWS S3: 프로필 및 게시글 이미지 저장
- Gemini API: 러닝 챗봇 답변 생성

## 실행 환경

- Java 17 이상
- Node.js 20 이상
- MySQL 8
- Redis 7

## 백엔드 환경변수

`runmate-server` 폴더에 `.env` 파일을 만들고 다음 항목을 설정합니다.

```dotenv
DB_URL=jdbc:mysql://localhost:3307/community_db?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=Asia/Seoul&forceConnectionTimeZoneToSession=true
DB_USERNAME=
DB_PASSWORD=

REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=

KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
KAKAO_REDIRECT_URI=

NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
NAVER_REDIRECT_URI=

MAIL_USERNAME=
MAIL_PASSWORD=

AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=
AWS_S3_BUCKET=

GOOGLE_API_KEY=
GEMINI_MODEL=gemini-3.1-flash-lite

SESSION_COOKIE_SECURE=false
```

실제 비밀번호와 API 키가 들어 있는 `.env` 파일은 Git에 커밋하지 않습니다.

## 데이터베이스 준비

MySQL에 데이터베이스를 생성합니다.

```sql
CREATE DATABASE community_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
```

기존 덤프 파일이 있다면 테이블 구조와 데이터를 복원합니다.

```powershell
mysql -u 사용자명 -p community_db < community_db.sql
```

챗봇 테이블만 별도로 생성해야 한다면 `runmate-server/sql/chatbot-schema.sql`을 실행합니다.

## 백엔드 실행

```powershell
cd runmate-server
.\mvnw.cmd spring-boot:run
```

백엔드는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 프론트엔드 실행

```powershell
cd runmate-client
npm install
npm run dev
```

프론트엔드는 기본적으로 `http://localhost:5173`에서 실행됩니다. 개발 중 `/api` 요청은 Nuxt 프록시를 통해 `http://localhost:8080`으로 전달됩니다.

## 주요 화면

- 홈: `/`
- 회원가입: `/signup`
- 로그인: `/login`
- 게시글 검색: `/courses`
- 챗봇: `/chat`
- 관리자: `/admin`

## 운영 구성

- 애플리케이션 서버: Nginx, Nuxt 정적 파일, Spring Boot
- 데이터 서버: MySQL, Redis

운영 서버 주소, 계정, 비밀번호와 API 키는 소스에 작성하지 않고 서버 환경변수로 관리합니다.
