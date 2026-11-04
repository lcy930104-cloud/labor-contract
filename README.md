# 전자근로계약서 (근로계약서 앱)

사업주가 계약 내용을 입력해 링크를 만들고, 근로자가 카카오톡으로 받은 링크에서 서명해 회신하는 전자근로계약서입니다.

이 저장소 하나로 두 가지가 만들어집니다.

| 무엇 | 어디서 | 누가 쓰나 |
|---|---|---|
| 웹페이지 (`docs/`) | GitHub Pages: `https://아이디.github.io/저장소이름/` | 근로자(링크 열어 서명), 사업주(브라우저·홈 화면 앱) |
| 안드로이드 앱 APK (`app/`) | Actions 탭 / Releases 탭 | 사업주 |

## 처음 설정 (한 번만)

1. **GitHub Pages 켜기**: Settings → Pages → Build and deployment → Source: *Deploy from a branch* → Branch: `main`, 폴더: `/docs` → Save.
   1~2분 뒤 `https://아이디.github.io/저장소이름/` 에서 페이지가 열립니다.
2. **서명 키 등록 (권장)**: Settings → Secrets and variables → Actions → *New repository secret*
   - Name: `KEYSTORE_BASE64`, Secret: 같이 받은 `keystore/release.jks.base64.txt` 파일 내용 전체
   - (선택) Name: `KEYSTORE_PASSWORD`, Secret: 키 비밀번호 (기본값 `samgang-contract`)
   - 등록하지 않아도 APK 는 만들어지지만, 빌드할 때마다 키가 바뀌어 업데이트 설치 전에 앱을 지워야 합니다.
3. **APK 받기**: Actions 탭 → "APK 만들기" → 가장 최근 실행 → 아래 Artifacts 의 `근로계약서-apk`, 또는 Releases 탭의 `근로계약서.apk`.
   휴대폰 브라우저로 Releases 페이지를 열어 내려받으면 바로 설치할 수 있습니다 ("출처를 알 수 없는 앱" 허용).

## 사용 흐름

- 사업주(앱): 새 계약서 작성 → 링크 만들기 → "카카오톡으로 보내기" → 근로자에게 전송
- 근로자(카톡 링크): 내용 확인 → 서명 → "카카오톡으로 보내기"(서명 완료 링크) 또는 "PDF 보내기"로 회신
- 사업주(앱): 카톡 메시지를 길게 눌러 **공유 → 근로계약서** 를 고르면 서명된 계약서가 열림 → PDF 저장·교부 → 필요하면 "세무회계삼강 채널로 보내기"

## 설정 바꾸기

`docs/index.html` 과 `app/src/main/assets/index.html` 맨 위 `CONFIG` 부분:

- `channelChatUrl`: 카카오톡 채널 채팅 주소 (세무회계삼강 주소가 들어 있음)
- `kakaoJsKey`: (선택) 카카오 디벨로퍼스 JavaScript 키 — 넣으면 카카오톡 공유 창이 바로 열림
- `firebase`: (선택) 짧은 링크 모드 — `docs/설치안내.txt` 참고
- `publicUrl`: 근로자용 페이지 주소. APK 는 빌드할 때 GitHub Pages 주소가 자동으로 들어가고, 앱 첫 화면에서 바꿀 수도 있습니다.

파일을 고쳐 `main` 에 올리면 페이지는 바로 갱신되고, `app/` 쪽이 바뀌면 APK 가 다시 만들어집니다.
