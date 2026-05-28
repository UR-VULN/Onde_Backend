package com.onde.api.application.lbs;

import com.onde.api.application.lbs.dto.PropertyRegisterRequest;
import com.onde.api.application.lbs.dto.PropertyRegisterResponse;
import com.onde.core.entity.lbs.Property;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.PropertyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    @DisplayName("매물 등록 시 좌표가 소수점 4자리 이상 정밀도가 아니면 ValidationException이 발생한다.")
    void registerProperty_invalidCoordinates() {
        // given
        PropertyRegisterRequest req = PropertyRegisterRequest.builder()
                .addressName("테헤란로 1")
                .latitude(37.5) // 소수점 1자리
                .longitude(126.97801)
                .build();

        // when & then
        assertThatThrownBy(() -> propertyService.registerProperty(req, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("좌표는 소수점 4자리 이상 정밀도여야 합니다.");
    }

    @Test
    @DisplayName("좌표가 소수점 4자리 이상 정밀도를 충족하고 회원 검증에 성공하면 매물이 정상적으로 등록된다.")
    void registerProperty_success() {
        // given
        Long sellerId = 1L;
        PropertyRegisterRequest req = PropertyRegisterRequest.builder()
                .addressName("테헤란로 1")
                .latitude(37.5665)  // 소수점 4자리
                .longitude(126.9781) // 소수점 4자리 (끝자리 0이 아닌 값으로 세팅하여 Double 절삭 방지)
                .build();

        Member seller = Member.builder()
                .id(sellerId)
                .role(MemberRole.SELLER)
                .build();

        Property mockSavedProperty = Property.builder()
                .id(15L)
                .seller(seller)
                .addressName(req.getAddressName())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .isVerified(false)
                .registeredAt(LocalDateTime.now())
                .build();

        Mockito.when(memberRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        Mockito.when(propertyRepository.save(Mockito.any(Property.class))).thenReturn(mockSavedProperty);

        // when
        PropertyRegisterResponse response = propertyService.registerProperty(req, sellerId);

        // then
        assertThat(response.getPropertyId()).isEqualTo(15L);
        assertThat(response.getAddressName()).isEqualTo("테헤란로 1");
        assertThat(response.isVerified()).isFalse();
        assertThat(response.getLatitude()).isEqualTo(37.5665);
    }
}
