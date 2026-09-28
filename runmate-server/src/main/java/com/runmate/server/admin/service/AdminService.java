package com.runmate.server.admin.service;

import com.runmate.server.admin.dto.AdminLogRow;
import com.runmate.server.admin.dto.AdminDtos.LogFeedResponse;
import com.runmate.server.admin.dto.AdminDtos.LogItem;
import com.runmate.server.admin.dto.AdminDtos.MemberFeedResponse;
import com.runmate.server.admin.dto.AdminDtos.MemberItem;
import com.runmate.server.admin.dto.AdminDtos.MemberStatusRequest;
import com.runmate.server.admin.dto.AdminDtos.ReportDecisionRequest;
import com.runmate.server.admin.dto.AdminDtos.ReportFeedResponse;
import com.runmate.server.admin.dto.AdminDtos.ReportItem;
import com.runmate.server.admin.mapper.AdminUserMapper;
import com.runmate.server.admin.mapper.AdminActionLogMapper;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.service.UserSessionService;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostStatus;
import com.runmate.server.post.mapper.CommentMapper;
import com.runmate.server.post.mapper.PostMapper;
import com.runmate.server.report.dto.AdminReportRow;
import com.runmate.server.report.mapper.ReportMapper;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserRole;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final int PAGE_SIZE = 9;

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminActionLogMapper adminActionLogMapper;
    private final UserSessionService userSessionService;

    @Transactional(readOnly = true)
    public ReportFeedResponse reports(Integer requestedPage) {
        PageWindow window = window(reportMapper.countByStatus("PENDING"), requestedPage);
        List<ReportItem> reports = window.total() == 0
                ? List.of()
                : reportMapper.searchByStatus("PENDING", window.limit(), window.offset()).stream()
                        .map(this::reportItem)
                        .toList();
        return new ReportFeedResponse(reports, window.page(), window.totalPages(), window.total());
    }

    @Transactional
    public void resolve(
            Long adminId,
            Long reportId,
            ReportDecisionRequest request,
            String ipAddress
    ) {
        AdminReportRow report = requireOpen(reportId);
        boolean hide = Boolean.TRUE.equals(request.hideTarget());
        boolean suspend = Boolean.TRUE.equals(request.suspendUser()) || ("USER".equals(report.targetType()) && hide);
        if (hide && !"USER".equals(report.targetType())) {
            hideTarget(report);
        }
        String suspendedNickname = null;
        if (suspend) {
            suspendedNickname = suspendUser(report.targetAuthorId(), adminId);
        }
        String stored = joinNote(resolveSummary(report, hide, suspendedNickname), note(request.note()));
        if (reportMapper.markProcessed(reportId, adminId, "RESOLVED", stored) == 0) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }
        logAction(adminId, "RESOLVE_REPORT", "REPORT", reportId, note(request.note()), ipAddress);
        if (hide && "POST".equals(report.targetType())) {
            logAction(adminId, "HIDE_POST", "POST", report.targetId(), note(request.note()), ipAddress);
        }
        if (hide && "COMMENT".equals(report.targetType())) {
            logAction(adminId, "HIDE_COMMENT", "COMMENT", report.targetId(), note(request.note()), ipAddress);
        }
        if (suspend) {
            logAction(adminId, "SUSPEND_USER", "USER", report.targetAuthorId(), note(request.note()), ipAddress);
        }
    }

    @Transactional
    public void reject(
            Long adminId,
            Long reportId,
            ReportDecisionRequest request,
            String ipAddress
    ) {
        AdminReportRow report = requireOpen(reportId);
        String stored = joinNote(targetName(report) + " 신고를 기각했습니다.", note(request.note()));
        if (reportMapper.markProcessed(reportId, adminId, "REJECTED", stored) == 0) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }
        logAction(adminId, "RESOLVE_REPORT", "REPORT", reportId,
                note(request.note()) == null ? "신고 기각" : note(request.note()), ipAddress);
    }

    @Transactional(readOnly = true)
    public MemberFeedResponse members(String requestedKeyword, Integer requestedPage) {
        String keyword = note(requestedKeyword);
        PageWindow window = window(adminUserMapper.count(keyword), requestedPage);
        List<MemberItem> users = window.total() == 0
                ? List.of()
                : adminUserMapper.search(keyword, window.limit(), window.offset()).stream()
                        .map(row -> new MemberItem(row.userId(), row.loginId(), row.nickname(), row.role(),
                                row.status(), row.createdAt()))
                        .toList();
        return new MemberFeedResponse(users, window.page(), window.totalPages(), window.total());
    }

    @Transactional
    public void changeMemberStatus(
            Long adminId,
            Long userId,
            MemberStatusRequest request,
            String ipAddress
    ) {
        String next = request.status().trim().toUpperCase();
        if (!"ACTIVE".equals(next) && !"SUSPENDED".equals(next)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "변경할 상태를 선택해 주세요.");
        }
        User user = requireManageable(userId, adminId);
        if (user.getStatus().name().equals(next)) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 같은 상태입니다.");
        }
        if (adminUserMapper.updateStatus(userId, next) == 0) {
            throw new AuthException(HttpStatus.CONFLICT, "회원 상태를 바꾸지 못했습니다.");
        }
        if (UserStatus.SUSPENDED.name().equals(next)) {
            userSessionService.invalidateAll(userId);
        }
        String actionType = UserStatus.SUSPENDED.name().equals(next)
                ? "SUSPEND_USER"
                : "UNSUSPEND_USER";
        logAction(adminId, actionType, "USER", userId, note(request.note()), ipAddress);
    }

    @Transactional(readOnly = true)
    public LogFeedResponse logs(Integer requestedPage) {
        PageWindow window = window(adminActionLogMapper.countAll(), requestedPage);
        List<LogItem> logs = window.total() == 0
                ? List.of()
                : adminActionLogMapper.searchAll(window.limit(), window.offset()).stream()
                        .map(row -> new LogItem(row.logId(), row.adminNickname(), displaySummary(row),
                                memoOnly(row.note()), row.createdAt(), row.status(), row.targetType(),
                                row.targetId(),
                                row.targetLabel() == null || row.targetLabel().isBlank() ? "삭제된 대상" : row.targetLabel(),
                                row.reason(), row.detail(), row.reporterNickname(), row.targetNickname(),
                                row.targetStatus()))
                        .toList();
        return new LogFeedResponse(logs, window.page(), window.totalPages(), window.total());
    }

    private AdminReportRow requireOpen(Long reportId) {
        AdminReportRow report = reportMapper.findAdminById(reportId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."));
        if (!"PENDING".equals(report.status()) && !"REVIEWING".equals(report.status())) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");
        }
        return report;
    }

    private void hideTarget(AdminReportRow report) {
        if ("POST".equals(report.targetType())) {
            Post post = postMapper.findById(report.targetId()).orElse(null);
            if (post == null || post.getDeletedAt() != null || post.getStatus() == PostStatus.DELETED
                    || post.getStatus() == PostStatus.HIDDEN) {
                return;
            }
            post.hide();
            postMapper.update(post);
            return;
        }
        if ("COMMENT".equals(report.targetType())) {
            commentMapper.markDeleted(report.targetId());
        }
    }

    private String suspendUser(Long userId, Long adminId) {
        User user = requireManageable(userId, adminId);
        if (adminUserMapper.updateStatus(userId, UserStatus.SUSPENDED.name()) == 0) {
            throw new AuthException(HttpStatus.CONFLICT, "회원을 정지하지 못했습니다.");
        }
        userSessionService.invalidateAll(userId);
        return user.getNickname();
    }

    private User requireManageable(Long userId, Long adminId) {
        if (userId == null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
        if (userId.equals(adminId)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "본인 계정은 바꿀 수 없습니다.");
        }
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (user.getRole() == UserRole.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "관리자 계정은 바꿀 수 없습니다.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "탈퇴한 회원은 바꿀 수 없습니다.");
        }
        return user;
    }

    private String resolveSummary(AdminReportRow report, boolean hide, String suspendedNickname) {
        StringBuilder summary = new StringBuilder(targetName(report)).append(" 신고를 처리했습니다.");
        if (hide && "POST".equals(report.targetType())) {
            summary.append(" 게시글을 숨겼습니다.");
        }
        if (hide && "COMMENT".equals(report.targetType())) {
            summary.append(" 댓글을 숨겼습니다.");
        }
        if (suspendedNickname != null && !suspendedNickname.isBlank()) {
            summary.append(' ').append(suspendedNickname).append(" 회원을 정지했습니다.");
        }
        return summary.toString();
    }

    private String memoOnly(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String rest = raw.trim()
                .replaceFirst("^(게시글|댓글|회원) 신고를 (처리|기각)했습니다\\.", "")
                .replaceFirst("\\s*게시글을 숨겼습니다\\.", "")
                .replaceFirst("\\s*댓글을 숨겼습니다\\.", "")
                .replaceFirst("\\s*\\S+ 회원을 정지했습니다\\.", "")
                .replaceFirst("\\s*회원을 정지했습니다\\.", "")
                .trim();
        return rest.isEmpty() ? null : rest;
    }

    private String displaySummary(AdminLogRow row) {
        String summary = row.summary() == null || row.summary().isBlank()
                ? "신고를 처리했습니다."
                : row.summary();
        String nickname = row.targetNickname();
        if (nickname == null || nickname.isBlank() || summary.contains(nickname)) {
            return summary;
        }
        return summary.replace("회원을 정지했습니다.", nickname + " 회원을 정지했습니다.");
    }

    private String targetName(AdminReportRow report) {
        return switch (report.targetType()) {
            case "POST" -> "게시글";
            case "COMMENT" -> "댓글";
            default -> "회원";
        };
    }

    private void logAction(
            Long adminId,
            String actionType,
            String targetType,
            Long targetId,
            String reason,
            String ipAddress
    ) {
        if (targetId == null) {
            return;
        }
        adminActionLogMapper.insert(
                adminId,
                actionType,
                targetType,
                targetId,
                reason,
                trimIp(ipAddress)
        );
    }

    private String trimIp(String ipAddress) {
        String value = note(ipAddress);
        if (value == null) {
            return null;
        }
        return value.length() <= 45 ? value : value.substring(0, 45);
    }

    private ReportItem reportItem(AdminReportRow row) {
        return new ReportItem(row.reportId(), row.reporterNickname(), row.targetType(), row.targetId(),
                row.targetLabel() == null || row.targetLabel().isBlank() ? "삭제된 대상" : row.targetLabel(),
                row.reason(), row.detail(), row.status(), row.createdAt());
    }

    private String joinNote(String summary, String note) {
        if (note == null) {
            return summary;
        }
        return summary + " " + note;
    }

    private String note(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PageWindow window(long total, Integer requestedPage) {
        int page = Math.max(requestedPage == null ? 0 : requestedPage, 0);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        return new PageWindow(page, totalPages, total, PAGE_SIZE, page * PAGE_SIZE);
    }

    private record PageWindow(int page, int totalPages, long total, int limit, int offset) {
    }
}
