package com.sns.identity.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final SnsKeySource keys;

    public JwksController(SnsKeySource keys) {
        this.keys = keys;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey rsa = new RSAKey.Builder(keys.publicKey())
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keys.keyId())
            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
            .build();
        // JWKSet.toJSONObject() returns a public-key-only view (private components are stripped).
        return new JWKSet(rsa).toJSONObject();
    }
}
