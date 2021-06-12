package com.blogcode.jpabatch.converter;

import com.blogcode.jpabatch.JpabatchApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JpabatchApplication.class)
class PersonTest {

    @Autowired
    private PersonRepository personRepository;

    @Test
    void name() {
        //given
        Profile profile = new Profile("cys", "cys.png");
        Person person = new Person(profile);
        //when
        Person result = personRepository.save(person);

        //then
        assertThat(result.getProfile().getName()).isEqualTo(profile.getName());
        assertThat(result.getProfile().getImage()).isEqualTo(profile.getImage());
    }
}