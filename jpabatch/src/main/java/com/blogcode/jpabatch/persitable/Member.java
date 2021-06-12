package com.blogcode.jpabatch.persitable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member implements Persistable<String> {

    @Id
    private String memberId;

    @ManyToOne
    @JoinColumn(name = "team_name")
    private Team team;

    @Transient
    private boolean isNew = true;

    public Member(String memberId, Team team) {
        this.memberId = memberId;
        this.team = team;
    }

    @Override
    public String getId() {
        return memberId;
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

    public void changeTeam(Team team) {
        this.team = team;
        team.addMember(this);
    }
}
