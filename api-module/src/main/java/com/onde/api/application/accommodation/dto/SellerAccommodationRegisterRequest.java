package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class SellerAccommodationRegisterRequest {
    private Long sellerId;
    private String name;
    private String description;
    private String category;
    private String location;
    private String businessLicense;
    private String thumbnailUrl;
    private List<RoomRegisterRequest> rooms;

    @Getter @Setter
    public static class RoomRegisterRequest {
        private String name;
        private Integer capacity;
        private Integer baseCapacity;
        private BigDecimal extraPersonFee;
    }
}
