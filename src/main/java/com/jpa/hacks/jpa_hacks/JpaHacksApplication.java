package com.jpa.hacks.jpa_hacks;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


@SpringBootApplication
public class JpaHacksApplication {

    public static void main(String[] args) {
        SpringApplication.run(JpaHacksApplication.class, args);
    }

}

@Service
@RequiredArgsConstructor
class JpaService {

    private final TransactionTemplate transactionTemplate;
    private final JpaRepo repository;
    private final ExternalService externalService;


    public void transactionWithTemplate() {
        // Step 1: Perform the transaction, commit immediately after DB operations.
        this.transactionTemplate.executeWithoutResult(status -> {
            // Save the entity inside the transaction
            this.repository.save(new JpaEntity(null, "test"));
            // The transaction commits here after executing the database operations
        });
        //Hikari pool Connection is set free right after transaction is completed
        // and doesn't have to wait until externalService returns
        this.externalService.doSomething();
    }


    /**
     * Starts a new transaction, ensuring the use of a new connection from the connection pool (HikariCP).
     * By default, HikariCP provides a maximum of 10 connections. Using Propagation.REQUIRES_NEW ensures
     * that this method uses a separate, new transaction, which will temporarily hold a new connection.
     * <p>
     * Important:
     * - The method creates and commits a new transaction independent of any surrounding transaction.
     * - If this method is called within another transaction, two active connections will be in use
     *   (one for the original transaction and one for this new transaction).
     * - This is useful when you want to perform certain operations in a new transaction, regardless
     *   of the state of the outer transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transactionWithNewConnection() {
        this.repository.save(new JpaEntity(null, "test2"));
    }


    public void saveWithCustomId() {
        /*
         * Since a custom ID is provided, JPA assumes this is an existing entity and will generate
         * an UPDATE query instead of an INSERT. Before updating, JPA will execute an additional SELECT query
         * to check if the entity with this ID already exists in the database.
         *
         * However, in this case, you want to save a new entity with a custom ID.
         * To ensure that JPA correctly handles this as an INSERT (not an UPDATE), consider adding a
         * version field (e.g., @Version) to your entity. This can help JPA
         * distinguish between new and existing entities more reliably.
         */
        final Long version = null;
        this.repository.save(new JpaEntity(System.currentTimeMillis(), "test", version));
    }




}

@Repository
interface JpaRepo extends JpaRepository<JpaEntity, Long> {

}


@Getter
@Setter
@NoArgsConstructor
@Entity
class JpaEntity {
    @Id
    private Long id;
    private String name;

    @Version
    private Long version;


    public JpaEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public JpaEntity(Long id, String name, Long version) {
        this.id = id;
        this.name = name;
    }
}

@Service
class ExternalService {

    @SneakyThrows
    public void doSomething() {
        Thread.sleep(1000);
    }
}
