package com.onde.core.entity.member;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members")
@Getter
@Setter // 👈 기존의 모든 get/set 호출을 완벽하게 지원하도록 추가
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    // [첫 번째 코드의 필드 스펙]
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 👈 두 필드가 DB의 동일한 PK 컬럼을 바라보도록 맵핑
    private Long id;

    // [두 번째 코드의 필드 스펙]
    // 다른 파일에서 member.getUserId()를 호출하는 로직이 있다면 이 필드가 지켜줍니다.
    @Column(name = "id", insertable = false, updatable = false) 
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MemberRole role;

    // [두 번째 코드의 인증 필드 스펙]
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "member_roles", 
        joinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id")
    )
    @Column(name = "role")
    @Builder.Default
    private List<String> roles = new ArrayList<>();
}