package com.runmate.server.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.runmate.server.admin.dto.AdminDtos.ReportFeedResponse;
import com.runmate.server.admin.service.AdminService;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    void blocksRegularMember() throws Exception {
        mockMvc.perform(get("/api/admin/reports")
                        .with(authentication(authToken("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdministrator() throws Exception {
        when(adminService.reports(null))
                .thenReturn(new ReportFeedResponse(List.of(), 0, 0, 0));

        mockMvc.perform(get("/api/admin/reports")
                        .with(authentication(authToken("ADMIN"))))
                .andExpect(status().isOk());
    }

    private UsernamePasswordAuthenticationToken authToken(String role) {
        RunmatePrincipal principal = new RunmatePrincipal(
                1L,
                "tester",
                "테스터",
                role,
                null
        );
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
