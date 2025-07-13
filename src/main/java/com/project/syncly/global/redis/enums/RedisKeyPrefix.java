package com.project.syncly.global.redis.enums;

public enum RedisKeyPrefix {
    EMAIL_AUTH_CODE("EMAIL_AUTH:C:"),
    EMAIL_AUTH_VERIFIED("EMAIL_AUTH:V:"),
    LOGIN_CACHE("LOGIN_CACHE:"),
    MEMBER_CACHE("MEMBER_CACHE:"),
    S3_AUTH_OBJECT_KEY("S3_OBJECT_KEY:"),
    BLACKLIST_ACCESS("BLACKLIST:ACCESS:"),
    BLACKLIST_REFRESH("BLACKLIST:REFRESH:"),
    //LiveKit
    CALL_ROOM("CALL_ROOM:"),
    CALL_PARTICIPANT("CALL_ROOM:%s:PARTICIPANT:%s"),
    CALL_ROOM_EVENTS("CALL_ROOM_EVENTS"),
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

}

