package com.runmate.server.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;
import com.runmate.server.auth.dto.KakaoPendingProfile;
import com.runmate.server.auth.dto.AuthDtos.KakaoSignupRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.repository.PhoneVerificationRepository;
import com.runmate.server.auth.security.RunmatePrincipal;
import com.runmate.server.terms.domain.Term;
import com.runmate.server.terms.domain.UserTermAgreement;
import com.runmate.server.terms.repository.TermRepository;
import com.runmate.server.terms.repository.UserTermAgreementRepository;
import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.repository.SocialAccountRepository;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoSignupService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final TermRepository termRepository;
    private final UserTermAgreementRepository agreementRepository;

    @Transactional
    public RunmatePrincipal signup(
            KakaoPendingProfile profile,
            KakaoSignupRequest request,
            String ipAddress,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();
        PhoneVerification phoneVerification = phoneVerificationRepository
                .findById(request.phoneVerificationId())
                .orElseThrow(() -> new AuthException("휴대전화 인증 정보를 찾을 수 없습니다."));

        validatePhoneVerification(phoneVerification, now);
        if (socialAccountRepository.findByProviderAndProviderUserId(
                com.runmate.server.user.domain.SocialProvider.KAKAO,
                profile.providerUserId()
        ).isPresent()) {
            throw new AuthException("이미 연결된 카카오 계정입니다.");
        }

        List<Term> terms = validateTerms(request.termIds(), now);
        User user = userRepository
                .findByPhoneNumber(phoneVerification.getPhoneNumber())
                .map(this::linkableExistingUser)
                .orElseGet(() -> createSocialUser(request, phoneVerification));

        socialAccountRepository.save(SocialAccount.createKakao(
                user,
                profile.providerUserId(),
                now
        ));
        agreementRepository.saveAll(terms.stream()
                .map(term -> UserTermAgreement.createAgreement(user, term, ipAddress, userAgent))
                .toList());
        phoneVerification.consume(now);

        user.recordLoginSuccess(now);

        return new RunmatePrincipal(
                user.getUserId(),
                user.getLoginId(),
                user.getNickname(),
                user.getRole().name(),
                user.getProfileImageUrl() == null
                        ? profile.profileImageUrl()
                        : user.getProfileImageUrl()
        );
    }

    private User linkableExistingUser(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException("이용이 정지된 계정에는 카카오 계정을 연결할 수 없습니다.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException("탈퇴한 계정에는 카카오 계정을 연결할 수 없습니다.");
        }
        if (socialAccountRepository.existsByUserAndProvider(
                user,
                com.runmate.server.user.domain.SocialProvider.KAKAO
        )) {
            throw new AuthException("이미 다른 카카오 계정이 연결된 회원입니다.");
        }
        return user;
    }

    private User createSocialUser(
            KakaoSignupRequest request,
            PhoneVerification phoneVerification
    ) {
        if (userRepository.existsByNickname(request.nickname())) {
            throw new AuthException("이미 사용 중인 닉네임입니다.");
        }
        return userRepository.save(User.createSocial(
                phoneVerification.getPhoneNumber(),
                request.nickname().trim(),
                phoneVerification.getVerifiedAt()
        ));
    }

    private void validatePhoneVerification(PhoneVerification verification, LocalDateTime now) {
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
            throw new AuthException("휴대전화 인증 완료 후 30분이 지났습니다. 다시 인증해 주세요.");
        }
    }

    private List<Term> validateTerms(List<Long> termIds, LocalDateTime now) {
        Set<Long> requestedIds = Set.copyOf(termIds);
        List<Term> terms = termRepository.findByTermIdIn(requestedIds.stream().toList());
        if (terms.size() != requestedIds.size() || terms.stream().anyMatch(term -> !term.isEffectiveAt(now))) {
            throw new AuthException("현재 동의할 수 없는 약관이 포함되어 있습니다.");
        }
        Set<Long> requiredIds = termRepository.findCurrentRequiredTerms(now).stream()
                .map(Term::getTermId)
                .collect(Collectors.toSet());
        if (!requestedIds.containsAll(requiredIds)) {
            throw new AuthException("필수 약관에 모두 동의해 주세요.");
        }
        return terms;
    }
}
