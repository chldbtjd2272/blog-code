### Spring cloud aws messaging listener 설정

> 큐잉 시스템을 사용할 시 큐에서 메시지를 꺼내 소비하는 시스템이 필요하다.
>
> spring cloud를 사용하면 추상화된 기능들을 사용하여 쉽게 consumer를 구축할 수 있다.
>
> [예제 코드 참고 링크](https://github.com/chldbtjd2272/blog-code/tree/master/sqslistener)



#### 시작전 알아야 할 내용

- SimpleMessageListenerContainer를 사용자가 커스텀 빈으로 생성하지 않으면 SqsAutoConfiguration이 동작해 sqs에 대한 설정들을 자동으로 실행한다.

- 해당 글은 spring-cloud-aws-autoconfigure으로 Sqs 설정을 자동으로 하는걸 예제로 작성했다.

- @EnableSqs 

  - sqs 사용에 필요한 설정들이 자동으로 이뤄진다. 
  - 관련 빈들이 올라온다.
    - AmazonSQSAsync - sqs 연동 클라이언트
    - SimpleMessageListenerContainer - 메시지 처리 리스너 관리자
    - QueueMessageHandler - 메시지 처리자
    - .....

- @SqsListener

  - method에 선언할 수 있는 어노테이션으로 구독할 큐의 메시지를 method에 연결해준다.

  - ```java
    @Slf4j
    @Component
    @RequiredArgsConstructor
    public class MessageListener {
    
        @SqsListener(value = "test-queue")// queue name
        public void listen(Message message) {
          log.info(message.toString());  
        }
    }
    ```

- SimpleMessageListenerContainer

  - AutoConfig로 올라오는 빈으로 큐에서 메시지를 가져와 @SqsListener 어노테이션이 붙은 메서드에 메시지 처리를 맡기는 과정을 관리하는 빈이다.
  - 해당 빈에서 실제 listener와 관련된 설정들을 할 수 있다.
  - 메시지 처리 스레드와 메시지 수신 작업들을 관리한다.

- listner에 대한 graceful shutdown은 SimpleMessageListenerContainer에 구현되어 있다.

  - graceful shutdown을 지원하지 않는다면 어플리케이션 종료될때 메시지가 정상처리가 되지 않거나, 유실될 수 있다.

  - 수신한 메시지들을 유실 없이 정상처리하고 종료하는 프로세스는 다음글에 자세히 설명하겠다.

    



#### Message Listner 설정

- AutoConfig가 동작해 Sqs 연동에 필요한 bean들을 띄울때 SimpleMessageListenerContainer을 만드는 방법은 SimpleMessageListenerContainerFactory가 bean으로 떠있으면 해당 factory에서 SimpleMessageListenerContainer를 만들고 factory가 bean으로 없을 때는 기본 생성자로 생성된  factory로 container를 생성한다.

  - ```java
    @Configuration
    @Import(ContextDefaultConfigurationRegistrar.class)
    public class SqsConfiguration {
    
    	@Autowired(required = false)
    	// @checkstyle:off
    	private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory = new SimpleMessageListenerContainerFactory();
    
    	...
    	@Bean
    	public SimpleMessageListenerContainer simpleMessageListenerContainer(
    			AmazonSQSAsync amazonSqs) {
    	...
    		SimpleMessageListenerContainer simpleMessageListenerContainer = this.simpleMessageListenerContainerFactory
    				.createSimpleMessageListenerContainer();
    		simpleMessageListenerContainer.setMessageHandler(queueMessageHandler(amazonSqs));
    		return simpleMessageListenerContainer;
    	}
    
    ....
    }
    
    ```

  - 커스텀한 설정을 추가한 SimpleMessageListenerContainer를 생성하고 싶다면 Factory를 빈으로 생성하면 된다.

- ```java
     @Bean
      public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(AmazonSQSAsync amazonSqs) {
          SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
          factory.setAmazonSqs(amazonSQSAsync);
          factory.setMaxNumberOfMessages(5);
          factory.setWaitTimeOut(10);
          factory.setVisibilityTimeout(60);
          factory.setAutoStartup(false);
          factory.setQueueMessageHandler(queueMessageHandlerFactory().createQueueMessageHandler());
          factory.setTaskExecutor(threadPoolTaskExecutor());
    
          return factory;
      }
  ```

  - MaxNumberOfMessages

    - queue에 메시지를 poll할때 한번에 가져올 메시지 양이다. 1~10까지 설정 가능하다.

  - WaitTimeOut

    - queue에 메시지가 존재하지 않을때 메시지가 들어올때까지 queue를 polling하는 시간
    - 해당시간까지 메시지가 들어오지 않으면 polling을 끊고 다시 연결한다.
    - 해당 설정을 길게 주면 poll 요청에 대한 비용이 줄어든다.
    - 설정 범위 1~20 초

  - VisibilityTimeout

    - 수신한 메시지를 다른 consumer에서 조회하지 못하도록 감추는 시간이다.
    - 해당 시간동안 가져간 메시지를 소비하지 못하면 queue에서 재조회 가능하다.
    - 재조회 시간이 지나 메시지가 보이는 상태에서 삭제 요청이 오면 수행되지 않는다. (메시지 처리시간을 고려해서 설정해야 한다.)

  - QueueMessageHandler

    - 메시지 처리자로 들어온 메시지 컨버팅등 다양한 메시지 처리 설정들을 커스텀할 수 있다.
    - sqsListener에 대한 글로벌 exceptionHandler도 추가 가능하다. 해당 내용은 다음글에 포스팅할 예정

  - TaskExecutor

    - 메시지를 처리할 스레드의 수, queue 사이즈 등 커스텀한 스레드 풀을 설정할 수 있다.

    - 스레드풀의 queue 사이즈는 0으로 설정하는 걸 추천한다.

      - 이미 sqs로 메시지를 관리하는데 어플리케이션 메모리에서 메시지를 관리할 필요가 없다.

    - 스레드 풀의 graceful shutdown을 위한 추가 설정이 필요하면 WaitForTasksToCompleteOnShutdown와 AwaitTerminationSeconds을 설정해야 한다.
    
  - SimpleMessageListenerContainerFactory는 queueStopTimeout 를 10초로 고정할 수 밖에 없어서 그 이상의 메시지 처리 작업 대기 시간을 가지고 싶다면 스레드 풀 종료 시 스레드 작업 대기시간을 설정해야한다.
      
- 커스텀 스레드 풀을 설정할 경우 어플리케이션 종료 시 커스텀 스레드풀을 destroy시키지 않는다. 순차적으로 종료하고 싶다면
      SimpleMessageListenerContainer 을 상속하여 doDestroy 재구현해줘야 한다.
    
  - ```java
        public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {
        ....
        	@Override
        	protected void doDestroy() {
        		if (this.defaultTaskExecutor) { //default 스레드풀일 경우만 객체 파괴
        			((ThreadPoolTaskExecutor) this.taskExecutor).destroy();
        		}
        	}
        ...
        }
        ```
  
- queueStopTimeout 

  - 해당 설정은 queue polling stop 명령이 들어올때 ( 어플리케이션이 종료될때 ) stop이 될 동안 대기하는 시간이다.
  - 메시지 유실없이 종료되려면 메시지 수신을 중지한 이후 메시지를 처리 스레드를 파괴해야한다. 그러기 위해선 어플리케이션 종료 명령이 들어올면  메인스레드가 queue에서 메시지를 수신하는 스레드에 수신 종료 명령을 내리고 종료 응답이 올때까지 대기 후 메시지 처리 thread를 destroy 해야한다.
  - polling 하고 있는 도중 종료 명령이 들어오면 polling이 끝날때까지 대기하고 종료한다.
  - 만약 waitTimeOut 설정이 queueStopTimeout 시간보다 길게 설정되어 있다면 TimeoutException이 발생할 수 있다.
    - https://stackoverflow.com/questions/58601625/prevent-spring-cloud-aws-messaging-from-trying-to-stop-the-queue
  - queueStopTimeout은 Factory클래스를 통해 설정할 수 없고, SimpleMessageListenerContainer를 통해 설정할 수 있다.
  - queueStopTimeout 설정을 위해 SimpleMessageListenerContainer를 띄우면 autoConfig는 이용할 수 없으므로 consumer구성에 필요한 bean들을 직접 띄워야한다.
    - 해당 설정 코드는 [github](https://github.com/chldbtjd2272/blog-code/blob/master/sqslistener/src/main/java/com/blogcode/sqslistener/config/CustomSqsListenerConfig.java) 참고

- @SqsListener(value = "test-queue",deletionPolicy = {})

  - deletionPolicy은 메시지 삭제 정책으로 네가지 종류가 있다.

  - ALWAYS

    - 메시지를 큐에서 꺼내면 무조건 삭제 처리한다.
    - 메시지가 정상 처리가 되지 않아도 삭제하므로 메시지 유실에 주의해야한다.
      - Ex) 메시지를 꺼내와 처리하는 도중 db 문제로 실패해도 해당 메시지는 삭제되므로 유실된다.

  - NEVER

    - 메시지 삭제요청이 올때까지 절대 삭제하지 않는다.

    - Acknowledgment 클래스의 acknowledge를 호출할 때 삭제처리 된다.

    - ```java
      @Slf4j
      @Component
      @RequiredArgsConstructor
      public class MessageListener {
      
          @SqsListener(value = "test-queue",deletionPolicy = SqsMessageDeletionPolicy.NEVER)
          public void listen(Message message, Acknowledgment acknowledgment) {
            log.info(message.toString());
            acknowledgment.acknowledge(); //=> 삭제 요청
          }
      }
      
      ```

    - Sqs spec상 중복수신은 일어날 수 있으므로 중복 방어로직은 무조건 추가해야한다. 그러므로 해당 정책을 사용할 시 메시지 삭제가 누락되어 재수신 되어도 서비스상 이슈는 없다.

  - NO_REDRIVE

    - DLQ가 정의되어 있지 않다면 메시지를 삭제한다. (ALWAYS와 동일)
      - DLQ 설명은 링크로 대체
    - DLQ가 정의되어 있다면 ON_SUCCESS와 동일

  - ON_SUCCESS

    - 메시지가 exception 발생안하고 정상 처리되면 삭제한다.



