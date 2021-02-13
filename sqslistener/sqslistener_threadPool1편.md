### Spring cloud SqsListener 메시지 처리 방식 살펴보기 - message 처리 스레드 풀



> spring-cloud-aws-messaging을 활용하면 아주 쉽게 Sqs consumer를 구현할 수 있다.
>
> 쉽게 사용할 수 있다는 장점도 있지만, Queue 별 동시 처리 수를 조절하려면 추상화된 내부 구현을 잘 알아야 한다.
>
> 내부적으로 메시지 처리를 어떤식으로 하고 있는지 살펴보고, Queue별 동시 처리 스레드 수를 조절하려면 어떤식으로 하면 되는지 알아보자.



- spec
  - Spring boot 
  - Spring-cloud-aws-messaging 

#### 시작 전 알아야 하는 내용

- [Messaging listener 설정](https://cyscrud.tistory.com/10?category=806731)

  

#### SQS Listener의 Message 처리 방식

- QueueMessageHandler

  - @SqsListener가 달린 메시지 처리 메서드와 Queue를 매칭해주고 메시지가 수신될 때 처리될 메서드를 매칭해준다.
  
- SimpleMessageListenerContainer

  - QueueMessageHandler와 스레드 풀을 가지고 있다.

  - QueueMessageHandler에서 큐 목록을 가져와 메시지 polling을 진행한다. 

  - 스레드 풀에서 수신 Queue 갯수대로 polling 스레드를 할당한다.

    - Thread 20, Queue 10 -> 실제 메시지를 처리하는 스레드는 10개
    - Thread 1, Queue 1 -> 메시지를 처리가 불가능하다.

  - MaxNumberOfMessages

    - Queue에서 한번 poll할때 가져올 최대 메시지 갯수

    - 해당 설정에 따라 큐별 동시 메시지 처리 갯수가 정해진다.

    - MaxNumberOfMessages를 10개로 설정하면 큐 하나당 동시에 10개의 스레드로 메시지를 처리한다.

    - 한번 땡겨온 메시지들을 전부 처리할때까지 해당 큐 메시지를 땡겨오지 않는다.

    - ```java
      private final class AsynchronousMessageListener implements Runnable {
      	//...
      		@Override
      		public void run() {
      			while (isQueueRunning()) {
      				try {
               //MaxNumberOfMessages 만큼 메시지를 가져온다.
      					ReceiveMessageResult receiveMessageResult = getAmazonSqs()
      							.receiveMessage(
      									this.queueAttributes.getReceiveMessageRequest());
      
      					CountDownLatch messageBatchLatch = new CountDownLatch(
      							receiveMessageResult.getMessages().size());
      					for (Message message : receiveMessageResult.getMessages()) {
      						if (isQueueRunning()) {
                    //스레드에 해당 메시지를 할당한다.
      							MessageExecutor messageExecutor = new MessageExecutor(
      									this.logicalQueueName, message, this.queueAttributes);
                    //처리완료되면 countDown()한다.
      							getTaskExecutor().execute(new SignalExecutingRunnable(
      									messageBatchLatch, messageExecutor));
      						}
      						else {
                    //처리 중간에 큐 정지 명령이 들어오면 해당 메시지는 처리하지 않고 countDown처리
      							messageBatchLatch.countDown();
      						}
      					}
      					try {
                 //메시지가 처리될때까지 대기
      						messageBatchLatch.await();
      					}
      					catch (InterruptedException e) {
      						Thread.currentThread().interrupt();
      					}
      				}
      				catch (Exception e) {
               //,,,
      				}
      			}
               //,,,
      		}
               //,,,
      	}
      
      ```

    - AsynchronousMessageListener는 큐 갯수만큼 생성되고, 수신 큐중 하나의 큐 메시지 처리에 딜레이가 걸려도 다른 큐에 처리에 영향을 주지 않는다.

      - ex) 카카오 페이 충전 큐, 네이버 페이 충전 큐가 각각 처리할때, 카카오 페이 충전 처리에 장애가 일어나 딜레이가 생겨도 네이버 페이 충전 큐 처리는 이상없이 처리된다.

  

  

  #### Test 진행

- messageOfSent 수만큼 스레드 사용한다. 
  - queue하나에 스레드 20개 메세지 1개일 경우 스레드 2개만 사용
  - Queue 2개에 스레드 20개 메세지 9개면 각각의 스레드 영역을 침범하지 않는다.
  
- message 처리 후 테스트 종료하기 위해 전역공간에 CountDownLatch 생성하고 메시지가 처리되면 countDown 수행

- SimpleMessageListenerContainer의 autoStartup을 false로 설정하여 큐에 메시지 생성 후 시작

  - ```java
    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(MessageThreadPoolProvider provider) {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSQSAsync);
        factory.setMaxNumberOfMessages(10);
        factory.setTaskExecutor(provider.getExecutor());
        factory.setAutoStartup(false);
        return factory;
    }
    ```

- 스레드 풀을 커스텀하게 생성하기 위해 MessageThreadPoolProvider를 빈으로 생성

  - ```java
    @Getter
    public class MessageThreadPoolProvider implements DisposableBean {
        private final ThreadPoolTaskExecutor executor;
    
        public MessageThreadPoolProvider() {
            executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(22);
            executor.setMaxPoolSize(22);
            executor.setThreadNamePrefix("sqs-listener-");
            executor.setWaitForTasksToCompleteOnShutdown(false);
            executor.setAwaitTerminationSeconds(30000);
            executor.initialize();
        }
    
        @Override
        public void destroy(){
            executor.destroy();
        }
    }
    
    ```



1. **Queue하나에 스레드 10개 MaxNumberOfMessages 1개일 경우 스레드는 2개만 사용한다. (수신,처리)**

   - ```java
     @Slf4j
     @Component
     @RequiredArgsConstructor
     public class PayListener {
       //사용되는 스레드 갯수를 확인하기 위해 스레드 풀 provider 주입
         private final MessageThreadPoolProvider messageThreadPoolProvider;
     
         @SqsListener(value = "kakao-pay-queue", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
        public void listenKakao(@Payload Message message) {
             message.execute();
             log.info("kakao 페이 충전 {}, active tread {}, thread pool 수{}",
                     message.toString() ,
                     messageThreadPoolProvider.getExecutor().getActiveCount(),
                     messageThreadPoolProvider.getExecutor().getPoolSize());
         }
     }
     ```

   - ```java
     @Getter
     @ToString
     @NoArgsConstructor
     public class Message {
         public static final Set<String> messageCapture = new HashSet<>();
         public static CountDownLatch latch;
         private String text;
     
         public Message(String text) {
             this.text = text;
         }
     
         public void execute() {
             messageCapture.add(text);
             latch.countDown();
         }
     }
     ```

   - ```java
     @SpringBootTest
     public class SimpleListenerTest {
     
         @Autowired
         private QueueMessagingTemplate queueMessagingTemplate;
     
         @Autowired
         private List<SimpleMessageListenerContainer> containerList;
     
         @Test
         @DisplayName("message poll 사이즈가 1이면 스레드를 하나만 사용한다.")
         void name() throws InterruptedException {
             //given
             Message.latch = new CountDownLatch(20);
             for (int i = 0; i < 20; i++) {
                 queueMessagingTemplate.convertAndSend("kakao-pay-queue", new Message("kakao" + i));
             }
     
             //when
             containerList.forEach(container -> container.start());
             Message.latch.await();
     
             //then
             assertThat(Message.getThreadCapture("sqs-listener-").size()).isEqualTo(1);
         }
     }
     ```

   - 처리 결과

     - 사용되는 스레드는 2개로 큐 수신 작업을 하는 스레드 1, 메시지 처리 스레드 1
     - setMaxNumberOfMessages를 1로 설정하면 스레드 풀에 유휴 스레드가 많아도 활용 불가

     - 같은 테스트에서 setMaxNumberOfMessages 10개로 변경 시 
     - 스레드 10개를 다 사용한다




2. **Queue 2개에 스레드 22개 각각 큐에 메세지가 10개일때 하나의 큐처리에 지연이 있어도 다른 큐는 영향받지 않는다.** **(MaxNumberOfMessages  = 5)**

   - ```java
     @Slf4j
     @Component
     @RequiredArgsConstructor
     public class PayListener {
       //사용되는 스레드 갯수를 확인하기 위해 스레드 풀 provider 주입
         private final MessageThreadPoolProvider messageThreadPoolProvider;
     
         @SqsListener(value = "kakao-pay-queue", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
         public void listenKakao(@Payload Message message) throws InterruptedException {
             message.execute();
             log.info("kakao 페이 충전 {}, active tread {}, thread pool {}",
                     message.toString() ,
                     messageThreadPoolProvider.getExecutor().getActiveCount(),
                     messageThreadPoolProvider.getExecutor().getMaxPoolSize());
             Thread.sleep(5000L);// 5초 지연
         }
     
         @SqsListener(value = "naver-pay-queue", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
         public void listenNaver(@Payload Message message) {
             message.execute();
             log.info("naver 페이 충전 {}, active tread {}, thread pool {}",
                     message.toString() ,
                     messageThreadPoolProvider.getExecutor().getActiveCount(),
                     messageThreadPoolProvider.getExecutor().getMaxPoolSize());
         }
     }
     ```

   - ```java
       @Test
         @DisplayName("처리되는 queue 작업은 서로 영향을 주지 않는다.")
         void name2() throws InterruptedException {
             //given
             Message.latch = new CountDownLatch(20);
             for (int i = 0; i < 10; i++) {
                 queueMessagingTemplate.convertAndSend("kakao-pay-queue", new Message("kakao" + i));
                 queueMessagingTemplate.convertAndSend("naver-pay-queue", new Message("naver" + i));
             }
     
             //when
             containerList.forEach(container -> container.start());
             Message.latch.await();
     
             //then
             assertThat(Message.messageCapture.size()).isEqualTo(20);
         }
     ```

   - 처리결과

     - Kakao pay처리는 5초 지연으로 처리가 늦어져도 naver pay 처리에는 영향이 없다.
     - 처리 시간을 보자






#### 결론

- spring-Could-aws를 활용해 SqsListener 서버를 구축한다면 스레드 수와 동시 처리 메시지 수를 잘 지정해야한다.
- MaxNumberOfMessages는 실제 동시에 처리될 메시지 갯수이므로 스레드 수를 잘 고려하여 설정한다.
- MaxNumberOfMessages는 전체 Queue에 공통으로 적용되므로, 큐 별로 각각 설정하고 싶다면 코드에서 설정하지말고 aws 설정하자
  - 적용 우선순위는 코드가 먼저 적용된다.
  - 큐 별로 처리 스레드를 다르게 가져가고 싶으면 Queue에서 직접 MaxNumberOfMessages를 각각 수정해주면 된다.
