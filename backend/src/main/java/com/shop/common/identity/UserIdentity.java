package com.shop.common.identity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserIdentity {

    private final String guestId;
    /** 로그인 사용자의 공개 ULID(JWT subject와 동일). */
    private final String userPublicId;

    public static UserIdentity ofUser(String userPublicId) {
        return new UserIdentity(null, userPublicId);
    }

    public static UserIdentity ofGuest(String guestId) {
        return new UserIdentity(guestId, null);
    }

    public boolean isGuest() {
        return guestId != null;
    }
    public boolean isUser() {
        return userPublicId != null;
    }

    public String getKey() {
        return isGuest() ? "guest:" + guestId : "user:" + userPublicId;
    }


}
