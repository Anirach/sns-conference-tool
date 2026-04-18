package com.sns.identity.app;

import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reject passwords that are obviously weak. Sits in front of BCrypt encoding so we don't burn
 * cycles hashing a guaranteed-bad password and we don't end up with a row whose password is
 * trivially crackable from offline dumps.
 *
 * <p>Three rules:
 * <ol>
 *   <li>Length ≥ 8 (also enforced as a Bean Validation floor on the request DTO).</li>
 *   <li>Password must not equal the email's local part (case-insensitive).</li>
 *   <li>Password must not appear in a small embedded blocklist of the most common
 *       breach-corpus passwords. The list is intentionally short — full breach-corpus
 *       checks (HIBP, etc.) are deferred to a hashed-prefix lookup behind a feature flag.</li>
 * </ol>
 *
 * <p>Error messages stay deliberately generic (no "matches your email", "is on the blocklist",
 * etc.) so an attacker probing common passwords against an arbitrary email can't infer which
 * rule is firing.
 */
@Component
public class PasswordPolicy {

    /** Top ~100 most-frequent passwords from the SecLists / RockYou corpus, copied inline. */
    private static final Set<String> BLOCKLIST = Set.of(
        "password", "12345678", "123456789", "1234567890", "qwerty12", "qwertyui", "asdfghjk",
        "letmein1", "welcome1", "iloveyou", "password1", "password!", "admin1234", "master12",
        "trustno1", "passw0rd", "abc12345", "qazwsx12", "1q2w3e4r", "1qaz2wsx", "qwertyuiop",
        "qwerty123", "111111qq", "monkey12", "dragon12", "sunshine", "princess", "football",
        "baseball", "welcome123", "admin123", "p@ssw0rd", "password123", "passw0rd1", "p4ssword",
        "letmein!", "1234abcd", "abcd1234", "00000000", "11111111", "22222222", "33333333",
        "44444444", "55555555", "66666666", "77777777", "88888888", "99999999", "12121212",
        "21212121", "asdf1234", "zxcvbnm1", "qwerasdf", "asdfqwer", "1qazxsw2", "2wsx3edc",
        "test1234", "user1234", "demo1234", "guest123", "root1234", "toor1234", "passpass",
        "admin@123", "admin!23", "changeme", "changeit", "secret12", "secret!1", "default1",
        "system12", "samsung1", "iphone12", "android1", "google12", "facebook", "instagram",
        "spotify1", "netflix1", "youtube1", "starwars", "harrypot", "minecraft", "fortnite",
        "pokemon1", "snoopy12", "michael1", "jennifer", "jordan23", "soccer12", "hockey12",
        "tennis12", "golfgolf", "summer12", "winter12", "spring12", "autumn12", "freedom1",
        "liberty1", "patriot1", "amazing1", "awesome1"
    );

    public void validate(String email, String password) {
        if (password == null || password.length() < 8) {
            throw weak();
        }
        String lower = password.toLowerCase(Locale.ROOT);
        String local = localPartOf(email);
        if (local != null && local.equals(lower)) {
            throw weak();
        }
        if (BLOCKLIST.contains(lower)) {
            throw weak();
        }
    }

    private static String localPartOf(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return null;
        return email.substring(0, at).toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException weak() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Password is too common or matches your email"
        );
    }
}
