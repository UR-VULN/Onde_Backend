package com.onde.core.repository;

import com.onde.core.entity.member.Member;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// static import로 QMember를 가져옵니다. (빌드 후 에러가 사라집니다)
import static com.onde.core.entity.member.QMember.member;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Member> searchMembersAdmin(String role, String keyword, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        
        List<Member> content = queryFactory
                .selectFrom(member)
                .where(
                        roleEq(role),
                        keywordContains(keyword),
                        createdBetween(startDate, endDate)
                )
                .orderBy(member.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .where(
                        roleEq(role),
                        keywordContains(keyword),
                        createdBetween(startDate, endDate)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 동적 쿼리를 위한 BooleanExpression 메서드들 ---

    private BooleanExpression roleEq(String role) {
        // ENUM 비교를 위해 문자열을 변환합니다 (프로젝트 설정에 맞게 수정 필요)
        return StringUtils.hasText(role) ? member.role.stringValue().eq(role) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        return member.name.containsIgnoreCase(keyword)
                .or(member.email.containsIgnoreCase(keyword));
    }

    private BooleanExpression createdBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) return null;
        
        if (startDate != null && endDate == null) {
            return member.createdAt.goe(startDate.atStartOfDay());
        }
        if (startDate == null && endDate != null) {
            return member.createdAt.loe(endDate.atTime(LocalTime.MAX));
        }
        return member.createdAt.between(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }
}