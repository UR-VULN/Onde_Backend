package com.onde.api.security;

import com.onde.api.application.admin.AdminMemberService;
import com.onde.core.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminMemberService adminMemberService;

    @MockBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("인증 정보가 없을 때 401 에러와 표준 에러 응답을 반환한다")
    void testAccessDeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-001"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    @DisplayName("관리자 권한이 없는 유저가 관리자 API 접근 시 403 에러를 반환한다")
    void testAccessDeniedWithLowRole() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/members/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"GENERAL_ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-002"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
