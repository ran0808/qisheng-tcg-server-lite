package com.game.config;

import com.game.network.handler.AESGCMDecryptHandler;
import com.game.network.handler.AESGCMEncryptHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class CryptoConfig {
    @Value("${game.crypto.aes.key}")
    private String aesKey;
    @Bean
    public AESGCMDecryptHandler aesGcmDecryptHandler() {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 16, 24, or 32 bytes long");
        }
        return new AESGCMDecryptHandler(keyBytes);
    }
    @Bean
    public AESGCMEncryptHandler aesGcmEncryptHandler() {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        return new AESGCMEncryptHandler(keyBytes);
    }
}
