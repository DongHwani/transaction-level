package com.example.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface FruitRepository  extends JpaRepository<Fruit, Long> {

    @Transactional(isolation = Isolation.READ_COMMITTED)
    Fruit findFruitById(Long id);

}
