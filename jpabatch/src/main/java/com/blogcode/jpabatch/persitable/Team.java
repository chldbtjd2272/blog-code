package com.blogcode.jpabatch.persitable;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team implements Persistable<String> {

    @Id
    private String name;

    @OneToMany(mappedBy = "team", fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    private final List<Member> members = new ArrayList<>();

    @Transient
    private boolean isNew = true;


    public Team(String name) {
        this.name = name;
    }

    public void addMember(Member member) {
        this.members.add(member);
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PrePersist
    public void mark() {
        this.isNew = false;
    }
}
