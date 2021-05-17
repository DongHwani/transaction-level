package com.example.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Getter @Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Fruit {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private int price;

    public void updatePrice(int updatePrice) {
        this.price = updatePrice;
    }
}
