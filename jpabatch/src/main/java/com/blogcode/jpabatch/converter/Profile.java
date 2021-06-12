package com.blogcode.jpabatch.converter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class Profile {
    private String name;
    private String image;
    private String path;

    public Profile(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public void setPath(String path){
        this.path = path;
    }
}
