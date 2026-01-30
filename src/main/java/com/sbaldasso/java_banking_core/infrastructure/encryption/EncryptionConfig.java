package com.sbaldasso.java_banking_core.infrastructure.encryption;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Encryption configuration for sensitive data.
 * 
 * Uses Jasypt (Java Simplified Encryption) with AES-256 for encrypting
 * sensitive database fields like account numbers.
 * 
 * Security Notes:
 * - Encryption key should be stored in a secure vault (not in code)
 * - Use environment variables or external configuration in production
 * - Consider key rotation policies
 * - Do not commit encryption keys to version control
 */
@Configuration
public class EncryptionConfig {

    @Value("${encryption.key:CHANGE_THIS_IN_PRODUCTION}")
    private String encryptionKey;

    /**
     * Creates a string encryptor bean for encrypting sensitive data.
     * 
     * Configuration:
     * - Algorithm: PBEWITHHMACSHA512ANDAES_256 (AES-256 encryption)
     * - Key Obtention Iterations: 1000 (PBKDF2 iterations)
     * - Pool Size: 4 (for better performance)
     * 
     * @return configured StringEncryptor
     */
    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        config.setPassword(encryptionKey);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("4");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");

        encryptor.setConfig(config);
        return encryptor;
    }
}
