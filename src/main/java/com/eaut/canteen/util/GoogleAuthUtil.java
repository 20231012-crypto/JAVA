package com.eaut.canteen.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Verifies "Sign in with Google" ID tokens. This only proves a token was genuinely issued by
 * Google for this app's client ID and identifies the signed-in Google account — any Google
 * account is accepted, there is no email-domain restriction.
 */
public final class GoogleAuthUtil {

    private static final GoogleIdTokenVerifier VERIFIER = new GoogleIdTokenVerifier.Builder(
            new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(AppConfig.get("google.clientId")))
            .build();

    private GoogleAuthUtil() {
    }

    public static String getClientId() {
        return AppConfig.get("google.clientId");
    }

    /** Returns the verified payload, or null if the token is missing, expired, tampered, or issued for a different app. */
    public static GoogleIdToken.Payload verify(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            return null;
        }
        try {
            GoogleIdToken idToken = VERIFIER.verify(idTokenString);
            return idToken == null ? null : idToken.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return null;
        }
    }
}
