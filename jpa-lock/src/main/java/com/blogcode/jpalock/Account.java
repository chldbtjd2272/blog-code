package com.blogcode.jpalock;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Entity
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String user;

    private long money;

    public Account(String user, long money) {
        this.user = user;
        this.money = money;
    }

    void use(long useMoney){
        this.money = this.money + useMoney;
    }
}
