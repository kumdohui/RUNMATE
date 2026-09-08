package com.runmate.server.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.EmailVerification;
import com.runmate.server.auth.domain.EmailVerificationPurpose;
import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;
import com.runmate.server.auth.dto.SignupRequest;
import com.runmate.server.auth.dto.SignupResponse;
import com.runmate.server.auth.exception.SignupException;
import com.runmate.server.auth.repository.EmailVerificationRepository;
import com.runmate.server.auth.repository.PhoneVerificationRepository;
import com.runmate.server.terms.domain.Term;
import com.runmate.server.terms.domain.UserTermAgreement;
import com.runmate.server.terms.repository.TermRepository;
import com.runmate.server.terms.repository.UserTermAgreementRepository;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(
            SignupRequest request,
            String ipAddress,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();

        EmailVerification emailVerification = emailVerificationRepository
                .findById(request.emailVerificationId())
                .orElseThrow(() -> new SignupException(
                        "요청한 이메일 인증 정보를 확인할 수 없습니다."
                ));

        validateEmailVerification(emailVerification, now);

        PhoneVerification phoneVerification = phoneVerificationRepository
                .findById(request.phoneVerificationId())
                .orElseThrow(() -> new SignupException(
                        "요청한 휴대전화 인증 정보를 확인할 수 없습니다."
                ));

        validatePhoneVerification(phoneVerification, now);

        validateDuplicateUser(
                request,
                emailVerification.getEmail(),
                phoneVerification.getPhoneNumber()
        );

        List<Term> agreedTerms = validateTerms(
                request.termIds(),
                now
        );

        String passwordHash = passwordEncoder.encode(
                request.password()
        );

        User user = User.create(
                request.loginId(),
                passwordHash,
                emailVerification.getEmail(),
                phoneVerification.getPhoneNumber(),
                request.nickname(),
                emailVerification.getVerifiedAt(),
                phoneVerification.getVerifiedAt()
        );

        User savedUser = userRepository.save(user);

        List<UserTermAgreement> agreements = agreedTerms.stream()
                .map(term -> UserTermAgreement.createAgreement(
                        savedUser,
                        term,
                        ipAddress,
                        userAgent
                ))
                .toList();

        userTermAgreementRepository.saveAll(agreements);

        emailVerification.consume(now);
        phoneVerification.consume(now);

        return new SignupResponse(savedUser.getUserId());
    }

    private void validateEmailVerification(
            EmailVerification verification,
            LocalDateTime now
    ) {
        if (verification.getPurpose() != EmailVerificationPurpose.SIGNUP) {
            throw new SignupException(
                    "회원가입을 위해 요청한 이메일 인증이 아닙니다."
            );
        }

        if (verification.getConsumedAt() != null) {
            throw new SignupException(
                    "이미 회원가입에 사용된 이메일 인증입니다."
            );
        }

        if (verification.getVerifiedAt() == null) {
            throw new SignupException(
                    "이메일 인증을 완료해 주세요."
            );
        }

        if (!verification.getExpiresAt().isAfter(now)) {
            throw new SignupException(
                    "이메일 인증 시간이 만료되었습니다. 다시 인증해 주세요."
            );
        }
    }

    private void validatePhoneVerification(
            PhoneVerification verification,
            LocalDateTime now
    ) {
        if (verification.getPurpose() != PhoneVerificationPurpose.SIGNUP) {
            throw new SignupException(
                    "회원가입을 위해 요청한 휴대전화 인증이 아닙니다."
            );
        }

        if (verification.getConsumedAt() != null) {
            throw new SignupException(
                    "이미 회원가입에 사용된 휴대전화 인증입니다."
            );
        }

        if (verification.getVerifiedAt() == null) {
            throw new SignupException(
                    "휴대전화 인증을 완료해 주세요."
            );
        }

        if (!verification.getExpiresAt().isAfter(now)) {
            throw new SignupException(
                    "휴대전화 인증 시간이 만료되었습니다 . 다시 인증해 주세요."
            );
        }
    }

    private void validateDuplicateUser(
            SignupRequest request,
            String email,
            String phoneNumber
    ) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new SignupException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new SignupException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new SignupException(
                    "이미 가입된 이메일입니다."
            );
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new SignupException(
                    "이미 가입된 휴대전화 번호입니다."
            );
        }
    }

    private List<Term> validateTerms(
            List<Long> termIds,
            LocalDateTime now
    ) {
        Set<Long> requestedTermIds = Set.copyOf(termIds);

        List<Term> agreedTerms = termRepository.findByTermIdIn(
                requestedTermIds.stream().toList()
        );

        if (agreedTerms.size() != requestedTermIds.size()) {
            throw new SignupException(
                    "존재하지 않는 약관이 포함되어 있습니다."
            );
        }

        boolean containsInvalidTerm = agreedTerms.stream()
                .anyMatch(term -> !term.isEffectiveAt(now));

        if (containsInvalidTerm) {
            throw new SignupException(
                    "현재 동의할 수 없는 약관이 포함되어 있습니다."
            );
        }

        Set<Long> requiredTermIds = termRepository
                .findCurrentRequiredTerms(now)
                .stream()
                .map(Term::getTermId)
                .collect(Collectors.toSet());

        if (!requestedTermIds.containsAll(requiredTermIds)) {
            throw new SignupException(
                    "필수 약관에 모두 동의해 주세요."
            );
        }

        return agreedTerms;
    }
}