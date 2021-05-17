package com.example.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface FruitRepository2 extends JpaRepository<Fruit, Long> {

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    Fruit findFruitById(Long id);

}
