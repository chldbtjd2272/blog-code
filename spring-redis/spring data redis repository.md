### Spring Data Redis Repository 사용시 주의 사항

> srping data redis repository는 추상화된 redis operation을 제공하여 쿼리 작성 없이 쉽게 redis에 질의가 가능하다.
>
> 이번 글에선 repository를 사용할때 발생할 수 있는 이슈에 대해 정리한다.



- spec
  - spring data redis 2.4.2
  - redis 6.0.8



#### 시작 전 알아야 하는 내용

- Spring Data Redis Repository를 사용하면 Stpring Data Jpa 처럼 기본 쿼리들을 자동으로 생성해준다.

  - ```java
    @RedisHash(value = "point",timeToLive = 60L) //ttl 설정
    public class Point {
    
        @Id
        private String id; // userId
        private Long point;
    
        public Point(String id, Long point) {
            this.id = id;
            this.point = point;
        }
    
        public String getId() {
            return id;
        }
    
        public Long getPoint() {
            return point;
        }
    }
    
    
    public interface PointRepository extends CrudRepository<Point, String> {
    }
    
    public class PointService{
       
         public void test() {
           //저장
            pointRepository.save(new Point("test1",100L));
    
           //조회
            pointRepository.findById("test1");
    
           //삭제
            pointRepository.deleteById("test1");
        }
    }
    ```

    

- repository를 사용하면 redis에 hash 형태로 자료를 관리한다.

  - @RedisHash의 value 값이 prefix로 id와 연결하여 key로 저장한다.

  - ```sql
     HSET point:{id} field1 {id} field2 {point}
     HGET point:{id} field1  ---> return test1
    ```

- **저장한 hash data와는 별개로 repository는 추가 데이터들을 저장한다.**

  - id로 설정된 값에 대한 helper index가 redis에 set으로 생성된다.  

    - 해당 인덱스는 repository에서 제공하는 추가 쿼리에 사용된다.

      - ```java
        pointRepository.count(); // --> 호출 시 id set을 통해 count를 조회한다.
        ```

      - redis 수행 쿼리 (id set[point] 의 멤버 수를 조회)
      
        - ![count_쿼리](/Users/choiyooseong/blog-code/spring-redis/img/count_쿼리.png)

  - ttl 설정을 추가할 시 원본 데이터에 대한 복사본이 생성된다.

    - Phantom data는 ttl + 5분으로 만료시간이 생성된다.

    - Phantom data는 repository에서 원본 데이터가 만료되더라도 phantom 정보를 통해 내부  만료 이벤트를 발행할때 원본 정보를 포함하는게 가능하다.

      - ```java
        //이벤트 리스너를 빈으로 띄우면 만료 이벤트 수신 가능
        @Slf4j
        @Component
        public class RedisExpireEventListener implements ApplicationListener<RedisKeyExpiredEvent> {
        
            @Override
            public void onApplicationEvent(RedisKeyExpiredEvent event) {
                  log.info("만료이벤트를 수신했습니다." + event.toString());
            }
        }
        ```

    - Phantom data를 사용하고 싶지 않으면 @EnableRedisRepositories에서 shadowCopy 설정을 off하면 된다.
      [spring data redis ttl](https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis.repositories.expirations)

    - Phantom data를 off하면 RedisKeyExpiredEvent에는 key와 keyspace만 존재한다.



#### Redis Repository의 데이터 생성 과정

- spring data redis 설정

  - ```java
    @Configuration
    @EnableRedisRepositories(
               // 만료이벤트 수신을 위한 key 이벤트 수신 설정
            enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP, 
               // 팬텀 데이터 생성 설정
            shadowCopy = RedisKeyValueAdapter.ShadowCopy.ON)
    public class RedisConfig {
    
        @Bean // redis connection 설정
        public RedisConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration("localhost", 6379);
            SocketOptions socketOptions = SocketOptions.builder().connectTimeout(Duration.ofSeconds(3)).build();
            ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(5))
                    .clientOptions(clientOptions).build();
            return new LettuceConnectionFactory(serverConfig, clientConfig);
        }
    
        @Bean // 만료 이벤트 listener로 만료 발생시 만료 대상 로깅
        public RedisExpireEventListener redisExpireEventListener(){
            return new RedisExpireEventListener(); 
        }
    }
    
    ```





1. ttl 10초로 설정된 point 인스턴스를 repository로 redis에 저장
   
   - point hash,point phantom data, id helper idx 3가지의 값이 레디스에 저장된다.
- save호출시 redis에서 수행되는 쿼리
     - ![redis_저장](/Users/choiyooseong/blog-code/spring-redis/img/redis_저장.png)
   - del 명령어는 기존 동일 key로 데이터가 존재한다면 문제가 될 수 있으므로 삭제 후 저장
   - Phantom data 의 만료 시간은 원본 데이터 +5분으로 설정된다. 
   - redis 생성되는 데이터
     - ![repository_추가_데이터](/Users/choiyooseong/blog-code/spring-redis/img/repository_추가_데이터.png)
   
   
   
2. redis에서 point 정보가 60초 뒤 만료되고 application에 만료 이벤트를 전송한다. 
   

3. redis로부터 만료이벤트를 전달받은 application은 phantom data를 조회하고 repository가 생성한 데이터를 삭제한다. 

   - redis의 만료이벤트를 바로 수신해서 처리하는 listner는 MappingExpirationListener

   - ```java
     static class MappingExpirationListener extends KeyExpirationEventMessageListener {
     ....
     
        @Override
     		public void onMessage(Message message, @Nullable byte[] pattern) {
     		...
     			Map<byte[], byte[]> hash = ops.execute((RedisCallback<Map<byte[], byte[]>>) connection -> {
     
     				Map<byte[], byte[]> hash1 = connection.hGetAll(phantomKey);//phantomData 조회
     
     				if (!CollectionUtils.isEmpty(hash1)) {
     					connection.del(phantomKey); //phantomData가 있을시 redis에 즉시 삭제
     				}
     
     				return hash1;
     			});
     
          ....
        		//phantomData를 이용해서 내부 만료 이벤트를 생성한다.
         	RedisKeyExpiredEvent event = new RedisKeyExpiredEvent(channel, key, value); 
     
     			ops.execute((RedisCallback<Void>) connection -> {
     
     				connection.sRem(converter.getConversionService().convert(event.getKeyspace(), byte[].class), event.getId()); //id set에서 만료된 id 삭제
     				new IndexWriter(connection, converter).removeKeyFromIndexes(event.getKeyspace(), event.getId()); // secondary index가 존재한다면 삭제
     				return null;
     			});
     
     			publishEvent(event); // 내부 만료 이벤트 발행 
     		}
     
     }
     ```

   - 만료 작업을 custom하게 처리하고 싶다면 KeyExpirationEventMessageListener 상속해서 listner를 재구현하면 된다.

   - redis에서 수행되는 쿼리

     - ![만료_이벤트_쿼리](/Users/choiyooseong/blog-code/spring-redis/img/만료_이벤트_쿼리.png)

     

4. 데이터 삭제 이후 applicationEventPublish를 통해 이벤트를 발행 등록된 listener에서 만료 이벤트를 처리한다.

   - ![만료_리스너](/Users/choiyooseong/blog-code/spring-redis/img/만료_리스너.png)





#### Repository 사용시 주의사항

- 위 시나리오에서 redis의 만료이벤트를 application이 받지 못한다면 id helper idx가 삭제되지 않는다.

  - MappingExpirationListener 코드를 보면 repository가 생성하는 추가 idx와 phantom data를 삭제처리하고 내부 이벤트 리스너에 만료이벤트를 발행하는데, redis에서 전송하는 만료이벤트를 수신하지 못한다면 해당 과정을 수행할 수 없다.
  - Application 장애 혹은 설정 문제로 이벤트를 받지 못한다면 원본 데이터와 팬텀 데이터는 ttl 시간이 지나 삭제되고, id set에서 해당 id를 삭제할 방법이 없다.
    - 삭제하기 위해선 원본 key로 repository.delete()요청을 해야한다.

- @EnableRedisRepositories EnableKeyspaceEvents 기본 설정은 off 되어있고, 해당 설정을 키지 않고 @RedisHash에 ttl 설정을 한다면  id helper idx와 secondary idx들이 삭제되지 않고 계속 누적된다.

  - 해당 상황을 모른채 서비스 운영을 한다면 redis에서 out of memory 발생

- application의 EnableKeyspaceEvents 설정을 on하더라도 redis에서 notify-keyspace-events에서 expire event에 대한 설정들을 켜야한다.

  - 기본적으로 aws redis config는 비활성화 되어 있다.

  - 만약 notify-keyspace-events가 설정되어 있지 않다면 application이 올라갈때 redis의 expire event 설정을 켠다. 그러나 다른 설정이 들어가 있다면 동작하지 않는다.

    [redis 링크](https://redis.io/topics/notifications)

  - 해당 설정을 켜지 않으면 데이터 만료가 일어날때 redis가 expire event를 발행하지 않는다.




#### Repository를 사용하고 싶다면..

- redis의 notify-keyspace-events 설정을 확인하자.
- application에서 만료 이벤트를 수신할 수 있도록 EnableKeyspaceEvents 설정을 키자.
- 만료이벤트를 통해 원본 데이터를 활용하여 추가 작업이 필요하지 않다면 redis 메모리 낭비하지 않도록 shadowCopy 설정을 off하자.



#### Redis 확인 명령어

- redis-cli monitor
  - Redis에 들어오는 명령어 모니터링
- config get notify-keyspace-events
  - 이벤트 설정 조회
- config set notify-keyspace-events 'xE'
  - redis notify-keyspace-events 만료 이벤트 생성
- PSUBSCRIBE __keyevent@*__:expired
  - 만료 이벤트 구독

