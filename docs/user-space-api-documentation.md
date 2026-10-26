# 지능형 북마크 웹 서비스
## 유저·스페이스 API 문서

> 유저 인증·프로필 및 개인/팀 스페이스 기능의 API 계약 문서입니다.
>
> 기준: 현재 프로젝트 코드, API 명세서, 확정된 권한 정책

---

## 1. 담당 범위

```text
유저
├── 회원가입
├── 로그인
└── 개인정보 수정

스페이스
├── 스페이스 생성
├── 스페이스 목록 조회
├── 스페이스 상세 조회
├── 스페이스 삭제
├── 팀원 초대
├── 팀원 목록 조회
└── 팀원 삭제
```

---

## 2. 공통 규칙

### Base URL

로컬 개발 환경:

```text
http://localhost:8080
```

### 인증 헤더

보호된 API는 로그인 후 발급받은 `accessToken`을 사용합니다.

```http
Authorization: Bearer {accessToken}
```

Swagger의 `Authorize` 입력창에는 프로젝트 설정상 `accessToken`만 입력합니다.

```text
올바른 입력: eyJhbGciOiJIUzI1NiJ9...
잘못된 입력: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 공통 응답 형식

```json
{
  "status": 200,
  "success": true,
  "message": "success message",
  "data": {}
}
```

### 주요 식별자

| 이름 | 의미 |
|---|---|
| `userId` | `User` 엔티티의 사용자 ID |
| `spaceId` | `Space` 엔티티의 스페이스 ID |
| `memberId` | `SpaceMember` 엔티티의 멤버십 ID |

> 팀원 삭제 API에는 `userId`가 아니라 `memberId`를 전달해야 합니다.

---

## 3. 권한 정책

| 기능 | 비로그인 | 비멤버 | MEMBER | OWNER |
|---|---:|---:|---:|---:|
| 스페이스 생성 | 불가 | - | 가능 | 가능 |
| 스페이스 목록 조회 | 불가 | - | 가능 | 가능 |
| 스페이스 상세 조회 | 불가 | 불가 | 가능 | 가능 |
| 스페이스 삭제 | 불가 | 불가 | 불가 | 가능 |
| 팀원 초대 | 불가 | 불가 | 불가 | TEAM만 가능 |
| 팀원 목록 조회 | 불가 | 불가 | 가능 | 가능 |
| 팀원 삭제 | 불가 | 불가 | 불가 | MEMBER 대상만 가능 |
| MEMBER 자기 탈퇴 | 불가 | - | 지원하지 않음 | - |

추가 정책:

- `PERSONAL` 스페이스에는 팀원을 초대할 수 없습니다.
- 팀원 초대는 `TEAM` 스페이스에서만 가능합니다.
- OWNER는 삭제 대상이 될 수 없습니다.
- 스페이스 삭제 시 해당 스페이스의 북마크와 북마크 태그, 멤버 정보를 DB에서 함께 영구 삭제합니다.

---

# 4. 유저 API

## 4.1 회원가입

```http
POST /api/auth/register
```

인증: 불필요

### Request Body

```json
{
  "email": "user@test.com",
  "password": "pw1234!",
  "name": "사용자"
}
```

### 성공 응답

```text
201 Created
```

```json
{
  "status": 201,
  "success": true,
  "message": "register success",
  "data": {
    "userId": 1,
    "email": "user@test.com"
  }
}
```

### 실패 응답

| 상황 | 상태 코드 |
|---|---:|
| 필수값 누락 또는 형식 오류 | 400 |
| 이미 등록된 이메일 | 409 |

비밀번호는 `PasswordEncoder`를 통해 암호화되어 저장되며 API 응답에 포함되지 않습니다.

---

## 4.2 로그인

```http
POST /api/auth/login
```

인증: 불필요

### Request Body

```json
{
  "email": "user@test.com",
  "password": "pw1234!"
}
```

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "login success",
  "data": {
    "accessToken": "JWT_ACCESS_TOKEN",
    "refreshToken": "JWT_REFRESH_TOKEN",
    "userId": 1,
    "nickname": "사용자"
  }
}
```

### 실패 응답

| 상황 | 상태 코드 |
|---|---:|
| 이메일 또는 비밀번호 불일치 | 401 |
| 필수값 누락 | 400 |

보호된 API에는 `accessToken`을 사용합니다.

---

## 4.3 개인정보 수정

```http
PUT /api/users/profile
```

인증: 필요

### Request Body

```json
{
  "name": "수정된 사용자",
  "profileImage": "https://example.com/profile.png"
}
```

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "profile updated successfully",
  "data": {
    "userId": 1,
    "nickname": "수정된 사용자",
    "updatedAt": "2026-08-30T18:20:31"
  }
}
```

`userId`는 Request Body로 받지 않으며, JWT의 현재 로그인 사용자 ID를 수정 대상으로 사용합니다.

---

# 5. 스페이스 API

## 5.1 스페이스 생성

```http
POST /api/spaces
```

인증: 필요

### Request Body

```json
{
  "spaceName": "캡스톤 프로젝트",
  "description": "팀 프로젝트 자료를 관리하는 공간",
  "type": "TEAM"
}
```

`type` 허용값:

```text
PERSONAL
TEAM
```

### 성공 응답

```text
201 Created
```

```json
{
  "status": 201,
  "success": true,
  "message": "space created successfully",
  "data": {
    "spaceId": 1,
    "spaceName": "캡스톤 프로젝트",
    "description": "팀 프로젝트 자료를 관리하는 공간",
    "type": "TEAM"
  }
}
```

생성자는 자동으로 `Space.owner`가 되며, `SpaceMember`에 `OWNER` 역할로 등록됩니다.

---

## 5.2 스페이스 목록 조회

```http
GET /api/spaces
```

인증: 필요

로그인한 사용자가 생성했거나 참여 중인 스페이스만 조회합니다.

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "space list retrieved successfully",
  "data": [
    {
      "spaceId": 1,
      "spaceName": "캡스톤 프로젝트",
      "type": "TEAM",
      "bookmarkCount": 0
    }
  ]
}
```

---

## 5.3 스페이스 상세 조회

```http
GET /api/spaces/{spaceId}
```

인증: 필요

권한: 해당 스페이스의 `OWNER` 또는 `MEMBER`만 가능

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "space retrieved successfully",
  "data": {
    "spaceId": 1,
    "spaceName": "캡스톤 프로젝트",
    "description": "팀 프로젝트 자료를 관리하는 공간",
    "type": "TEAM",
    "ownerId": 1,
    "memberCount": 2,
    "bookmarkCount": 0,
    "createdAt": "2026-08-30T18:20:31"
  }
}
```

---

## 5.4 스페이스 삭제

```http
DELETE /api/spaces/{spaceId}
```

인증: 필요

권한: `OWNER`만 가능

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "space deleted successfully",
  "data": true
}
```

`MEMBER`, 비멤버 및 비로그인 사용자는 삭제할 수 없습니다.

삭제 범위: 해당 스페이스의 `bookmarks` 행(주소, 제목, 요약, 방문 정보 포함),
그 북마크의 `bookmark_tags` 행, `space_members` 행과 `spaces` 행을 함께 영구 삭제합니다.
사용자 계정과 다른 스페이스의 북마크·태그·멤버 정보는 유지됩니다.
삭제는 하나의 트랜잭션에서 처리하므로 DB 삭제 중 오류가 발생하면 전체 삭제가 롤백됩니다.

---

# 6. 팀원 API

## 6.1 팀원 초대

```http
POST /api/spaces/{spaceId}/invite
```

인증: 필요

권한: `TEAM` 스페이스의 `OWNER`만 가능

### Request Body

```json
{
  "email": "member@test.com"
}
```

### 성공 응답

```text
201 Created
```

```json
{
  "status": 201,
  "success": true,
  "message": "member invited successfully",
  "data": {
    "memberId": 2,
    "email": "member@test.com"
  }
}
```

### 검증 순서

1. 요청자가 해당 스페이스의 OWNER인지 확인합니다.
2. 스페이스 타입이 TEAM인지 확인합니다.
3. 이메일에 해당하는 User가 존재하는지 확인합니다.
4. 이미 가입된 사용자인지 확인합니다.
5. `MEMBER` 역할로 등록합니다.

### 실패 응답

| 상황 | 상태 코드 |
|---|---:|
| MEMBER 또는 비멤버가 초대 | 403 |
| PERSONAL 스페이스에 초대 | 403 |
| 존재하지 않는 사용자 | 404 |
| 이미 가입된 사용자 | 409 |

---

## 6.2 팀원 목록 조회

```http
GET /api/spaces/{spaceId}/members
```

인증: 필요

권한: 해당 스페이스의 `OWNER` 또는 `MEMBER`

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "member list retrieved successfully",
  "data": [
    {
      "memberId": 1,
      "userId": 1,
      "nickname": "owner",
      "email": "owner@test.com",
      "role": "OWNER"
    },
    {
      "memberId": 2,
      "userId": 2,
      "nickname": "member",
      "email": "member@test.com",
      "role": "MEMBER"
    }
  ]
}
```

---

## 6.3 팀원 삭제

```http
DELETE /api/spaces/{spaceId}/members/{memberId}
```

인증: 필요

권한: `OWNER`만 가능하며, 대상은 `MEMBER`만 가능

### 성공 응답

```text
200 OK
```

```json
{
  "status": 200,
  "success": true,
  "message": "member removed successfully",
  "data": true
}
```

### 허용되는 경우

```text
요청자: OWNER
대상: MEMBER
→ 삭제 가능
```

### 차단되는 경우

```text
요청자: MEMBER
→ 삭제 불가

요청자: 비멤버
→ 삭제 불가

요청자: OWNER
대상: OWNER
→ 삭제 불가

요청자: MEMBER
대상: 자기 자신
→ 삭제 불가
```

### 주의

다음 응답을 사용하면 안 됩니다.

```http
DELETE /api/spaces/{spaceId}/members/{userId}
```

`memberId`는 반드시 `GET /api/spaces/{spaceId}/members` 응답의 `memberId`를 사용합니다.

---

# 7. 오류 상태 코드

| 상태 코드 | 의미 | 예시 |
|---:|---|---|
| 400 | 잘못된 요청 | 필수값 누락, 잘못된 type |
| 401 | 인증 실패 | 토큰 없음, 잘못된 토큰, 잘못된 로그인 |
| 403 | 권한 없음 | MEMBER의 삭제·초대, 비멤버 조회 |
| 404 | 리소스 없음 | 없는 spaceId, userId, memberId |
| 409 | 충돌 | 중복 이메일, 중복 멤버 초대 |
| 500 | 서버 오류 | 처리되지 않은 예외 |

공통 오류 응답 예시:

```json
{
  "status": 403,
  "success": false,
  "message": "only owner can remove members"
}
```

---

# 8. Swagger 통합 테스트 순서

1. `POST /api/auth/register`로 OWNER 계정을 생성합니다.
2. MEMBER와 비멤버 테스트 계정을 생성합니다.
3. `POST /api/auth/login`으로 OWNER 로그인합니다.
4. 응답의 `accessToken`을 복사합니다.
5. Swagger `Authorize`에 토큰만 입력합니다.
6. `POST /api/spaces`로 TEAM 스페이스를 생성합니다.
7. 응답의 `spaceId`를 기록합니다.
8. `POST /api/spaces/{spaceId}/invite`로 MEMBER를 초대합니다.
9. `GET /api/spaces/{spaceId}/members`로 `memberId`를 확인합니다.
10. MEMBER 계정으로 로그인 후 상세·목록 조회를 확인합니다.
11. MEMBER 권한으로 삭제·초대 요청이 차단되는지 확인합니다.
12. OWNER 권한으로 MEMBER 삭제를 확인합니다.
13. OWNER 삭제 시도와 존재하지 않는 `memberId`를 확인합니다.
14. Swagger에서 Logout 후 토큰 없는 요청을 확인합니다.

> `memberId`와 `userId`를 혼동하지 않도록 주의합니다.

---

# 9. 프론트엔드·AI 협업 체크리스트

## 프론트엔드 전달 사항

- 보호 API에는 `Authorization: Bearer {accessToken}`이 필요합니다.
- Swagger Authorize에는 accessToken만 입력합니다.
- 로그인 사용자 ID는 Request Body로 임의 전달하지 않습니다.
- `spaceId`, `userId`, `memberId`의 의미를 구분합니다.
- OWNER와 MEMBER의 권한이 다릅니다.
- PERSONAL 스페이스에는 팀원을 초대할 수 없습니다.
- 오류 응답은 `status`, `success`, `message` 형식으로 처리합니다.

## AI·북마크 담당자 협의 사항

- AI 요청에서 `userId`와 `spaceId`를 어떻게 전달할지 결정합니다.
- 팀 스페이스의 북마크 접근 권한을 합의합니다.
- 스페이스 삭제 시 해당 북마크와 태그가 DB에서 영구 삭제됨을 공유합니다.
- 요약 결과가 개인 데이터인지 팀 스페이스 데이터인지 결정합니다.

---

