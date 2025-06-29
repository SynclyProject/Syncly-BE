package com.project.syncly.global.redis.enums;

public enum RedisKeyPrefix {
    EMAIL_AUTH_CODE("EMAIL_AUTH:C:"),
    EMAIL_AUTH_VERIFIED("EMAIL_AUTH:V:"),
    LOGIN_CACHE("LOGIN_CACHE:"),
    MEMBER_CACHE("MEMBER_CACHE:"),
    BLACKLIST_ACCESS("BLACKLIST:ACCESS:"),
    BLACKLIST_REFRESH("BLACKLIST:REFRESH:"),

    //WebSocket 관련 키
    WS_SESSIONS("WS:SESSIONS:"),
    WS_ONLINE_USERS("WS:ONLINE_USERS");

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

    public String get() { return prefix; }
}

