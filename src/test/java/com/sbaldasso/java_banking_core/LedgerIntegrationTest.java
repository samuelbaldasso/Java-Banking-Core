package com.sbaldasso.java_banking_core;

import com.sbaldasso.java_banking_core.application.command.EntryCommand;
import com.sbaldasso.java_banking_core.application.command.PostTransactionCommand;
import com.sbaldasso.java_banking_core.application.dto.AccountDto;
import com.sbaldasso.java_banking_core.application.dto.BalanceDto;
import com.sbaldasso.java_banking_core.application.dto.TransactionDto;
import com.sbaldasso.java_banking_core.application.service.AccountApplicationService;
import com.sbaldasso.java_banking_core.application.service.BalanceApplicationService;
import com.sbaldasso.java_banking_core.application.service.LedgerApplicationService;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import com.sbaldasso.java_banking_core.domain.valueobject.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete ledger flow:
 * Create accounts → Post transfer → Verify balances → Reverse → Verify balances
 * restored
 */
@SpringBootTest
@TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.kafka.producer.bootstrap-servers=localhost:9092",
                "spring.kafka.consumer.bootstrap-servers=localhost:9092"
})
@Transactional
class LedgerIntegrationTest {

        @Autowired
        private AccountApplicationService accountService;

        @Autowired
        private LedgerApplicationService ledgerService;

        @Autowired
        private BalanceApplicationService balanceService;

        @Test
        void completeTransferFlowShouldWork() {
                // Step 1: Create two ASSET accounts
                AccountDto accountA = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto accountB = accountService.createAccount(AccountType.ASSET, "BRL");

                assertNotNull(accountA.getAccountId());
                assertNotNull(accountB.getAccountId());

                UUID transferExternalId = UUID.randomUUID();

                // Create a funding account first
                AccountDto fundingAccount = accountService.createAccount(AccountType.LIABILITY, "BRL");

                // Deposit 100 to account A from funding
                PostTransactionCommand initialDeposit = new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(accountA.getAccountId(), new BigDecimal("100"), "BRL",
                                                                EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(), new BigDecimal("100"),
                                                                "BRL",
                                                                EntryType.CREDIT)));

                TransactionDto depositTx = ledgerService.postTransaction(initialDeposit);
                assertEquals(TransactionStatus.POSTED, depositTx.getStatus());

                // Step 3: Verify account A balance = 100
                BalanceDto balanceA = balanceService.getBalance(accountA.getAccountId());
                assertEquals(new BigDecimal("100.00"), balanceA.getBalance());
                assertEquals("BRL", balanceA.getCurrency());

                // Account B balance should be 0
                BalanceDto balanceB = balanceService.getBalance(accountB.getAccountId());
                assertEquals(new BigDecimal("0.00"), balanceB.getBalance());

                // Step 4: Transfer 30 from A to B
                PostTransactionCommand transferCommand = new PostTransactionCommand(
                                transferExternalId,
                                EventType.TRANSFER,
                                List.of(
                                                new EntryCommand(accountA.getAccountId(), new BigDecimal("30"), "BRL",
                                                                EntryType.CREDIT),
                                                new EntryCommand(accountB.getAccountId(), new BigDecimal("30"), "BRL",
                                                                EntryType.DEBIT)));

                TransactionDto transferTx = ledgerService.postTransaction(transferCommand);
                assertEquals(TransactionStatus.POSTED, transferTx.getStatus());

                // Step 5: Verify balances after transfer
                balanceA = balanceService.getBalance(accountA.getAccountId());
                assertEquals(new BigDecimal("70.00"), balanceA.getBalance()); // 100 - 30

                balanceB = balanceService.getBalance(accountB.getAccountId());
                assertEquals(new BigDecimal("30.00"), balanceB.getBalance()); // 0 + 30

                // Step 6: Reverse the transfer
                UUID reversalExternalId = UUID.randomUUID();
                TransactionDto reversalTx = ledgerService.reverseTransaction(
                                transferTx.getTransactionId(),
                                reversalExternalId);

                assertEquals(TransactionStatus.POSTED, reversalTx.getStatus());
                assertEquals(EventType.REVERSAL, reversalTx.getEventType());

                // Step 7: Verify balances restored after reversal
                balanceA = balanceService.getBalance(accountA.getAccountId());
                assertEquals(new BigDecimal("100.00"), balanceA.getBalance()); // Back to 100

                balanceB = balanceService.getBalance(accountB.getAccountId());
                assertEquals(new BigDecimal("0.00"), balanceB.getBalance()); // Back to 0

                // Step 8: Verify idempotency - posting same transaction again returns existing
                TransactionDto duplicateTx = ledgerService.postTransaction(transferCommand);
                assertEquals(transferTx.getTransactionId(), duplicateTx.getTransactionId());
        }
}
