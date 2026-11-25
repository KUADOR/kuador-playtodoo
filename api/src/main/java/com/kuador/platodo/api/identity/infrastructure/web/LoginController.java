package com.kuador.platodo.api.identity.infrastructure.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kuador.platodo.api.identity.application.LogIn;

/**
 * Login operations of {@code contracts/tenancy.openapi.yaml}. Tenancy keeps each one on its host: staff on the host
 * of a complex, players and Kuador on the global host.
 */
@RestController
class LoginController {

    static final String STAFF_LOGIN = "/api/v1/auth/staff/login";
    static final String PLAYER_LOGIN = "/api/v1/auth/players/login";
    static final String PLATFORM_LOGIN = "/api/v1/auth/platform/login";

    private final LogIn staffLogIn;
    private final LogIn playerLogIn;
    private final LogIn platformLogIn;

    LoginController(@Qualifier(IdentityConfiguration.STAFF) LogIn staffLogIn,
            @Qualifier(IdentityConfiguration.PLAYER) LogIn playerLogIn,
            @Qualifier(IdentityConfiguration.PLATFORM) LogIn platformLogIn) {
        this.staffLogIn = staffLogIn;
        this.playerLogIn = playerLogIn;
        this.platformLogIn = platformLogIn;
    }

    @PostMapping(STAFF_LOGIN)
    AccessTokenResponse logInStaff(@RequestBody LoginRequest request) {
        return logIn(staffLogIn, request);
    }

    @PostMapping(PLAYER_LOGIN)
    AccessTokenResponse logInPlayer(@RequestBody LoginRequest request) {
        return logIn(playerLogIn, request);
    }

    @PostMapping(PLATFORM_LOGIN)
    AccessTokenResponse logInPlatformUser(@RequestBody LoginRequest request) {
        return logIn(platformLogIn, request);
    }

    private static AccessTokenResponse logIn(LogIn logIn, LoginRequest request) {
        return AccessTokenResponse.of(logIn.logIn(request.email(), request.password()));
    }
}
