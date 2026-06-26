package com.onde.api.application.member;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.repository.MemberRepository;
import com.onde.core.entity.reservation.Reservation;
import com.onde.core.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Member createTestMember(Long id, MemberRole role, String email, String password) {
        Member member = Member.builder()
                .id(id)
                .authSubjectId(java.util.UUID.randomUUID().toString())
                .role(role)
                .email(email != null ? email : (role.name().toLowerCase() + (id != null ? id : "") + "@onde.com"))
                .password(password != null ? password : "testpassword123")
                .build();
        Member savedMember = memberRepository.save(member);

        // 테스트 편의성을 위해 USER(ID=1) 생성 시, 영수증 다운로드 테스트용 dummy reservation도 함께 삽입
        if (id != null && id == 1L) {
            Reservation reservation = Reservation.builder()
                    .id(1L)
                    .userId(savedMember.getId())
                    .targetType(com.onde.core.entity.reservation.ReservationTarget.ROOM)
                    .targetId(1L)
                    .checkIn(LocalDateTime.now())
                    .checkOut(LocalDateTime.now().plusDays(3))
                    .status(com.onde.core.entity.reservation.ReservationStatus.RESERVED)
                    .totalPrice(java.math.BigDecimal.valueOf(350000))
                    .build();
            reservationRepository.save(reservation);
        }

        return savedMember;
    }
}
