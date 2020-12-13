package com.blogcode.jpalock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface AccountWriteLockRepository extends JpaRepository<Account,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Account findByUser(String user);
}
