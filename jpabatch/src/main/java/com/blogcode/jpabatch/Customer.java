package com.blogcode.jpabatch;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@TableGenerator(
        name = "CustomerIdGenerator",
        table = "sequences",
        allocationSize = 100)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(
            strategy = GenerationType.TABLE,
            generator = "CustomerIdGenerator"
    )
    private Long id;

    @Setter
    private String name;


    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private final List<Item> items = new ArrayList<>();

    public Customer(String name) {
        this.name = name;
    }

    public void addItem(Item item) {
        item.setCustomer(this);
        this.items.add(item);
    }

    public void update() {
        this.name = "update_" + this.name;
        items.forEach(Item::update);
    }
}
