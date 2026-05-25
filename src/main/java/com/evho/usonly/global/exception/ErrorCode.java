package com.evho.usonly.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_FILE_TYPE       (HttpStatus.BAD_REQUEST,  "허용되지 않는 파일 형식입니다."),
    INVALID_FILE_FOR_ANALYSIS(HttpStatus.BAD_REQUEST, "이미지 파일만 분석할 수 있습니다."),
    COUPLE_REQUIRED         (HttpStatus.BAD_REQUEST,  "커플 연결 후 사용할 수 있습니다."),
    SELF_CODE_NOT_ALLOWED   (HttpStatus.BAD_REQUEST,  "본인의 코드는 입력할 수 없습니다."),
    NOTE_SELF_MOVE          (HttpStatus.BAD_REQUEST,  "자기 자신 안으로 이동할 수 없습니다."),
    NOTE_DESCENDANT_MOVE    (HttpStatus.BAD_REQUEST,  "자손 메모 안으로 이동할 수 없습니다."),

    // 401 Unauthorized
    UNAUTHORIZED            (HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다."),
    MEMBER_NOT_REGISTERED   (HttpStatus.UNAUTHORIZED, "등록되지 않은 회원입니다."),

    // 400 Bad Request (chat)
    CHAT_MESSAGE_BLANK      (HttpStatus.BAD_REQUEST,  "메시지 내용을 입력해 주세요."),

    // 403 Forbidden
    FORBIDDEN               (HttpStatus.FORBIDDEN,    "권한이 없습니다."),
    CHAT_DELETE_FORBIDDEN   (HttpStatus.FORBIDDEN,    "본인 메시지만 삭제할 수 있습니다."),
    CHAT_EDIT_FORBIDDEN     (HttpStatus.FORBIDDEN,    "본인 메시지만 수정할 수 있습니다."),
    MEDIA_NOT_IN_ALBUM      (HttpStatus.FORBIDDEN,    "해당 앨범의 사진이 아닙니다."),

    // 404 Not Found
    MEMBER_NOT_FOUND        (HttpStatus.NOT_FOUND,    "회원을 찾을 수 없습니다."),
    CHAT_NOT_FOUND          (HttpStatus.NOT_FOUND,    "메시지를 찾을 수 없습니다."),
    NOTE_NOT_FOUND          (HttpStatus.NOT_FOUND,    "메모를 찾을 수 없습니다."),
    SCHEDULE_NOT_FOUND      (HttpStatus.NOT_FOUND,    "일정을 찾을 수 없습니다."),
    ALBUM_NOT_FOUND         (HttpStatus.NOT_FOUND,    "앨범을 찾을 수 없습니다."),
    MEDIA_NOT_FOUND         (HttpStatus.NOT_FOUND,    "미디어를 찾을 수 없습니다."),
    COUPON_NOT_FOUND        (HttpStatus.NOT_FOUND,    "쿠폰을 찾을 수 없습니다."),
    ANNIVERSARY_NOT_FOUND   (HttpStatus.NOT_FOUND,    "기념일을 찾을 수 없습니다."),
    INVITE_CODE_NOT_FOUND   (HttpStatus.NOT_FOUND,    "유효하지 않은 초대 코드입니다."),

    // 409 Conflict
    ALREADY_COUPLE          (HttpStatus.CONFLICT,     "이미 커플입니다."),
    PARTNER_ALREADY_COUPLE  (HttpStatus.CONFLICT,     "상대방은 이미 커플입니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
