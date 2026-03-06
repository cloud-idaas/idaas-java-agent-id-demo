package com.alibaba.idaas.util;

import com.cloud_idaas.core.exception.ConfigException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JwtValidator {

    private static final Cache<String, JWKSet> jwkSetCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(5)
            .build();

    public static void validate(String jwksUri, String jwtToken) {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT token is null or empty");
        }

        try {
            jwtToken = jwtToken.substring(7);
            SignedJWT signedJWT = SignedJWT.parse(jwtToken);
            String kid = signedJWT.getHeader().getKeyID();
            if (kid == null) {
                throw new IllegalArgumentException("JWT header missing 'kid'");
            }

            JWKSet jwkSet = jwkSetCache.get(jwksUri, () -> JWKSet.load(new URL(jwksUri)));
            JWK jwk = jwkSet.getKeyByKeyId(kid);
            if (jwk == null) {
                throw new IllegalArgumentException("No JWK found for kid: " + kid);
            }
            RSAPublicKey publicKey = ((RSAKey) jwk).toRSAPublicKey();

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new JOSEException("JWT signature verification failed!");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String agentAudience = System.getenv("AGENT_AUDIENCE");
            if (agentAudience == null){
                throw new ConfigException("AGENT_AUDIENCE should be specified via an environment variable.");
            }
            if (!claims.getAudience().contains(agentAudience)) {
                throw new IllegalArgumentException("Invalid audience");
            }
            String agentScope = System.getenv("AGENT_SCOPE");
            if (agentScope == null){
                throw new ConfigException("AGENT_SCOPE should be specified via an environment variable.");
            }
            List<String> scopes = Arrays.asList(claims.getStringClaim("scope").trim().split(" "));
            if (!scopes.contains(agentScope)) {
                throw new IllegalArgumentException("Invalid scope");
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}