#### Intellij 빠져나가지 않고 코드 리뷰하기 (Feat : Merge Request Integration)



- 세팅 방법

  - Plugin을 설치한 후 Preference > Other Settings > Merge Request Integration > GitLab에서 GitLab 서버 정보를 입력

  - Merge Request 승인 기능을 사용하고 싶다면 information 체크박스를 클릭

    

- 상세 기능

  - 하단 plugin을 통해 현재 생성된 MR항목을 확인하고 나에게 할당된 MR 리뷰 수행 후 승인처리가 가능하다.

    - 리뷰를 완료한 후 Intellij에서 바로 승인 가능

      

  - MR 요청한 브랜치를 커밋단위로 선택하여 브랜치의 전체 흐름을 보며 코드리뷰 진행

    - 작업 단위가 큰 브랜치는 Commit단위로 나눠서 리뷰를 해야 효율적인 코드리뷰가 가능하다
      

  - Code Review로 넘어가면 자동으로 해당 브랜치로 checkout을 되어 리뷰 코드에서 IntelliJ 단축키를 활용한 코드리뷰가 가능하다.

    - 해당 메서드 사용 부분 확인, 코드 내부 들어가기 등
    - 자동으로 checkout 기능을 사용하고 싶다면 Preference > Other Settings > Merge Request Integration >Code Review > check out targe branch when doing code rivew 체크
    - 코드리뷰 완료 후 stop을 누르면 원래 브랜치로 다시 돌아온다.
    - 코드 리뷰 시 원래 코드뿐 아니라 변경된 코드를 넘나들며 리뷰하는게 가능해진다.
      

  - MR이 리뷰어로부터 comment를 받으면 `Comments`탭을 통해 댓글을 이동할 수 있다.

    - 아직 MR로 리뷰 요청을 안해봐서 해당기능은 사용해보지 못했다ㅠ



- 참고

  - private 저장소는 CE버전을 이용할 수 없으므로 (무료 사용 기간 40일) EE버전을 구매해서 사용해야한다. 

  - Upsource Plugin에서 제공하는 기능 대부분을 지원하지만 Feed 기능이 없다.

    - 리뷰 요청 시 인텔리제이로 알림이 안온다.ㅠ

  - MR로 리뷰할 시 Merge 조건으로 승인을 꼭 받도록 설정해 리뷰를 강제할 수 있다

    - git flow와 upsource를 활용할때는 리뷰 강제성이 없어 누락되는 리뷰가 많았다

    

    

    