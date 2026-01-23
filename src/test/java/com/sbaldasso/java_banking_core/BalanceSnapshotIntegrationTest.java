package com.sbaldasso.java_banking_core;

import com.sbaldasso.java_banking_core.application.command.EntryCommand;
import com.sbaldasso.java_banking_core.application.command.PostTransactionCommand;
import com.sbaldasso.java_banking_core.application.dto.AccountDto;
import com.sbaldasso.java_banking_core.application.dto.BalanceDto;
import com.sbaldasso.java_banking_core.application.service.AccountApplicationService;
import com.sbaldasso.java_banking_core.application.service.BalanceApplicationService;
import com.sbaldasso.java_banking_core.application.service.LedgerApplicationService;
import com.sbaldasso.java_banking_core.application.service.SnapshotApplicationService;
import com.sbaldasso.java_banking_core.domain.model.BalanceSnapshot;
import com.sbaldasso.java_banking_core.domain.valueobject.AccountType;
import com.sbaldasso.java_banking_core.domain.valueobject.EntryType;
import com.sbaldasso.java_banking_core.domain.valueobject.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for balance snapshot functionality.
 * Tests complete flow: create accounts → post transactions → create snapshot →
 * verify balance calculation optimization
 */
@SpringBootTest
@TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:snapshottest",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.kafka.producer.bootstrap-servers=localhost:9092",
                "spring.kafka.consumer.bootstrap-servers=localhost:9092",
                "spring.task.scheduling.enabled=false" // Disable scheduled tasks in tests
})
@Transactional
class BalanceSnapshotIntegrationTest {

        @Autowired
        private AccountApplicationService accountService;

        @Autowired
        private LedgerApplicationService ledgerService;

        @Autowired
        private BalanceApplicationService balanceService;

        @Autowired
        private SnapshotApplicationService snapshotService;

        @Test
        void shouldCalculateBalanceWithoutSnapshot() {
                // Given: Account with 5 transactions
                AccountDto account = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto fundingAccount = accountService.createAccount(AccountType.LIABILITY, "BRL");

                // Post 5 deposits
                for (int i = 1; i <= 5; i++) {
                        ledgerService.postTransaction(new PostTransactionCommand(
                                        UUID.randomUUID(),
                                        EventType.DEPOSIT,
                                        List.of(
                                                        new EntryCommand(account.getAccountId(),
                                                                        new BigDecimal(String.valueOf(i * 100)), "BRL",
                                                                        EntryType.DEBIT),
                                                        new EntryCommand(fundingAccount.getAccountId(),
                                                                        new BigDecimal(String.valueOf(i * 100)),
                                                                        "BRL", EntryType.CREDIT))));
                }

                // When: Get balance (no snapshot exists yet)
                BalanceDto balance = balanceService.getBalance(account.getAccountId());

                // Then: Balance = 100 + 200 + 300 + 400 + 500 = 1500
                assertEquals(new BigDecimal("1500.00"), balance.getBalance());
        }

        @Test
        void shouldCreateSnapshotAndCalculateBalanceFromIt() throws Exception {
                // Given: Account with 10 transactions
                AccountDto account = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto fundingAccount = accountService.createAccount(AccountType.LIABILITY, "BRL");

                // Post 10 deposits
                for (int i = 1; i <= 10; i++) {
                        ledgerService.postTransaction(new PostTransactionCommand(
                                        UUID.randomUUID(),
                                        EventType.DEPOSIT,
                                        List.of(
                                                        new EntryCommand(account.getAccountId(),
                                                                        new BigDecimal("100.00"), "BRL",
                                                                        EntryType.DEBIT),
                                                        new EntryCommand(fundingAccount.getAccountId(),
                                                                        new BigDecimal("100.00"), "BRL",
                                                                        EntryType.CREDIT))));
                }

                // When: Create snapshot after first 10 transactions
                Instant snapshotTime = Instant.now();
                BalanceSnapshot snapshot = snapshotService.createSnapshotForAccount(account.getAccountId(),
                                snapshotTime);

                // Post 5 more transactions after snapshot
                Thread.sleep(100); // Ensure transactions are after snapshot time
                for (int i = 1; i <= 5; i++) {
                        ledgerService.postTransaction(new PostTransactionCommand(
                                        UUID.randomUUID(),
                                        EventType.DEPOSIT,
                                        List.of(
                                                        new EntryCommand(account.getAccountId(),
                                                                        new BigDecimal("100.00"), "BRL",
                                                                        EntryType.DEBIT),
                                                        new EntryCommand(fundingAccount.getAccountId(),
                                                                        new BigDecimal("100.00"), "BRL",
                                                                        EntryType.CREDIT))));
                }

                // Get balance (should use snapshot + incremental calculation)
                BalanceDto balance = balanceService.getBalance(account.getAccountId());

                // Then: Total balance = 15 * 100 = 1500
                assertEquals(new BigDecimal("1500.00"), balance.getBalance());

                // Verify snapshot has correct balance (10 * 100 = 1000)
                assertEquals(new BigDecimal("1000.00"), snapshot.getBalance().getAmount());
        }

        @Test
        void shouldCalculateHistoricalBalanceWithSnapshot() throws Exception {
                // Given: Account with transactions at different time points
                AccountDto account = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto fundingAccount = accountService.createAccount(AccountType.LIABILITY, "BRL");

                // Transaction 1: +100
                ledgerService.postTransaction(new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(account.getAccountId(), new BigDecimal("100.00"),
                                                                "BRL", EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(),
                                                                new BigDecimal("100.00"), "BRL",
                                                                EntryType.CREDIT))));

                Thread.sleep(100);
                Instant time2 = Instant.now();

                // Transaction 2: +200
                ledgerService.postTransaction(new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(account.getAccountId(), new BigDecimal("200.00"),
                                                                "BRL", EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(),
                                                                new BigDecimal("200.00"), "BRL",
                                                                EntryType.CREDIT))));

                // Create snapshot at time2 (balance should be 300)
                snapshotService.createSnapshotForAccount(account.getAccountId(), time2);

                Thread.sleep(100);

                // Transaction 3: +300
                ledgerService.postTransaction(new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(account.getAccountId(), new BigDecimal("300.00"),
                                                                "BRL", EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(),
                                                                new BigDecimal("300.00"), "BRL",
                                                                EntryType.CREDIT))));

                // When: Get historical balance at time2
                BalanceDto balanceAtTime2 = balanceService.getBalanceAsOf(account.getAccountId(), time2);

                // Then: Balance at time2 = 100 + 200 = 300
                assertEquals(new BigDecimal("300.00"), balanceAtTime2.getBalance());

                // And: Current balance = 100 + 200 + 300 = 600
                BalanceDto currentBalance = balanceService.getBalance(account.getAccountId());
                assertEquals(new BigDecimal("600.00"), currentBalance.getBalance());
        }

        @Test
        void shouldCreateSnapshotsForAllAccounts() {
                // Given: Multiple accounts with different balances
                AccountDto account1 = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto account2 = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto fundingAccount = accountService.createAccount(AccountType.LIABILITY, "BRL");

                // Add transactions to account1
                ledgerService.postTransaction(new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(account1.getAccountId(), new BigDecimal("1000.00"),
                                                                "BRL", EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(),
                                                                new BigDecimal("1000.00"), "BRL",
                                                                EntryType.CREDIT))));

                // Add transactions to account2
                ledgerService.postTransaction(new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(account2.getAccountId(), new BigDecimal("2000.00"),
                                                                "BRL", EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(),
                                                                new BigDecimal("2000.00"), "BRL",
                                                                EntryType.CREDIT))));

                // When: Create snapshots for all accounts
                Instant snapshotTime = Instant.now();
                int snapshotsCreated = snapshotService.createSnapshotsForAllAccounts(snapshotTime);

                // Then: Snapshots created for all 3 accounts (2 assets + 1 liability)
                assertTrue(snapshotsCreated >= 3);

                // Verify balances are calculated correctly from snapshots
                BalanceDto balance1 = balanceService.getBalance(account1.getAccountId());
                BalanceDto balance2 = balanceService.getBalance(account2.getAccountId());

                assertEquals(new BigDecimal("1000.00"), balance1.getBalance());
                assertEquals(new BigDecimal("2000.00"), balance2.getBalance());
        }

        @Test
        void shouldPreventDuplicateSnapshots() {
                // Given: Account with transactions
                AccountDto account = accountService.createAccount(AccountType.ASSET, "BRL");
                AccountDto fundingAccount = accountService.createAccount(AccountType.LIABILITY, "BRL");

                ledgerService.postTransaction(new PostTransactionCommand(
                                UUID.randomUUID(),
                                EventType.DEPOSIT,
                                List.of(
                                                new EntryCommand(account.getAccountId(), new BigDecimal("500.00"),
                                                                "BRL", EntryType.DEBIT),
                                                new EntryCommand(fundingAccount.getAccountId(),
                                                                new BigDecimal("500.00"), "BRL",
                                                                EntryType.CREDIT))));

                Instant snapshotTime = Instant.now();

                // When: Create snapshot twice
                BalanceSnapshot snapshot1 = snapshotService.createSnapshotForAccount(account.getAccountId(),
                                snapshotTime);
                BalanceSnapshot snapshot2 = snapshotService.createSnapshotForAccount(account.getAccountId(),
                                snapshotTime);

                // Then: Should return the same snapshot (idempotent)
                assertNotNull(snapshot1);
                assertNotNull(snapshot2);
                assertEquals(snapshot1.getSnapshotId(), snapshot2.getSnapshotId());
        }
}
