package com.blogcode.jpalock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.persistence.LockModeType;

public interface AccountReadLockRepository extends JpaRepository<Account,Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Account findByUser(String user);

}
