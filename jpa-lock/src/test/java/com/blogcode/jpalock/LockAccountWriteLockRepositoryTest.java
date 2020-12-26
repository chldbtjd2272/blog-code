package com.blogcode.jpalock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionSystemException;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = LockTestConfig.class)
public class LockAccountWriteLockRepositoryTest {

    @Autowired
    private AccountWriteLockRepository writeLockRepository;
    @Autowired
    private AccountReadLockRepository readLockRepository;
    @Autowired
    private TransactionSupport support;


    @AfterEach
    void tearDown() {
        writeLockRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("쓰기 잠금이 걸리면 다른 트랜잭션에서 조회 불가하다")
    void name() throws InterruptedException {
        //given
        writeLockRepository.save(new Account("test", 1000));

        CountDownLatch latch = new CountDownLatch(1);

        //when
        new Thread((() -> support.doTransaction(() -> {
            Account account = writeLockRepository.findByUser("test");
            account.use(-1000);
            latch.countDown();
            Thread.sleep(4000L);
        }))).start();

        //then
        latch.await();
        support.doTransaction(() -> {
            Account account = writeLockRepository.findByUser("test");
            assertThat(account.getMoney()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("읽기 잠금은 다른 트랜잭션에서 조회 가능하다")
    void name2() throws InterruptedException {
        //given
        Long id = writeLockRepository.save(new Account("test", 1000))
                .getId();

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        //when
        new Thread((() -> support.doTransaction(() -> {
            Account account = readLockRepository.findByUser("test");
            account.use(-1000);
            latch2.countDown();
            latch.await();
        }))).start();

        latch2.await();
        support.doTransaction(() -> {
            Account account = readLockRepository.findByUser("test");
            assertThat(account.getMoney()).isEqualTo(1000);
        });
        latch.countDown();

        Account account = readLockRepository.findById(id)
                .orElseThrow(RuntimeException::new);
        assertThat(account.getMoney()).isEqualTo(0);
    }

    @Test
    @DisplayName("읽기 잠금이 걸려있는 트랜잭션이 있으면 변경하는 트랜잭션에서 락이걸린다.")
    void name4() throws InterruptedException {
        //given
        Account account2=readLockRepository.save(new Account("test", 1000));

        CountDownLatch latch = new CountDownLatch(1);

        //when
        new Thread((() -> support.doTransaction(() -> {
            readLockRepository.findByUser("test");
            latch.countDown();
            Thread.sleep(2000L);//socket timeout 3초
        }))).start();

        latch.await();
        support.doTransaction(() -> {
            Account account = readLockRepository.findByUser("test");
            account.use(-1000);
        });
        Account account = readLockRepository.findById(account2.getId()).orElseThrow(RuntimeException::new);
        System.out.println(account.getMoney());
//        assertThatThrownBy(() -> {
//        })
    }
}
