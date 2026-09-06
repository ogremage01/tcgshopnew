package com.shop.common.identity;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Component
public class IdentityResolver {

    public UserIdentity resolve(HttpServletRequest request, Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return UserIdentity.ofUser(authentication.getName());
        }

        String guestId = (String) request.getAttribute("guestId");
        
        return UserIdentity.ofGuest(guestId);
    }

}
