### Aws Credentials 설정

>aws Credentials은 AWS를 사용할 권한을 어플리케이션에 부여하는 것이다.
>
>자격을 부여하는 여러 정책을 사용해보고, 가장 좋은 정책에 대해 알아보자



Spring에서 Aws 서비스를 사용할 때 가장 쉽게 사용할 수 있는 방법은 spring-cloud를 사용하는 것이다.

SpringCloud를 사용한 Sqs연동 예제로 credential설정 방법을 알아보자.



우선 스프링 빌드 그래들에 aws-messaging 의존성을 추가해주자



```groovy
plugins {
    id 'org.springframework.boot' version '2.2.6.RELEASE'
    id 'io.spring.dependency-management' version '1.0.9.RELEASE'
    id 'java'
}

group = 'com.blogcode'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

ext {
    set('springCloudVersion', "Hoxton.SR4")
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    //spring cloud aws sqs 연동모듈
    implementation 'org.springframework.cloud:spring-cloud-starter-aws-messaging'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

test {
    useJUnitPlatform()
}

```



다음은 aws sqs서비스에 접근하도록 도와주는 client를 설정해보자.

```java
@Slf4j
@Configuration
public class SqsMessageConfig {

    //aws sqs에 연결해주는 클라이언트 
    //해당 클라이언트를 이용하여 메세지 리스너와 전송 클라이언트가 sqs에 연동가능해 진다.
    @Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAws() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())//-> 자격증명
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }

    //메세지 리스너
    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(
        AmazonSQSAsync amazonSQSAsync) {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSQSAsync);
        return factory;
    }

    //메시지 전송 클라이언트
    @Primary
    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSqs) {
        return new QueueMessagingTemplate(amazonSqs, (ResourceIdResolver) null, 
                                          new MappingJackson2MessageConverter());
    }
}

```

SQS서비스를 이용하기 위해선 메세지 전송 클라이언트와 메시지를 읽을 수 있는 리스너가 필요하다.

위 전송 클라이언트와 리스너 둘다 SQS 서비스에 접근할 수 있는 AmazonSQSAsync 빈을 만들어야 한다.

AmazonSQSAsync 모듈에 SQS접근 권한에 대한 설정을 위해 여러 Provider클래스를 제공해주고, 사용자는 해당 Provider를 사용해 credential을 설정하면 된다.



#### IAM 만들기

 계정에 생성된 SQS서비스에 접근하기 위한 자격을 얻기 위해선 AWS_ACCESS_KEY_ID와AWS_SECRET_ACCESS_KEY가 필요하다.

IAM을 생성하고 sqs에 대한 권한을 주고 액세스 키를 발급하면 된다.





1. BasicAWSCredentials

credentials을 주입하는 가장 간단한 방법이다. 액세스키와 비밀키를 코드에 직접 적어 주입하는 방법이다.

아래와 같이 발급된 키를 이용해 client빈을 만들면 sqs접급 권한이 주입된다.

```java
@Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAws() {
        final String accessKey="*****";
        final String secretKey="*****";
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey,secretKey);
        
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }

```







2. EnvironmentVariableCredentialsProvider

자바의 환경 변수로 설정하는 방법이다.  자바 구동시 환경 변수로 AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY 를 입력해주면 Provider에 주입된다.

발급받은 액세스 키를 환경변수에 넣어주고, Provider를 EnvironmentVariableCredentialsProvider를 설정해 준다.

```java
  @Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAws() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())//-> 자격증명
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }

```

Provider 코드 안쪽을 보면 환경변수를 가져와 BasicAWSCredentials에 주입해주는 코드가 있다.

```java
public class EnvironmentVariableCredentialsProvider implements AWSCredentialsProvider {
    public EnvironmentVariableCredentialsProvider() {
    }

    public AWSCredentials getCredentials() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        if (accessKey == null) {
            accessKey = System.getenv("AWS_ACCESS_KEY");
        }

        String secretKey = System.getenv("AWS_SECRET_KEY");
        if (secretKey == null) {
            secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        }

        accessKey = StringUtils.trim(accessKey);
        secretKey = StringUtils.trim(secretKey);
        String sessionToken = StringUtils.trim(System.getenv("AWS_SESSION_TOKEN"));
        if (!StringUtils.isNullOrEmpty(accessKey) && !StringUtils.isNullOrEmpty(secretKey)) {
            return (AWSCredentials)(sessionToken == null ? new BasicAWSCredentials(accessKey, secretKey) : new BasicSessionCredentials(accessKey, secretKey, sessionToken));
        }
        
         .....
    }
```



인텔리제이를 통해 간단하게 환경변수를 넣어준 뒤 구동하면 sqs 리스너와 메세지 발송 클라이언트에 연동된다.





3. SystemPropertiesCredentialsProvider

자바의 시스템 속성을 이용해 주입하는 방법이다.  

시스템 전역 속성에 액세스 키들을 주입해주면 SystemPropertiesCredentialsProvider 에서 값을 읽어들여 자격을 주입해 준다.



```java
    @Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAws() {
      System.setProperty("aws.accessKeyId","*****");
      System.setProperty("aws.secretKey","*****");
        
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new SystemPropertiesCredentialsProvider())//-> 자격증명
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }
```



4. InstanceProfileCredentialsProvider

이 방법이 가장 안전하게 aws 서비스를 이용할 수 있는 방법이다. (aws 문서에서도 해당 방법 추천하고 있다.)

이 방식은 iam role을 배포 ec2에 바로 부여하는 방식이다. Iam role은 사용자를 생성하는 것이 아닌 aws 서비스 간 역할을 만들어 서로를 호출할 수 있는 권한을 부여하는것이다.

만약 s3를 가진 역할을 생성한다면 해당 ec2는 해당 계정의 s3를 연동할 수 있는 권한을 가질 수 있다.(읽기 쓰기 등 다양하게 권한을 부여할 수 있다.)



위와같이 역할을 생성한 뒤 ec2생성시 role을 주입해주자.

```java
  @Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAws() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())//-> 자격증명
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }
```



그리고 InstanceProfileCredentialsProvider를 주입해주면 끝이다.

보안상 안전한 이유는 당연히 access-key가 프로젝트 코드내에 없어도 권한이 부여된다는 것이다.



만약 credentials설정을 아예 안한다면 어떻게 될까?

DefalultAWSCredentialsProviderChain에 의해 위 provider설정내역들을 쭉 적용해보고 적용이 되는 Provider설정을 자동으로 설정해버린다.

우선순위가 가장 높은 설정은 다행히 InstanceProfileCredentialsProvider이므로, role을 이용해 접근권한을 부여한다면 withCredentials에 아무것도 넣지 않아도 된다!







#### 다른 계정의 aws 서비스 호출하기

다른 계정의 aws 서비스를 호출하기 위해선 다른 aws계정 대한 role을 생성하면 된다.



sqs권한을 추가한 assume role을 생성한뒤 아까 생성했던 sqs의 권한 정책에 다른 계정에 대한 접근권한 role을 추가한다.



```java
@Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQSAws() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new STSProfileCredentialsServiceProvider(new RoleInfo()
                        .withRoleArn("arn:aws:iam::0000000000:role/RoleName") // assume role
                        .withRoleSessionName("--session name--"))) // session token
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }
```









