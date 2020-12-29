### Spring Aws Cloud SqsListener의 Graceful Shutdown

> SqsListener를 사용하면 특별한 추가 구현없이 메세지 유실이 없는 우아한 종료를 할 수 있다.
>
> 이번 글은 graceful shutdown이 어떤식으로 동작하는지 확인해보자.





#### 시작전 알아야 할 내용

- 기본 설정 링크

- SimpleMessageListenerContainer를 통해 메시지 수신과 처리한다.

  - 내부에 스레드 풀을 가지고 메시지 수신하고 메시지를 처리한다. 

  - 스레드 풀에 스레드 하나로 메시지를 수신하고 수신한 메시지를 내부 스레드 풀에 넘겨준다.

    - 해당 메시지가 다 처리될 때까지 메시지 수신 스레드는 wait한다.

    - AsynchronousMessageListener가 큐에 메시지를 수신해서 처리하는 Runnable 인스턴스고, 해당 인스턴스도 내부 스레드 풀로 수행된다.

    - ```java
      private final class AsynchronousMessageListener implements Runnable {
      ....
      		@Override
      		public void run() {
      			while (isQueueRunning()) {
      				try {
                //메시지 조회
      					ReceiveMessageResult receiveMessageResult = getAmazonSqs()
      							.receiveMessage(
      									this.queueAttributes.getReceiveMessageRequest());
               //메시지 수 만큼 latch 설정
      					CountDownLatch messageBatchLatch = new CountDownLatch(
      							receiveMessageResult.getMessages().size());
      					for (Message message : receiveMessageResult.getMessages()) {
      						if (isQueueRunning()) {
                            //메시지를 내부 스레드 풀에 할당
      							MessageExecutor messageExecutor = new MessageExecutor(
      									this.logicalQueueName, message, this.queueAttributes);
      							getTaskExecutor().execute(new SignalExecutingRunnable(
      									messageBatchLatch, messageExecutor));
      						}
      						else {
      							messageBatchLatch.countDown();
      						}
      					}
      					try {
                  //처리될때까지 수신 작업은 stop
      						messageBatchLatch.await();
      					}
      					catch (InterruptedException e) {
      						Thread.currentThread().interrupt();
      					}
      				}
      				catch (Exception e) {
      				...
      				}
      			}
      ...
      		}
      ```

    - 내부 스레드가 3개라고 가정하고, 동시에 수행 가능한 메시지는 최대 2개다. (수신 스레드 1,처리 스레드 2) 

- 메시지 삭제는 메세지 처리가 다 이뤄진 후에 수행된다.

  - MessageExecutor는 메시지 처리 Runnable로 @SqsListener 메서드를 다 처리한 이후 삭제 요청한다.

  - ```java
    private final class MessageExecutor implements Runnable {
    
    		....
    
    		@Override
    		public void run() {
    			String receiptHandle = this.message.getReceiptHandle();
    			org.springframework.messaging.Message<String> queueMessage = getMessageForExecution();
    			try {
    				executeMessage(queueMessage);
    				applyDeletionPolicyOnSuccess(receiptHandle);
    			}
    			catch (MessagingException messagingException) {
    				applyDeletionPolicyOnError(receiptHandle, messagingException);
    			}
    		}
       ....
    
    	}
    ```

- 빈 사이클 관리는 SimpleMessageListenerContainer의 부모 AbstractMessageListenerContainer를 통해 이뤄진다.

  - SmartLifecycle
    - 해당 인터페이스 상속을 통해 일반 빈보다 더 먼저 처리되는 빈 사이클을 가지게 된다.
    - AbstractMessageListenerContainer은 SmartLifecycle의 phase를 최대값으로 설정하여 가장 나중에 생성되고 가정 먼저 종료되는 빈으로 생성한다.
  - DisposableBean
    - 해당 인터페이스 상속으로 어플리케이션 종료 시 SmartLifecycle이 stop 작업이 다 끝나면 빈 destroy 작업을 진행한다.
    - AbstractMessageListenerContainer은 destroy 호출시 SmartLifecycle의 stop을 다시한번 시도하고, destroy 작업을 실행한다.
    - SimpleMessageListenerContainer의 destroy는 defualtThreadPool을 shutdown한다.





#### Graceful Shutdown

- 어플리케이션 종료시 가장 먼저 호출되는 stop은 AbstractMessageListenerContainer의 stop이다.

  - ```java
    abstract class AbstractMessageListenerContainer
    		implements InitializingBean, DisposableBean, SmartLifecycle, BeanNameAware {
    ...
      @Override
    	public void stop() {
    		getLogger().debug("Stopping container with name {}", getBeanName());
    		synchronized (this.getLifecycleMonitor()) {
    			this.running = false;
    			this.getLifecycleMonitor().notifyAll();
    		}
    		doStop();
    	}
    
      @Override
    	public void destroy() {
    		synchronized (this.lifecycleMonitor) {
    			stop();
    			this.active = false;
    			doDestroy();
    		}
    	}
    ...
      protected abstract void doStop();
    
    	protected void doDestroy() {
    
    	}
    }
    ```

  - doStop은 SimpleMessageListenerContainer에서 구현하고 있다.

    - ```java
      public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {
      ....
      	private ConcurrentHashMap<String, Future<?>> scheduledFutureByQueue;
      
      	private ConcurrentHashMap<String, Boolean> runningStateByQueue;
      ...
      
      	@Override
      	protected void doStop() {
      		notifyRunningQueuesToStop();
      		waitForRunningQueuesToStop();
      	}
      
        @Override
      	protected void doDestroy() {
      		if (this.defaultTaskExecutor) {
      			((ThreadPoolTaskExecutor) this.taskExecutor).destroy();
      		}
      	}
      
      	private void notifyRunningQueuesToStop() {
      		for (Map.Entry<String, Boolean> runningStateByQueue : this.runningStateByQueue
      				.entrySet()) {
      			if (runningStateByQueue.getValue()) {
      				stopQueue(runningStateByQueue.getKey());
      			}
      		}
      	}
      
      	private void waitForRunningQueuesToStop() {
      		for (Map.Entry<String, Boolean> queueRunningState : this.runningStateByQueue
      				.entrySet()) {
      			String logicalQueueName = queueRunningState.getKey();
      			Future<?> queueSpinningThread = this.scheduledFutureByQueue
      					.get(logicalQueueName);
      
      			if (queueSpinningThread != null) {
      				try {
                //큐 리스너가 종료될때까지 어플리케이션 종료 명령을 대기한다.
                //QueueStopTimeout까지 결과확인을 못하면 TimeoutException을 내고 어플리케이션 종료를 계속 실행한다.
      					queueSpinningThread.get(getQueueStopTimeout(), TimeUnit.MILLISECONDS); //-->code 1번
      				}
      				catch (ExecutionException | TimeoutException e) {
      					getLogger().warn("An exception occurred while stopping queue '"
      							+ logicalQueueName + "'", e);
      				}
      				catch (InterruptedException e) {
      					Thread.currentThread().interrupt();
      				}
      			}
      		}
      	}
      }
      ```

    - runningStateByQueue는 AsynchronousMessageListener에서 queue를 계속 수신할건지에 대한 플래그 맵이다. 종료 명령이 들어오면 notifyRunningQueuesToStop()가 호출되어 listner에게 큐 메시지 수신을 멈추라고 명령한다.

    - scheduledFutureByQueue는 메시지 리스너의 Future객체로 AsynchronousMessageListener가 큐 수신 종료 명령을 받아 종료 완료되었는지 확인 가능하다.

    - waitForRunningQueuesToStop()를 통해 돌고 있는 listener가 다 멈췄는지 확인될때까지 어플리케이션 종료를 대기한다.(code 1번 확인)

    - QueueStopTimeout까지 메시지 리스너가 종료되지 않았다면 우선 어플리케이션 종료를 계속 수행한다.

- AbstractMessageListenerContainer stop을 수행하고 나서 destroy를 수행한다.

  - AbstractMessageListenerContainer의 destory메서드를 보면 stop을 다시한번 호출한다.
    - stop을 다시 호출하는 이유는 처음 stop을 호출했을때 메시지 리스너가 종료된걸 확인 못하고 TimeoutException이 발생할 경우를 대비해서 다시한번 종료시도를 하는 것이다.
    - QueueStopTimeout을 10초로 설정하면 최대 20초까지 메시지 리스너가 종료 완료할 시간을 가지게 되는것이다.
    - 만약 하나의 메시지 처리가 30초가 걸린다면 해당 메시지는 처리되지 못하고 어플리케이션이 종료될 수 있다.
      - 해당 메시지는 처리하는 동안 ThreadPool의 스레드가 인터럽트되었기 때문에 메시지 삭제는 실행되지 않는다. 
      - 시간지나면 다시 조회가능 (유실 가능성 x)
    
  - SimpleMessageListenerContainer는 doDestroy를 구현하고 있고, 자기 내부 스레드풀이 디폴트 스레드 풀일 경우 바로 파괴한다.
    - 디폴트 스레드 풀은 종료명령이 들어오면 일을 하고 있는 도중에도 스레드 강제 종료해버린다.
    - 만약 스레드가 일을 끝낼때동안 기다리게 하고 싶으면 커스텀 스레드 풀을 만들고 waitForTasksToCompleteOnShutdown, awaitTerminationMillis을 설정하면 된다.
    - 커스텀 스레드 풀을 SimpleMessageListenerContainer에 줄 경우 doDestroy는 재구현해야한다.
    - QueueStopTimeout을 10초, 메시지 처리 30초, awaitTerminationMillis 10초라고 하면 메시지 처리가 정상적으로 이뤄진다.
    
    - 커스텀 스레드풀은 [github 링크 참고](https://github.com/chldbtjd2272/blog-code/blob/master/sqslistener/src/main/java/com/blogcode/sqslistener/config/CustomSqsListenerConfig.java)


- AmazonSQSBufferedAsyncClient를 sqs 연동 모듈을 사용한다면 정삭적으로 메시지 처리완료하고 종료되어도 sqs에서 메시지 삭제 처리가 안될 수 있다.

  - AmazonSQSBufferedAsyncClient는 비동기로 sqs 연동 작업(send, delete 등)을 수행하는데 메시지를 수행하는 도중 종료 명령이 오면 해당 작업을 다 처리하기전에 바로 종료된다.

  - sqs 연동 작업 처리를 다 완료하고 종료하고 싶다면 QueueBufferConfig를 추가해야한다.

    - ```java
          @Primary
          @Bean(destroyMethod = "shutdown")
          public AmazonSQSBufferedAsyncClient amazonSQSAws() {
              //queue설정으로 sqs 요청 다 처리한 이후 shutdown
              QueueBufferConfig queueBufferConfig = new QueueBufferConfig();
              queueBufferConfig.setFlushOnShutdown(true);
      
              return new AmazonSQSBufferedAsyncClient(AmazonSQSAsyncClientBuilder.standard()
                      .withCredentials(new EnvironmentVariableCredentialsProvider())
                      .withRegion(Regions.AP_NORTHEAST_2)
                      .build(), queueBufferConfig);
          }
      
      ```

  - 메시지 삭제 처리를 진행할때 QueueBuffer를 이용하는데 FlushOnShutdown을 true로 변경하면 종료 명령이 와도 버퍼에 들어있는 요청을 flush하고 종료한다.

    - ```java
      class QueueBuffer {
          ....
      	 QueueBufferConfig config;
        ....
         public void shutdown() {
              if (config.isFlushOnShutdown()) {
                  flush();
              }
              receiveBuffer.shutdown();
          }
      }
      ```