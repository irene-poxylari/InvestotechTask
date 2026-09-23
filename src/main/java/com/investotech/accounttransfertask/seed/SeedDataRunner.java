package com.investotech.accounttransfertask.seed;

import com.investotech.accounttransfertask.entity.Account;
import com.investotech.accounttransfertask.entity.User;
import com.investotech.accounttransfertask.repository.AccountRepository;
import com.investotech.accounttransfertask.repository.UserRepository;
import com.investotech.accounttransfertask.security.ApiKeyHasher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true")
public class SeedDataRunner implements ApplicationRunner {
    public static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ApiKeyHasher hasher;

    public SeedDataRunner(UserRepository userRepository,
                          AccountRepository accountRepository,
                          ApiKeyHasher hasher) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.hasher = hasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("Seed data run..");
        Instant now = Instant.now();
        User alice = userRepository.findById(ALICE_ID)
                .orElseGet(() -> userRepository.save(
                        new User(ALICE_ID,  hasher.sha256("alice-key"))));
        User bob = userRepository.findById(BOB_ID)
                .orElseGet(() -> userRepository.save(
                        new User(BOB_ID, hasher.sha256("bob-key"))));

        userRepository.save(alice);
        System.out.println("Alice saved");
        userRepository.save(bob);

        if (!accountRepository.existsById("alice-eur")) {
            accountRepository.save(new Account("alice-eur", alice, "EUR", 100_000, now));
            System.out.println("Alice's account saved");
        }
        if (!accountRepository.existsById("bob-eur")) {
            accountRepository.save(new Account("bob-eur", bob, "EUR", 25_000, now));
            System.out.println("Bob's account saved");

        }
        if (!accountRepository.existsById("alice-usd")) {
            accountRepository.save(new Account("alice-usd", alice, "USD", 50_000, now));
            System.out.println("Alice's usd account saved");

        }
    }
}
