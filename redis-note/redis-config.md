### Spring data redis 설정

[redis github](https://github.com/chldbtjd2272/blog-code/tree/master/redis-note)

- 기본 yml로 설정가능하다.

```java
spring:
  redis:
    timeout: 3s -> 명령어 수행 timeout
    host: localhost 
    port: 6379
```

- yml로 설정시 redis 연동에 필요한 bean들이 자동으로 생성된다.
  - redisTemplate,RedisConnectionFactory 등
  - defualt connectionFactory는 Lettuce가 사용된다.
  - Lettuce에서 pool을 사용하고 싶다면 redis:lettuce -> 설정에서 풀 설정들을 하면된다.
- Redis connection의 low -level설정 들을 하고싶다면 직접 해당 설정 bean들을 만들면 된다.

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(getStandaloneConfig(), getLettuceClientConfiguration());
    }

    private RedisStandaloneConfiguration getStandaloneConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localHost");
        config.setPort(6379);
        return config;
    }

    private LettuceClientConfiguration getLettuceClientConfiguration() {
        return LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .clientOptions(getClientOptions())
                .build();
    }

    private ClientOptions getClientOptions() {
        return ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(3)).build())
                .timeoutOptions(TimeoutOptions.enabled())
                .build();
    }

}

```



- 자세한 옵션 설정
  - https://lettuce.io/core/release/reference/#_publishsubscribe