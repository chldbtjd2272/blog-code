### Hibernate 대량 배치



> Hibernate에서 id 생성 전략으로 Table 전략을 사용해 대량 삽입, 업데이트에 대한 정리

- Spec
  - Spring data jpa
  - Hibernate
  - mariadb



#### 시작 전 알아야 하는 내용

- Hibernate에서 Identity 전략을 사용하여 데이터 삽입시 batch insert를 지원하지 않는다. 

  - Id 리턴받아 entity에 채워넣어야 하기 때문에 대량 Insert가 불가능하다.
- Hibernate에서 batch 기능은 jdbc의 batch api를 호출해 삽입해야할 대상 쿼리를 하나로 묶어 한번에 전송하는 것을 의미
  
- batch insert를 위해선 SEQUENCE 혹은 TABLE 전략을 사용해야하는데, 예제는 mysql 기반인 mariadb로 진행하므로 TABLE전략으로 진행한다.

  - mysql은 SEQUENCE 기능을 제공하지 않는다.

- 대량 insert, update, delete 작업을 효율적으로 처리하기 위해 JDBC는 batch 작업을 위한 별도 API를 제공한다.

  - jdbc는 단순 spec이고 각 벤더사에서 구현체를 구현하여 제공한다.

  - Mysql/MariaDB 는 "rewriteBatchedStatements" 설정 값을 설정하지 않으면 jdbc batch api 를 사용해도 성능이 향상되지 않는다.

  - ```sql
    insert into item (customer_id, item_number, id) values (?, ?, ?),(?, ?, ?),(?, ?, ?),(?, ?, ?).....
    ```

- 예제는 고객과 아이템으로 고객이 여러개의 아이템을 구매하고 한번에 대량 삽입한다.

  - sql script

  - ```mysql
    #시퀀스 테이블
    CREATE TABLE sequences
    (
        sequence_name varchar(255) NOT NULL COMMENT '시퀀스명',
        next_val      bigint       NOT NULL COMMENT '시퀀스값',
    
        CONSTRAINT pk_sequences PRIMARY KEY (sequence_name)
    ) ENGINE = InnoDB COMMENT ='시퀀스 테이블';
    
    #고객 테이블
    CREATE TABLE customer
    (
        id      bigint   NOT NULL COMMENT 'ID',
        name varchar(30) NOT NULL COMMENT '이름',
    
        CONSTRAINT pk_customer PRIMARY KEY (id),
        INDEX idx_customer_name (name)
    ) ENGINE = InnoDB COMMENT ='고객 테이블';
    
    #아이템
    CREATE TABLE item
    (
        id      bigint       NOT NULL COMMENT 'ID',
        item_number  bigint  NOT NULL COMMENT '아이템 번호',
        customer_id bigint NOT NULL COMMENT '고객 id',
    
        CONSTRAINT pk_item PRIMARY KEY (id),
        CONSTRAINT fk_item FOREIGN KEY (customer_id) REFERENCES customer(id)
    ) ENGINE = InnoDB COMMENT ='아이템 테이블';
    
    ```

    



### 대량 삽입 



- ```java
      @Test
      void name() {
          //given
          List<Customer> customers = new ArrayList<>();
          for (int i = 0; i < 3; i++) {
              Customer customer = new Customer(String.format("user-%s", i));
              for (long j = 0; j < 3; j++) {
                  customer.addItem(new Item(j));
              }
              customers.add(customer);
          }
  
          //when
          customerRepository.saveAll(customers);
  
          //then
          assertThat(itemRepository.findAll().size()).isEqualTo(10000);
          assertThat(customerRepository.findAll().size()).isEqualTo(10);
      }
  ```

  - 위와 같이 3 고객을 생성 각각 3개의 아이템을 각 고객이 가지도록 설정하고 마지막 repository를 이용하여 한번에 save한다.

  - 위 테스트 코드를 jpa,jdbc 추가 설정 없이 실행할 시 개별로 삽입된다. 

    

- 추가 설정

  - 대량 삽입을 위해선 기본 datasource 설정과 jpa 설정 이외에 추가설정이 필요하다.

  - ```properties
    spring.jpa.properties.hibernate.jdbc.batch_size=1000
    spring.jpa.properties.hibernate.order_inserts=true
    spring.jpa.properties.hibernate.order_updates=true
    spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
    ```

  - spring.jpa.properties.hibernate.jdbc.batch_size

    - 대량 삽입 설정으로 쿼리를 묶어서 전달하는 설정이다. 설정된 사이즈만큼 묶어서 전달한다.

    - 해당 설정만 되어있을 경우 실제 db에서는 묶어서 되지 않고 개별 수행한다.

    - 어플리케이션 로그

      - ![batch_설정](/Users/choiyooseong/blog-code/jpabatch/img/batch_설정.png)

    - DB 실행 로그

      - ![batch_설정_db](/Users/choiyooseong/blog-code/jpabatch/img/batch_설정_db.png)

    - ```sql 
      insert into customer (name, id) values (?, ?)
      insert into item (customer_id, item_number, id) values (?, ?, ?),(?, ?, ?),(?, ?, ?),(?, ?, ?).....
      insert into customer (name, id) values (?, ?)
      insert into item (customer_id, item_number, id) values (?, ?, ?),(?, ?, ?),(?, ?, ?),(?, ?, ?).....
      .....
      ```

  - rewriteBatchedStatements

    - 위 jdbc 스펙을 true로 설정하면 db 에서도 묶어서 실행된다.
    - db 로그
      - ![rewrite_db](/Users/choiyooseong/blog-code/jpabatch/img/rewrite_db.png)

  - spring.jpa.properties.hibernate.order_inserts

    - 해당 설정이 들어가면 customer은 customer끼리 item은 item끼리 묶어서 인서트한다.
    - 실행되는 쿼리 수가 더 적으므로 더 빠른 속도로 삽입이 가능하다.
    - 어플리케이션 로그
      - ![order_inserts](/Users/choiyooseong/blog-code/jpabatch/img/order_inserts.png)

  - spring.jpa.properties.hibernate.order_updates

    - update시 order_inserts와 마찬가지로 같은 update 테이블끼리 묶어서 실행한다.
    - 어플리케이션 로그
      - ![order_update](/Users/choiyooseong/blog-code/jpabatch/img/order_update.png)

- ID 생성 방식

  - ```java
    @Getter
    @Entity
    @TableGenerator(
            name = "CustomerIdGenerator",
            table = "sequences", // 시퀀스를 관리하는 별도 테이블
            allocationSize = 100)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class Customer {
    
        @Id
        @GeneratedValue(
                strategy = GenerationType.TABLE,
                generator = "CustomerIdGenerator"
        )
        private Long id;
    
        @Setter
        private String name;
    
        @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
        private final List<Item> items = new ArrayList<>();
    
        ....
    }
    
    
    @Getter
    @Entity
    @TableGenerator(
            name = "ItemIdGenerator",
            table = "sequences",
            allocationSize = 100)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class Item {
    
        @Id
        @GeneratedValue(
                strategy = GenerationType.TABLE,
                generator = "ItemIdGenerator"
        )
        private Long id;
    
        private Long itemNumber;
    
        @ManyToOne
        @JoinColumn(name = "customer_id")
        private Customer customer;
    
        ....
    }
    
    
    ```

  - allocationSize

    - sequences 테이블에 저장된 customer테이블과 item 테이블의 각각의 사용 가능한 id 값을 락을 걸어 조회하고 allocationSize만큼 update 실행한다.
    - 위의 예제로 각 sequence가 customer 100, point 100 인 상태에서 실행시 0~100사이의 id를 순차적으로 사용하여 insert한다.
      sequences는 200으로 update
    - 만약 한번 삽입양이 allocationSize보다 크다면 필요한 양 만큼 select - update를 반복하고 그 순간 각 sequnce에 대한 row는 락이 걸린다.
    - allocationSize를 너무 크게 잡으면 비어있는 id가 많이 생기고, 너무 작게 잡으면 id 발번 과정이 여러차례 일어나 성능저하를 줄 수 있다. 
      한번에 삽입되는 양과 성능테스트를 통해 적정양을 설정해야한다.

  - sequence는 row로 각 테이블 별 id를 관리한다.

    - ![sequence_table](/Users/choiyooseong/blog-code/jpabatch/img/sequence_table.png)



#### 주의사항

- table전략은 id를 발번하는 과정에서 sequence 테이블에 발번하는 table row를 락을 걸어 조회하므로 동시요청이 많은 작업일때 성능저하가 일어난다.
- 테이블에 pk는 걸려있지만 auto_increment 설정을 사용하지 않기 때문에 추후 auto_increment설정을 추가하기 위해선 별도의 테이블 작업이 필요하다.
- Table 전략 사용은 실시간으로 다수의 요청을 받아 개별 생성되는 api 형태보단 단일 프로세스로 하나의 스레드가 대량 삽입하는 배치 작업으로 생성되는 작업에 적합하다. 
  - 새벽에 대량으로 적립되는 포인트
- Auto_increment의 유무만 다르므로 별도의 파티션 작업이나 테이블 관리는 기존과 동일하다.



#### 전략 전환

- TABLE전략에서 일반적으로 많이 사용하는 IDENTITY전략으로의 전환이 필요한 경우에는 기존 테이블에 auto_increment설정을 추가해주면 된다.
  - pk설정은 이미 되어 있으므로 auto_increment 설정을 추가해 주면 된다.
  - Auto_increment 설정 추가는 Changing the column data type으로 작업 도중 dml을 수행할 수 없다. 
    - Table 전략을 사용하고 있었다는 것은 배치성으로 데이터를 생성하고 있었다는 의미로 배치 수행 시간을 피해서 작업을 수행하거나 배치를 정지해야한다.
    - https://stackoverflow.com/questions/54163679/why-does-adding-auto-increment-take-so-long
    - https://dev.mysql.com/doc/refman/5.6/en/innodb-online-ddl-operations.html
- 기존에 대량 insert기능을 사용하고 있었다면 해당 부분을 jdbc를 직접 사용하는 방법으로 확장해야한다.

