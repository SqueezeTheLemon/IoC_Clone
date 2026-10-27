# IoC clone 프로젝트
## 1. IoC란 무엇인가?
= Inversion of Control

"객체를 누가 만들고 누가 연결하느냐"의 주도권을 내 코드가 아니라 바깥의 컨테이너가 갖는 것.

### 1.1 코드로 보기

`TestPostService.java`
~~~ java
@RequiredArgsConstructor   // lombok: final 필드를 받는 생성자를 자동으로 만들어 줌
public class TestPostService {
    private final TestPostRepository testPostRepository;
}
~~~

=> TestPostService는 일하라면 TestPost Repository가 필요함. (이를 의존이라 한다. )

`IOC가 없다면?`
~~~java
public class TestPostService {
    private final TestPostRepository testPostRepository = new TestPostRepository(); // 내가 직접 만듦
}
~~~

Service가 Repository 객체를 직접 생성 (new)

=> Service를 10군데에서 만들면 Repository도 10개를 직접 만들어야 하고, 추후 다른 Repository로 바꾸려면 Service 코드를 몽땅 고쳐야 함.

`IoC가 있다면?`
~~~java
// service는 repository를 생성자로 받는다고만 선언
public TestPostService(TestPostRepository testPostRepository){};
~~~

"바깥(container)"에서 이루어지는 일
~~~java
TestPostRepository repo = new TestPostRepository();
TestPostService service = new TestPostService(repo);
~~~

=> 이걸 해당 프로젝트에서 직접 구현할 것! 

## 1.2 TERMS

| 용어          | 뜻                                | 과제에서                                                       |
|-------------|----------------------------------|------------------------------------------------------------|
| Bean        | 컨테이너가 만들고 관리하는 객체                | testPostService, testPostRepository, testFacadePostService |
| DI (의존성 주입) | 필요한 객체를 바깥에서 넣어주는 역할             | 생성자에 Repository를 넣어주는 것                                    |
| SingleTon   | 같은 이름의 빈은 딱 하나만 만들어두고 계속 같은 걸 반환 | 두 번 요청ㅇ해도 isSameAs                                         |

# 2. Test Case로 확인하고자 하는 것

- t1 : new ApplicationContext()로 객체 생성
- t2 : getBean("testPostService")로 TestPostService bean 얻어오기 
- t3 : 같은 이름으로 두번 얻어도 같은 객체임을 확인 (singleTon)
- t4 : getBean ("testPostRepository")로 TestPostRepository Bean 얻어오기
- t5 : testPostService의 testPostRepository 필드의 빈이 주입되어 있음 (t2-t4 연결)
- t6 : testFacadePostService에 testPostService,testPostRepository 빈이 주입되어 있음. 




































