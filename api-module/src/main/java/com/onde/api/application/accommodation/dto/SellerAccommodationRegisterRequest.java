package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class SellerAccommodationRegisterRequest {
    private Long sellerId;
    private String name;
    private String description;
    private String region;
    private String city;
    private Integer starRating;
    private Double latitude;
    private Double longitude;
    private List<String> amenities;
    private List<RoomRegisterRequest> rooms;

    @Getter @Setter
    public static class RoomRegisterRequest {
        private String roomType;
        private Integer baseCapacity;
        private Integer maxCapacity;
        private Integer defaultPrice;
        private Integer totalQuantity;
    }
}
