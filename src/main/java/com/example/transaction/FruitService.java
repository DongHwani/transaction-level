package com.example.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Service
@Slf4j
@RequiredArgsConstructor
public class FruitService {

    private final EntityManager entityManager;
    private final FruitRepository fruitRepository;
    private final FruitRepository2 fruitRepository2;

    @Transactional(readOnly = true, timeout = 3000, isolation = Isolation.REPEATABLE_READ)
    public void repeatableRead(Long id) {
        Fruit fruit1 = fruitRepository2.findFruitById(id);
        log.info("before : {}", fruit1.getPrice());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        entityManager.detach(fruit1);
        Fruit fruit2 = fruitRepository2.findFruitById(id);
        log.info("after : {}", fruit2.getPrice());

        if(fruit1.getPrice() != fruit2.getPrice()) {
            throw new RuntimeException("다르다");
        }
    }

    @Transactional(readOnly = true)
    public void readCommitted(Long id) {
        Fruit fruit1 = fruitRepository.findFruitById(id);
        log.info("before : {}", fruit1.getPrice());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        entityManager.detach(fruit1);
        Fruit fruit2 = fruitRepository.findFruitById(id);
        log.info("after : {}", fruit2.getPrice());

        if(fruit1.getPrice() != fruit2.getPrice()) {
            throw new RuntimeException("다르다");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 5000)
    public void updatePrice_READCOMMITED(Long id, int updatePrice) {
        Fruit fruit = fruitRepository.findFruitById(id);

        fruit.updatePrice(updatePrice);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 5000)
    public void updatePrice_REPEATABLEREAD(Long id, int updatePrice) {
        Fruit fruit = fruitRepository.findFruitById(id);

        fruit.updatePrice(updatePrice);
    }

}
