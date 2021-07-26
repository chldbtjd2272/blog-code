#### Maria DB Connector + Aurora 이슈

문제상황

- aurora db를 사용하는 서비스를 운영 중에 긴 응답시간을 가진 조회요청이 writer DB로 요청이 들어가 커넥션을 다 사용하는 이슈가 발생
- Maraia db connection 설정을 라이터 클러스터, 리더 클러스터를 콤마로 연결해 설정하여 connection이 writer,reader에 각각 max-connection-pool 수만큼 연결되어있음
- Writer 커넥션 풀에 가용 커넉션이 없는데 reader db로 보내는 readOnly 요청이 정상 수행되지 않는 이슈 발생



의심 상황

-  Aurora DB는 콤마로 연결하는 방식이 아닌 클러스터 주소만 추가하도록 설정하기를 권장한다
  - https://mariadb.com/kb/en/failover-and-high-availability-with-mariadb-connector-j/
- 콤마로 연결하는 방식은 커넥션은 각각 잡아두지만 실제로 writer 유휴 커넥션이 없을 경우 커넥션이 없다고 판단한다.
  - writer 유휴 커넥션이 있을 때는 readOnly 요청을 reader pool을 잡아둔 커넥션으로 처리한다. 
- 클러스터 주소만 넣을 경우에는 라이터 커넥션만 잡아두고 특정 트래픽 이상 readOnly 요청이 들어올 시 reader 커넥션을 관리한다.
  - reader 커넥션이 추가된 만큼 writer 커넥션이 줄어든다
  - 총 커넥션 수는 writer+reader = max-connection pool
    

테스트

- 콤마 연결로 할 경우
  - WriterDB에 조회 시간이 오래걸리는 요청을 보낸다
  - WriterDB의 커넥션이 없는 상태가 될 때 ReadOnly 요청을 보내 수행되는지 확인
    
- 클러스터 주소만 넣을 경우
  - ReadOnly 요청을 보내 writer+reader = max-connection pool을 유지하는지 확인

