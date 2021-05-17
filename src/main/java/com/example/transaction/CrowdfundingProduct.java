package com.example.transaction;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigInteger;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder @Getter @ToString
public class CrowdfundingProduct {

    @Id @GeneratedValue
    private Long id;
    private String productName;
    private BigInteger currentAmount;
    private int totalInvestor;

    public void invest(BigInteger investmentAmount) {
        this.currentAmount = currentAmount.add(investmentAmount);
        this.totalInvestor++;
    }
}
