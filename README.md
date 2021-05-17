---
title: 트랜잭션 격리레벨
date: 2021-05-05
description: 트랜잭션 격리레벨
tags:
  - Transaction
---  

데이터베이스는 여러 쓰레드(사용자)들이 사용하게 된다. 때로는 동일한 데이터에 동시에 여러 쓰레드들이 접근하여 데이터를 조작 할 때 동시성 문제로 인하여 
데이터의 정합성이 깨질 수도 있다. 
이러한 동시성 문제를 해결할 때 쓰레드간의 트랜잭션의 고립수준을 정하여 트랜잭션간 영향도 범위를 정하기 위하여 격리레벨을 사용한다.   

이번 포스팅에서는 트랜잭션 격리레벨에 대한 개념정리와 
spring의 @Transactional 어노테이션을 사용하여 격리레벨이 소스코드상에서 어떻게 진행되는지 확인하는 방식으로 내용을 정리하였다. 

  트랜잭션 격리레벨은 총 4개로 구성되어 있다. 고립수준이 높아질 수록 동시성에 대한 데이터의 고립성 혹은 안전성은 높지만 
  동시성에 대한 처리는 낮아지게 된다.  

  |Isolation Level| Dirty Read | Nonrepeatable Read | Phantom Read |
  |:---------------:|:-----------:|:-----------:|:-----------:|
  |Read Uncommitted|o|o|o|    
  |Read Committed|x|o|o|
  |Repeatable Read|x|x|o|
  |Serializable|x|x|x|

  위의 표는 트랜잭션 격리레벨에 따라 어떤 동시성 이슈가 발생하는지 정리된 표이다. 위 표에 대한 자세한 내용은
  아래의 각각의 격리레벨에 대한 설명에서 자세히 다루겠다. 

  1. Read Uncommitted  
   일반적으로 거의 사용하지 않는 레벨이다. Read UnCommitted는 특정 트랜잭션이 commit, rollback 여부와 상관없이
   다른 트랜잭션에서 보여진다. Commit 혹은 Rollback의 결과 이전에 다른 트랜잭션에서 보여지기 때문에 
   `Dirty Read`와 `Dirty Wirte` 현상이 발생할 수 있다. 
   (Dirty Read/Wirte는 동시에 트랜잭션이 발생되었을 때, 커밋되지 않는 사항에 대해서 읽거나 수정을 할때를 일컫는다) 

   ```
    현재 DB에는 product 테이블에 사과상품이 500원으로 되어있다

    +-------+               +---+                 +------+
    |트랜잭션A|               | DB |                |트랜잭션B|
    +-------+               +---+                 +------+
       |                      |                       |
       |  update product      |                       |
       |  set price = 1000    |                       |
       |  where name = '사과'  |  select price         |
       | -------------------+ |  from product         |
       |                    | |  where name ='사과'    |
       |                    | | +-------------------- | 
       |                    | | | (Dirty Read 발생)    |
       | <----- A 커밋 ------+ | +------------------>  |
       |                      |                       |
       |                      |                       |
   ```        
   - 트랜잭션A가 사과 상품의 가격을 500원에서 1000원으로 변경한다.
   - 트랜잭션B가 사과 상품의 가격을 조회하면, 아직 커밋되지 않는 1000원의 가격을 조회한다.
   - 트랜잭션 A에 대해 커밋완료한다.   
   만약 트랜잭션 A가 롤백이 된다면 업데이트한 사과의 가격 1000원은 취소되고 다시 500원이 되지만, 커밋되지 않는 데이터를
   트랜잭션B는 읽었기 때문에(Dirty Read) 1000원의 가격을 조회한 결과가 나오게 되어 데이터의 정합성이 깨진다.
   이러한 부분때문에 Read Uncommitted 격리레벨은 거의 사용하지 않는다.


  2. Read Committed  
  커밋된 데이터만 읽고, 쓸수 있는 레벨이기 때문에 Read Uncommited에서 발생되었던 Dirty Read 혹은 Dirty Write 현상이 발생하지 않는다. 
  커밋된 데이터와 트랜잭션 진행중인 데이터를 별도의 공간으로 보관하기 때문에 커밋된 데이터만 읽을 수 있도록 보장한다. 
  또한, 데이터 쓰기는 행단위 잠금을 사용하는데,  동일한 데이터에 동시에 여러 트랜잭션이 
  같은 데이터를 수정한다면 먼저 수정에 들어간 트랜잭션이 끝날때까지 대기상태에 있는다. 
  
   ```
    현재 DB에 저장되어있는 데이터 
    +-------------------+
    | ID | name | price |
    +-------------------+
    | 1  | 사과  | 500   |
    +-------------------+ 
   
    +-------+               +---+                 +------+
    |트랜잭션A|               | DB |                |트랜잭션B|
    +-------+               +---+                 +------+
       |                      |                       |
       |  update product      |                       |
       |  set price = 1000    |                       |
       |  where name = '사과'  |  select price         |
       | -------------------+ |  from product         |
       |                    | |  where name ='사과'    |
       |                    | |  +------------------  | 
       |                    | |  |                    |
       |                    | |  +------------------> | 
       |                    | |   커밋되어있는 500원 리턴  |
       |  <----- A 커밋 -----+ |                       |
       |                      |                       |
       |                      |                       |
   ```
  커밋되어있는 데이터만 읽거나 쓰기 때문에 Dirty Read/Write 현상이 발생되지 않는다.
  하지만, 하나의 트랜잭션에서 동일한 쿼리를 실행할 때 읽는 시점에 따라 데이터가 달라지는 현상이 발생할 수 있다. 

    ```
    현재 DB에 저장되어있는 데이터 
    +-------------------+
    | ID | name | price |
    +-------------------+
    | 1  | 사과  | 500   |
    +-------------------+ 
   
    +-------+               +---+                 +------+
    |트랜잭션A|               | DB |                |트랜잭션B|
    +-------+               +---+                 +------+
       |                      |                       |
       |  update product      |                       |
       |  set price = 1000    |                       |
       |  where name = '사과'  |  select price         |
       | -------------------+ |  from product         |
       |                    | |  where name ='사과'    |
       |                    | |  +------------------  | 
       |                    | |  |                    |
       |                    | |  +--- 500원 리턴 -----> | 
       |                    | |                       |
       |  <----- A 커밋 -----+ |                       |
       |                      |                       |
       |                      |  select price         |
       |                      |  from product         |
       |                      |  where name = '사과'   |
       |                      |  +------------------- |
       |                      |  |                    |
       |                      |  +--- 1000원 리턴  ---> |
       |                      |                       |
  ```
   - 트랜잭션A가 사과 상품가격을 1000원으로 변경한다. 
   - 트랜잭션B가 사과 상품가격을 조회한다. 이때 이미 커밋된 내용인 `500원`을 리턴받는다.
   - 트랜잭션A의 상품가격 1000원 변경건이 커밋된다.
   - 트랜잭션B가 사과 상품가격을 조회한다. 이때 커밋된 내용인 `1000원`을 리턴받는다.  

  트랜잭션 B는 하나의 트랜잭션 내에서 동일한 조회 쿼리를 수행하였지만 
  `읽는 시점`에 따라 다른 결과를 받아 데이터가 다르게 나오는 문제가 발생한다. 이를 `Nonrepeatable Read` 현상이라 한다. 
  
  v. ReadCommitted의 Nonrepeatable Read 현상 테스트
  ```java
public interface FruitRepository  extends JpaRepository<Fruit, Long> {
    @Transactional(isolation = Isolation.READ_COMMITTED)
    Fruit findFruitById(Long id);
}
  ```
  데이터를 조회할 때 격리레벨 READ_COMMITED로 지정하였다.

  ```java
    @Transactional(readOnly = true)
    public void readCommitted(Long id) {
        Fruit fruit1 = fruitRepository.findFruitById(id);
        log.info("before : {}", fruit1.getPrice());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        entityManager.detach(fruit1); //영속성 캐싱으로 인하여 비영속 상태로 처리
        Fruit fruit2 = fruitRepository.findFruitById(id);
        log.info("after : {}", fruit2.getPrice());

        if(fruit1.getPrice() != fruit2.getPrice()) {
            throw new RuntimeException("다르다");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updatePrice_READCOMMITED(Long id, int updatePrice) {
        Fruit fruit = fruitRepository.findFruitById(id);

        fruit.updatePrice(updatePrice);
    }
  ``` 
조회 부분은 과일이라는 객체에 대해 두 번의 동일한 조회요청이 이루어진다. 조회 후 과일의 가격 값이 다르면 RuntimeException이 발생되도록 만들었다. 
그리고 과일 객체의 상태값을 업데이트 할 수 있는 서비스 기능도 제공하여 두 개의 트랜잭션이 발생했을 때 Nonrepeatable현상이 발생하는지 
확인 할 수 있다. 

  ```java
   @Test
    public void readCommitted() throws Exception {
            int threadCount = 50;
            ExecutorService selectorExecutor = Executors.newFixedThreadPool(10);
            ExecutorService updatorExecutor = Executors.newFixedThreadPool(10);

            CountDownLatch countDownLatch = new CountDownLatch(threadCount);
            CountDownLatch countDownLatch1 = new CountDownLatch(threadCount);

            AtomicInteger success = new AtomicInteger();
            AtomicInteger fail = new AtomicInteger();

            for (int i = 0; i < threadCount; i++) {
                updatorExecutor.execute(() -> {
                    service.updatePrice_READCOMMITED(fruit.getId(), 300);
                    countDownLatch1.countDown();
                });
                selectorExecutor.execute(() -> {
                    try {
                        service.readCommitted(fruit.getId());
                        success.incrementAndGet();
                    }catch (Exception e) {
                        fail.incrementAndGet();
                    }
                    countDownLatch.countDown();
                });
            }
            countDownLatch.await();
            countDownLatch1.await();

            assertThat(success.get()).isNotEqualTo(threadCount);
    }
 ```

조회요청을 하는 쓰레드와 내용을 업데이트 쓰레드를 실행시켜 fruit의 가격이 다를 경우 RuntimeException이 발생되어 AtomicInteger의 fail값이 증가되고,
성공할 경우 success값이 증가하는 방식으로 테스트 환경을 만들었다. 

Nonrepeatable 현상이 발생하여 조회요청을 하는 쓰레드 갯수와 성공갯수가 달라야 테스트가 성공한다. 두 트랜잭션이 커밋과 업데이트 시점이 상호 영향이 안가는 범위내에서 
운이 좋게 Nonrepeatable 현상이 발생이 안 될 수도 있기에 조회 요청 Thread Count값을 되도록이면 크게 잡는 것이 좋다.  

![readCommitted 테스트 성공](../transaction-isolevel/readcommited.png)
(테스트 통과)

  3. Repeatable Read  
  하나의 트랜잭션이 진행되는 동안은 같은 데이터를 읽게 해줄 수 있도록 보장한다. 
  트랜잭션을 버젼관리를 함으로써 읽는 시점에 특정 버전에 해당되는 레코드를 읽게 된다. 
  `MVCC(Multi Version Concurrency Content)`라고도 하며, 하나의 레코드에 대해서 여러 버전을 관리한다.

  ```
    version1(현재 저장되어있는 데이터)     version2 (트랜잭션B에서 업데이트 후 저장되는 데이터)
    +---------------------+          +---------------------+
    | ID | name   | price |          | ID | name   | price |
    +---------------------+          +---------------------+  
    | 1  | apple  | 500   |          | 1  | apple  | 500   | 
    +---------------------+          +---------------------+  
    | 2  | banana | 1000  |          | 2  | banana | 2000  | 
    +---------------------+          +---------------------+ 
   
    +-------+               +---+                 +------+
    |트랜잭션A|               | DB |                |트랜잭션B|
    +-------+               +---+                 +------+
       |                      |                       |
       | select price         |                       |
       | from product         |                       |
       | where name = banana  |  update product       |
       |  ---- verson1 ----+  |  set price = 2000     |
       |                   |  |  where name = banana  |
       |  <----- 1000------+  |  +------------------  | 
       |                      |  |                    |
       |                      |  +--commit version2-> | 
       |                      |                       |
       |                      |                       |
       |                      |                       |
       |  select price        |  select price         |
       |  from product        |  from product         |
       |  where name = banana |  where name = banana  |
       |  ----- verson1 ----+ |  +---- verison2 ----- |
       |                    | | |                     |
       |  <------ 1000 -----+ |  +----- 2000 -------> |
       |                      |                       |
  ```
  버전관리를 함으로써 동일한 쿼리에 동일한 데이터를 읽을 수 있도록 보장한다. 
  하지만, 동일한 레코드를 여러 트랜잭션에서 수정할 때 문제가 발생할 수 있다. 

  ```
    현재 저장되어있는 DB
    +-------------------------+
    | name   | totalInvestor  | 
    +-------------------------+
    |   A    |        1       |
    +-------------------------+ 

    +-------+               +---+                 +------+
    |트랜잭션A|               | DB |                |트랜잭션B|
    +-------+               +---+                 +------+
      |                      |                       |
      | select totalInvestor |                       |
      | from product         |                       |
      | where name = A       |  select totalInvestor |
      |  -----------------+  |  from product         |
      |                   |  |  where name = A       |
      |  <-----  1  ------+  |  +------------------  | 
      |                      |  |                    |
      |                      |  +------ 1 ------->   | 
      |                      |                       |
      | update product       |                       |
      | set totalInvestor= 2 |                       |
      | where name = A       |                       |
      |  ------------------+ |  update product       |
      |                    | |  set totalInvestor= 2 |
      |                    | |  where name = A       |
      |                    | |  +------------------- |
      |                    | |  |                    |
      | <------ commit ----+ |  |                    |
      |                      |  |                    |  
      |                      |  |                    |
      |                      |  +----- commit ------>|
      |                      |                       |
  ```
  트랜잭션B에서 기대값은 3이지만 2가 되는 문제가 발생할 수 있다. 
  
  4. Serializable  
  트랜잭션이 서로 완전히 격리되는 가장 높은 단계이다. 
  다른 트랜잭션에서 수정 중인 데이터를 읽거나 수정할 수 없으며, 다른 트랜잭션B에서 
  조회중인 데이터조차도 읽거나 수정할 수 없다. 

  해당 격리수준은 특정 트랜잭션이 종료될때까지 잠금을 보유한다.

[Refference]
- https://www.youtube.com/watch?v=poyjLx-LOEU
- https://nesoy.github.io/articles/2019-05/Database-Transaction-isolation


