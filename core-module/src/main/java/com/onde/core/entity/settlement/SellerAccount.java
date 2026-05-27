package com.onde.core.entity.settlement;

import jakarta.persistence.*;
import lombok.*;

/**
 * 판매자(Seller)의 정산용 은행 계좌 정보를 저장하는 엔티티입니다.
 * 정산 데이터를 승인하고 지급 처리할 때 실제 송금할 은행명과 계좌번호 정보를 조회하는 데 사용됩니다.
 */
@Entity
@Table(name = "seller_accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SellerAccount {

    /**
     * 계좌 정보 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 판매자 식별자 (FK 역할)
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * 정산용 은행명 (예: 신한은행, 국민은행 등)
     */
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    /**
     * 정산용 은행 계좌번호 (하이픈 제외 숫자 권장)
     */
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
}

