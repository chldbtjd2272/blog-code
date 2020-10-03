## Spring data redis Repository 사용시 주의사항

[redis github](https://github.com/chldbtjd2272/blog-code/tree/master/redis-note)

- spring data redis Repository는 레디스 저장 대상을 entity object로 편리하게 관리할 수 있도록 도와준다.

```java
@Getter
@RedisHash(value = "Token",timeToLive = 60)
public class Token implements Serializable {

    @Id
    private String id;
    private LocalDateTime generateTime;

    public Token(String id) {
        this.id = id;
        this.generateTime = LocalDateTime.now();
    }
}

```

- jpa를 사용할 때처럼 CrudRepository를 사용해 저장,조회가 가능하다.
- @RedisHash를 사용하면 레디스에 저장시 hash로 저장된다.
  - HGET token:1 id -> "1"
  - https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/#redis.repositories.keyspaces

#### 주의사항

- Redis repository를 사용하면 편리하게 저장,조회가 가능하지만 저장되는 데이터를 관리하기 위한 추가 데이터들이 저장된다.

- Token을 저장시 

  - Token:1 
    - @RedisHash의 value값이 prefix로 Token:id 로 저장된다.
  - Token:1:phantom
    - ttl설정으로 생성된 데이터로 본 데이터의 카피 데이터이다.
    - 60초 뒤에 본데이터가 삭제되고 +5분뒤에 copy데이터가 삭제된다.
  - Token (Keyspace 관리)
    - Token key는 Token(Token:1) 타입의 모든 id들을 set으로 관리한다. Token데이터가 추가될때마다 해당 set에 id가 추가된다.
    - https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/#redis.repositories.keyspaces

- 여기서 문제는 추가로 저장되는 **Token**이다.

  - Token set은 phantom데이터가 삭제될때 spring data redis의 repository가 RedisKeyExpiredEvent를  발행하도록 도와준다.

  - RedisKeyExpiredEvent를 받으면 데이터를 Token set에 해당 데이터를 삭제한다.

  - 보통 redis repository에서 만료 이벤트 발행설정은 off되어있다. 해당 설정을 이용하려면 @EnableRedisRepository에 EnableKeyspaceEvents 설정을 켜야한다.

  - 문제는 off 되어있는 상황을 모르고(set의 키가 삭제안되고 계속 쌓인다.) redis사용시 oom이 발생할 수 있다.

  - 추가적으로 aws redis를 사용할 시 aws redis 설정은 notify-keyspace-events 설정이 off 되어있다.(이벤트 수신 설정)

    해당 설정도 on 시켜야한다.


    https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/#redis.repositories.expirations

    https://redis.io/topics/notifications

    

