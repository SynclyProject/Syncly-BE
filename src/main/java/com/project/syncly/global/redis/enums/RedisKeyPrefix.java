package com.project.syncly.global.redis.enums;

public enum RedisKeyPrefix {
    EMAIL_AUTH_CODE("EMAIL_AUTH:C:"),
    EMAIL_AUTH_VERIFIED("EMAIL_AUTH:V:"),

    EMAIL_AUTH_CODE_BEFORE_CHANGE_PASSWORD("EMAIL_AUTH:CHANGE_PASSWORD:C:"),
    EMAIL_AUTH_VERIFIED_BEFORE_CHANGE_PASSWORD("EMAIL_AUTH:CHANGE_PASSWORD:V:"),

    LOGIN_CACHE("LOGIN_CACHE:"),
    MEMBER_CACHE("MEMBER_CACHE:"),
    S3_AUTH_OBJECT_KEY("S3_OBJECT_KEY:"),
    BLACKLIST_ACCESS("BLACKLIST:ACCESS:"),
    BLACKLIST_REFRESH("BLACKLIST:REFRESH:"),
    //LiveKit
    CALL_ROOM("CALL_ROOM:"),
    CALL_PARTICIPANT("CALL_ROOM:%s:PARTICIPANT:%s"),
    CALL_ROOM_EVENTS("CALL_ROOM_EVENTS"),

    //WebSocket 관련 키
    WS_SESSIONS("WS:SESSIONS:"),
    WS_ONLINE_USERS("WS:ONLINE_USERS"),
    WS_NOTE_SESSIONS("WS:NOTE_SESSIONS:"),  // sessionId -> noteId:workspaceMemberId

    // Refresh Whitelist
    REFRESH_CURRENT("refresh:current:%s:%s"),
    CASHED_UA_HASH("CASHED:UA_HASH:%s:%s"),
    REFRESH_USED("rt:used:%s"),

    //profile
    MEMBER_PROFILE("PROFILE_CACHE:"),

    // Note 실시간 협업 관련 키 (Yjs CRDT 기반)
    NOTE_YDOC("NOTE:YDOC:"),                    // note:{noteId}:ydoc (Base64 encoded Yjs Update)
    NOTE_STATE_VECTOR("NOTE:STATE_VECTOR:"),   // note:{noteId}:state_vector (상태 추적용)
    NOTE_USERS("NOTE:USERS:"),                  // note:{noteId}:users
    NOTE_CURSORS("NOTE:CURSORS:"),              // note:{noteId}:cursors
    NOTE_DIRTY("NOTE:DIRTY:"),                  // note:{noteId}:dirty (자동 저장 플래그)

    // Deprecated (OT 관련, 마이그레이션 후 제거)
    // NOTE_CONTENT, NOTE_REVISION, NOTE_OPERATIONS
    ;

    private final String prefix;

    RedisKeyPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String get(String suffix) {
        return prefix + suffix;
    }

    public String get(Object suffix) {
        return prefix + suffix.toString();
    }

    public String format(String... args) {
        return String.format(this.prefix, (Object[]) args);
    }

    public String get() { return prefix; }

}

