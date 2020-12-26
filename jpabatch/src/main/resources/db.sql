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
