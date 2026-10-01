package com.runmate.server.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;
import com.runmate.server.auth.dto.AuthDtos.TermConsentRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.mapper.PhoneVerificationMapper;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.terms.domain.Term;
import com.runmate.server.terms.domain.UserTermAgreement;
import com.runmate.server.terms.mapper.UserTermAgreementMapper;
import com.runmate.server.terms.service.TermConsentService;
import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.SocialProvider;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.SocialAccountMapper;
import com.runmate.server.user.mapper.UserMapper;
import com.runmate.server.user.service.ProfileImageUrlResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialSignupService {
    private final UserMapper userMapper;
    private final SocialAccountMapper socialAccountMapper;
    private final PhoneVerificationMapper phoneVerificationMapper;
    private final UserTermAgreementMapper agreementMapper;
    private final TermConsentService termConsentService;
    private final RejoinBlockService rejoinBlockService;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    @Transactional
    public RunmatePrincipal signup(
            SocialProvider provider,
            String providerUserId,
            String profileImageUrl,
            String nickname,
            Long phoneVerificationId,
            List<TermConsentRequest> termConsents,
            String ipAddress,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();
        PhoneVerification verification = phoneVerificationMapper
                .findById(phoneVerificationId)
                .orElseThrow(() -> new AuthException(
                        "휴대전화 인증 정보를 찾을 수 없습니다."
                ));

        validatePhoneVerification(verification, now);
        rejoinBlockService.requireRejoinAllowed(
                verification.getPhoneNumber(),
                now
        );
        requireUnlinkedProviderAccount(
                provider,
                providerUserId
        );

        List<Term> terms = termConsentService.validate(
                termConsents,
                now
        );
        User user = userMapper
                .findByPhoneNumber(verification.getPhoneNumber())
                .map(existing -> requireLinkable(existing, provider))
                .orElseGet(() -> createUser(nickname, verification));

        socialAccountMapper.save(SocialAccount.create(
                user,
                provider,
                providerUserId,
                now
        ));
        agreementMapper.saveAll(terms.stream()
                .map(term -> UserTermAgreement.createAgreement(
                        user,
                        term,
                        ipAddress,
                        userAgent
                ))
                .toList());
        verification.consume(now);
        phoneVerificationMapper.update(verification);
        user.recordLoginSuccess(now);
        userMapper.update(user);

        return new RunmatePrincipal(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname(),
                user.getRole().name(),
                profileImageUrlResolver.resolve(user, profileImageUrl)
        );
    }

    private void requireUnlinkedProviderAccount(
            SocialProvider provider,
            String providerUserId
    ) {
        if (socialAccountMapper.findByProviderAndProviderUserId(
                provider,
                providerUserId
        ).isPresent()) {
            throw new AuthException(
                    "이미 연결된 " + providerLabel(provider) + " 계정입니다."
            );
        }
    }

    private User requireLinkable(
            User user,
            SocialProvider provider
    ) {
        String providerLabel = providerLabel(provider);
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(
                    "이용이 정지된 계정에는 " + providerLabel + " 계정을 연결할 수 없습니다."
            );
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException(
                    "탈퇴한 계정에는 " + providerLabel + " 계정을 연결할 수 없습니다."
            );
        }
        if (socialAccountMapper.existsByUserAndProvider(user, provider)) {
            throw new AuthException(
                    "이미 다른 " + providerLabel + " 계정이 연결된 회원입니다."
            );
        }
        return user;
    }

    private User createUser(
            String requestedNickname,
            PhoneVerification verification
    ) {
        String nickname = requestedNickname == null
                ? ""
                : requestedNickname.trim();
        int nicknameLength = nickname.codePointCount(0, nickname.length());
        if (nicknameLength < 2 || nicknameLength > 20) {
            throw new AuthException("닉네임은 2~20자로 입력해 주세요.");
        }
        if (userMapper.existsByNickname(nickname)) {
            throw new AuthException("이미 사용 중인 닉네임입니다.");
        }
        return userMapper.save(User.createSocial(
                verification.getPhoneNumber(),
                nickname,
                verification.getVerifiedAt()
        ));
    }

    private void validatePhoneVerification(
            PhoneVerification verification,
            LocalDateTime now
    ) {
        if (verification.getPurpose() != PhoneVerificationPurpose.SIGNUP) {
            throw new AuthException("회원가입용 휴대전화 인증이 아닙니다.");
        }
        if (verification.getConsumedAt() != null) {
            throw new AuthException("이미 사용된 휴대전화 인증입니다.");
        }
        if (verification.getVerifiedAt() == null) {
            throw new AuthException("휴대전화 인증을 완료해 주세요.");
        }
        if (!verification.getVerifiedAt().plusMinutes(30).isAfter(now)) {
            throw new AuthException(
                    "휴대전화 인증 완료 후 30분이 지났습니다. 다시 인증해 주세요."
            );
        }
    }

    private String providerLabel(SocialProvider provider) {
        return switch (provider) {
            case KAKAO -> "카카오";
            case NAVER -> "네이버";
        };
    }
}
