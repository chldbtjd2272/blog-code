package com.blogcode.jpabatch;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@TableGenerator(
        name = "ItemIdGenerator",
        table = "sequences",
        allocationSize = 100)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(
            strategy = GenerationType.TABLE,
            generator = "ItemIdGenerator"
    )
    private Long id;

    private Long itemNumber;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Item(Long itemNumber) {
        this.itemNumber = itemNumber;
    }

    void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void update() {
        this.itemNumber = itemNumber * -1;
    }
}
