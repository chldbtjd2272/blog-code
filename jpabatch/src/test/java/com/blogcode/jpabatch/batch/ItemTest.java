package com.blogcode.jpabatch.batch;

import com.blogcode.jpabatch.JpabatchApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JpabatchApplication.class)
class ItemTest {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void tearDown() {
        itemRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
    }

    @Test
    void name() {
        //given
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Customer customer = new Customer(String.format("user-%s", i));
            for (long j = 0; j < 3; j++) {
                customer.addItem(new Item(j));
            }
            customers.add(customer);
        }

        //when
        customerRepository.saveAll(customers);

        //then
        assertThat(itemRepository.findAll().size()).isEqualTo(9);
        assertThat(customerRepository.findAll().size()).isEqualTo(3);
    }

    @Test
    void name2() {
        //given
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Customer customer = new Customer(String.format("user-%s", i));
            for (long j = 0; j < 3; j++) {
                customer.addItem(new Item(j));
            }
            customers.add(customer);
        }

        customerRepository.saveAll(customers);

        //when
        customers.forEach(Customer::update);
        customerRepository.saveAll(customers);

        //then
        List<Customer> updatedCustomers = customerRepository.findAll();
        updatedCustomers.forEach(customer -> assertThat(customer.getName()).contains("update_"));
    }
}