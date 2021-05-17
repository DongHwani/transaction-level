package com.example.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FruitTest {

    @Autowired
    private FruitService service;

    @Autowired
    private FruitRepository fruitRepository;

    private Fruit fruit;

    @BeforeEach
    public void setup() {
        fruit = Fruit.builder()
                .name("사과")
                .price(5000)
                .build();
        fruit = fruitRepository.save(fruit);
    }

    @Test
    @DisplayName("ISOLEVEL READCOMMITTED는 Non-Repeatable현상이 발생된다.")
    public void nonRepeatableRead_WhenReadCommittedLevel() throws Exception {
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

    @Test
    @DisplayName("ISOLEVEL Repeatable READ 레벨은 Non-Repeatable 현상이 발생되지 않는다.")
    public void repeatableRead_WhenRepeatableReadLevel() throws InterruptedException {
        int threadCount = 50;
        ExecutorService selectorExecutor = Executors.newFixedThreadPool(10);
        ExecutorService updatorExecutor = Executors.newFixedThreadPool(10);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        CountDownLatch countDownLatch1 = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            updatorExecutor.execute(() -> {
                service.updatePrice_REPEATABLEREAD(fruit.getId(), 300);
                countDownLatch1.countDown();
            });
            selectorExecutor.execute(() -> {
                try {
                    service.repeatableRead(fruit.getId());
                    success.incrementAndGet();
                }catch (Exception e) {
                    e.printStackTrace();
                    fail.incrementAndGet();
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        countDownLatch1.await();

        assertThat(success.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("ISOLEVEL Repeatable READ 레벨은 PhantomRead 현상이 발생된다.")
    public void phantomRead_WhenRepeatableReadLevel() throws InterruptedException {
        //TODO
    }


}
