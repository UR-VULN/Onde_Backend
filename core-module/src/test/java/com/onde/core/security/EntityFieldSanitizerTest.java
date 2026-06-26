package com.onde.core.security;

import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.flight.Passenger;
import com.onde.core.entity.flight.SeatClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityFieldSanitizerTest {

    @Test
    void sanitizeEntity_sanitizesStringFieldsOnly() {
        Post post = Post.builder()
                .memberId(1L)
                .title("<script>alert(1)</script>제목")
                .content("본문 & <script>bad</script>")
                .type(PostType.REVIEW)
                .status(PostStatus.ACTIVE)
                .likeCount(0)
                .commentCount(0)
                .rating(5)
                .build();

        EntityFieldSanitizer.sanitizeEntity(post);

        assertEquals("제목", post.getTitle());
        assertEquals("본문 &amp; ", post.getContent());
    }

    @Test
    void sanitizeEntity_sanitizesEmbeddedFields() {
        FlightBooking booking = FlightBooking.builder()
                .bookingCode("BK-001")
                .userId(1L)
                .passenger(Passenger.builder()
                        .passengerName("<script>alert(1)</script>홍길동")
                        .passengerPassport("M1234567")
                        .passengerBirthdate(LocalDate.of(1990, 1, 1))
                        .build())
                .seatClass(SeatClass.ECONOMY)
                .totalPrice(BigDecimal.valueOf(100000))
                .reservedUntil(LocalDateTime.now().plusHours(1))
                .build();

        EntityFieldSanitizer.sanitizeEntity(booking);

        assertEquals("홍길동", booking.getPassenger().getPassengerName());
        assertEquals("M1234567", booking.getPassenger().getPassengerPassport());
    }
}
