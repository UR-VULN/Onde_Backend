package com.onde.core.repository;

import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.notification.DeviceType;
import com.onde.core.entity.notification.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByMemberIdAndDeviceType(Long memberId, DeviceType deviceType);

    List<FcmToken> findByMemberId(Long memberId);

    @Query("SELECT t FROM FcmToken t JOIN t.member m WHERE m.role IN :roles")
    List<FcmToken> findByMemberRoleIn(@Param("roles") List<MemberRole> roles);
}
