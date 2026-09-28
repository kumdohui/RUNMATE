package com.runmate.server.admin.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.runmate.server.admin.dto.AdminDtos.MemberStatusRequest;
import com.runmate.server.admin.mapper.AdminActionLogMapper;
import com.runmate.server.admin.mapper.AdminUserMapper;
import com.runmate.server.auth.service.UserSessionService;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.report.mapper.ReportMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserRole;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminServiceTest {
    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
    private final AdminActionLogMapper actionLogMapper = mock(AdminActionLogMapper.class);
    private final UserSessionService sessionService = mock(UserSessionService.class);

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                reportMapper,
                postMapper,
                commentMapper,
                userMapper,
                adminUserMapper,
                actionLogMapper,
                sessionService
        );
    }

    @Test
    void suspendingMemberInvalidatesSessionsAndWritesAuditLog() {
        User member = member(UserStatus.ACTIVE);
        when(userMapper.findById(8L)).thenReturn(Optional.of(member));
        when(adminUserMapper.updateStatus(8L, "SUSPENDED")).thenReturn(1);

        adminService.changeMemberStatus(
                1L,
                8L,
                new MemberStatusRequest("SUSPENDED", "반복 신고"),
                "127.0.0.1"
        );

        verify(sessionService).invalidateAll(8L);
        verify(actionLogMapper).insert(
                1L,
                "SUSPEND_USER",
                "USER",
                8L,
                "반복 신고",
                "127.0.0.1"
        );
    }

    @Test
    void reactivatingMemberWritesAuditLogWithoutInvalidatingSessions() {
        User member = member(UserStatus.SUSPENDED);
        when(userMapper.findById(8L)).thenReturn(Optional.of(member));
        when(adminUserMapper.updateStatus(8L, "ACTIVE")).thenReturn(1);

        adminService.changeMemberStatus(
                1L,
                8L,
                new MemberStatusRequest("ACTIVE", "이의 신청 승인"),
                "127.0.0.1"
        );

        verify(sessionService, never()).invalidateAll(8L);
        verify(actionLogMapper).insert(
                1L,
                "UNSUSPEND_USER",
                "USER",
                8L,
                "이의 신청 승인",
                "127.0.0.1"
        );
    }

    private User member(UserStatus status) {
        User member = mock(User.class);
        when(member.getRole()).thenReturn(UserRole.USER);
        when(member.getStatus()).thenReturn(status);
        return member;
    }
}
