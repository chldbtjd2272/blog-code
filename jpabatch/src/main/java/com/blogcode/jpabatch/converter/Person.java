package com.blogcode.jpabatch.converter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 1L;

    @Convert(converter = ProfileConverter.class)
    private Profile profile;

    public Person(Profile profile) {
        this.profile = profile;
    }
}
