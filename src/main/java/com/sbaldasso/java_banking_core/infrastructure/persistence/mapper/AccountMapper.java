package com.sbaldasso.java_banking_core.infrastructure.persistence.mapper;

import com.sbaldasso.java_banking_core.domain.model.Account;
import com.sbaldasso.java_banking_core.infrastructure.persistence.entity.AccountJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Account domain model and AccountJpaEntity.
 */
@Component
public class AccountMapper {

    AccountMapper() {
    }

    /**
     * Converts domain Account to JPA entity.
     */
    public AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.getAccountId(),
                account.getAccountType(),
                account.getCurrencyCode(),
                account.getStatus(),
                account.getCreatedAt());
    }

    /**
     * Converts JPA entity to domain Account.
     */
    public Account toDomain(AccountJpaEntity entity) {
        return Account.create(
                entity.getAccountType(),
                entity.getCurrency());
    }
}
