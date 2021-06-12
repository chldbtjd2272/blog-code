#### Persistable



> jpa에서 제공하는 id 전략을 사용하지 않고 id값을 직접 생성해서 사용할 경우 발생하는 문제와 해결책 정리





```java

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    private String memberId;

    @ManyToOne
    @JoinColumn(name = "team_name")
    private Team team;

    public Member(String memberId,Team team) {
        this.memberId = memberId;
        this.team = team;
    }
}




@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    private String name;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private final List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

    public void addMember(Member member) {
        this.members.add(member);
    }
}

```

위와같이 특별한 아이디 전략을 사용하지 않고 insert를 할 경우 select query가 발생한다.

해당 사유는 JpaRepository의 기능때문인데,

```java
public class SimpleJpaRepository<T, ID> implements JpaRepositoryImplementation<T, ID> {

...

	@Transactional
	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be null.");

		if (entityInformation.isNew(entity)) {
			em.persist(entity);
			return entity;
		} else {
			return em.merge(entity);
		}
	}

....
}
```

repository를 통해 save할 시 새로운 entity라고 판단할 시 persist를 호출하고, 아닐시에는 merge를 호출해 엔티티를 저장한다.

- https://stackoverflow.com/questions/1069992/jpa-entitymanager-why-use-persist-over-merge
- 새로운 객체로 판단하는 id 값이 null일 경우 새 객체로 판단하고 persist를 호출하고 null이 아닐경우는 merge를 호출한다.
- merge는 id 값으로 hibernate 1차 캐시 조회 후 값이 없으면 db를 조회하고 값을 저장한다. 값이 있을 경우에는 update를 수행한다.



이 과정은 단순 insert만 수행해도 되는 상황에도 select 이 수행되므로 성능이 떨어진다. 이 문제를 해결하기 위해선 Persistable 인터페이스를 구현해 새로운 객체인지 기존에 영속화된 객체인지 구분해 주면 해결 가능하다.

```java

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member implements Persistable<String> {

		...
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

    ....
}


@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team implements Persistable<String> {

		......
    
		@Transient
    private boolean isNew = true;


    public Team(String name) {
        this.name = name;
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

		.....
}


```



- isNew값은 entity가 영속화 된 객체인지 새로 저장되는 객체인지를 판단하는 플래그 역할을 한다.

- Transient를 선언하면 영속화 대상에서 제외된다.
- isNew 값을 true로 선언하여 객체 생성시 항상 새로운 객체로 인식하도록 한다.
- ​    @PrePersist
  - 영속화하기 직전 isNew값을 false로 변경하여 영속화된 객체로 표시한다.
- ​    @PostLoad
  - db에서 조회한 값은 isNew값을 false로 지정해 영속화된 객체로 판단하도록 설정한다.

