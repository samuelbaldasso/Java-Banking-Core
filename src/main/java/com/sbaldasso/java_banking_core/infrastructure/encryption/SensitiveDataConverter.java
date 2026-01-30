package com.sbaldasso.java_banking_core.infrastructure.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for automatic encryption/decryption of sensitive data.
 * 
 * This converter is applied to entity fields that contain sensitive information
 * (like account numbers, SSN, etc.) and automatically encrypts/decrypts data
 * when writing to or reading from the database.
 * 
 * Usage:
 * 
 * @Convert(converter = SensitiveDataConverter.class)
 *                    private String accountNumber;
 */
@Converter
@Component
public class SensitiveDataConverter implements AttributeConverter<String, String> {

    private final StringEncryptor encryptor;

    public SensitiveDataConverter(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    /**
     * Encrypts the attribute value before storing in the database.
     * 
     * @param attribute the plain text value
     * @return encrypted value, or null if attribute is null
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptor.encrypt(attribute);
    }

    /**
     * Decrypts the database value when reading from the database.
     * 
     * @param dbData the encrypted value from database
     * @return decrypted plain text value, or null if dbData is null
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptor.decrypt(dbData);
    }
}
