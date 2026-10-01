package com.runmate.server.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.domain.EmailVerification;
import com.runmate.server.auth.domain.EmailVerificationPurpose;
import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;
import com.runmate.server.auth.dto.AuthDtos.SignupRequest;
import com.runmate.server.auth.dto.AuthDtos.SignupResponse;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.mapper.EmailVerificationMapper;
import com.runmate.server.auth.mapper.PhoneVerificationMapper;
import com.runmate.server.terms.domain.Term;
import com.runmate.server.terms.domain.UserTermAgreement;
import com.runmate.server.terms.mapper.UserTermAgreementMapper;
import com.runmate.server.terms.service.TermConsentService;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.UserStatus;
import com.runmate.server.user.mapper.UserMapper;

@Service
public class SignupService {

    private final UserMapper userMapper;
    private final EmailVerificationMapper emailVerificationMapper;
    private final PhoneVerificationMapper phoneVerificationMapper;
    private final UserTermAgreementMapper userTermAgreementMapper;
    private final TermConsentService termConsentService;
    private final RejoinBlockService rejoinBlockService;
    private final PasswordEncoder passwordEncoder;

    public SignupService(
            UserMapper userMapper,
            EmailVerificationMapper emailVerificationMapper,
            PhoneVerificationMapper phoneVerificationMapper,
            UserTermAgreementMapper userTermAgreementMapper,
            TermConsentService termConsentService,
            RejoinBlockService rejoinBlockService,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.emailVerificationMapper = emailVerificationMapper;
        this.phoneVerificationMapper = phoneVerificationMapper;
        this.userTermAgreementMapper = userTermAgreementMapper;
        this.termConsentService = termConsentService;
        this.rejoinBlockService = rejoinBlockService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(
            SignupRequest request,
            String ipAddress,
            String userAgent
    ) {
        LocalDateTime now = LocalDateTime.now();

        EmailVerification emailVerification = emailVerificationMapper
                .findById(request.emailVerificationId())
                .orElseThrow(() -> new AuthException(
                        "요청한 이메일 인증 정보를 확인할 수 없습니다."
                ));

        validateEmailVerification(emailVerification, now);

        PhoneVerification phoneVerification = phoneVerificationMapper
                .findById(request.phoneVerificationId())
                .orElseThrow(() -> new AuthException(
                        "요청한 휴대전화 인증 정보를 확인할 수 없습니다."
                ));

        validatePhoneVerification(phoneVerification, now);
        rejoinBlockService.requireRejoinAllowed(
                phoneVerification.getPhoneNumber(),
                now
        );

        validateUniqueLocalCredentials(request.loginId(), emailVerification.getEmail());

        List<Term> agreedTerms = termConsentService.validate(
                request.termConsents(),
                now
        );

        String passwordHash = passwordEncoder.encode(
                request.password()
        );

        Optional<User> existingUser = userMapper
                .findByPhoneNumber(phoneVerification.getPhoneNumber());
        User savedUser = existingUser
                .map(user -> addLocalLogin(
                        user,
                        request,
                        passwordHash,
                        emailVerification
                ))
                .orElseGet(() -> createNewUser(
                        request,
                        passwordHash,
                        emailVerification,
                        phoneVerification
                ));

        List<UserTermAgreement> agreements = agreedTerms.stream()
                .map(term -> UserTermAgreement.createAgreement(
                        savedUser,
                        term,
                        ipAddress,
                        userAgent
                ))
                .toList();

        userTermAgreementMapper.saveAll(agreements);

        emailVerification.consume(now);
        phoneVerification.consume(now);
        phoneVerificationMapper.update(phoneVerification);

        return new SignupResponse(savedUser.getUserId(), existingUser.isPresent());
    }

    private void validateEmailVerification(
            EmailVerification verification,
            LocalDateTime now
    ) {
        if (verification.getPurpose() != EmailVerificationPurpose.SIGNUP) {
            throw new AuthException(
                    "회원가입을 위해 요청한 이메일 인증이 아닙니다."
            );
        }

        if (verification.getConsumedAt() != null) {
            throw new AuthException(
                    "이미 회원가입에 사용된 이메일 인증입니다."
            );
        }

        if (verification.getVerifiedAt() == null) {
            throw new AuthException(
                    "이메일 인증을 완료해 주세요."
            );
        }

        if (!verification.getVerifiedAt().plusMinutes(30).isAfter(now)) {
            throw new AuthException(
                    "이메일 인증 완료 후 30분이 지났습니다. 다시 인증해 주세요."
            );
        }
    }

    private void validatePhoneVerification(
            PhoneVerification verification,
            LocalDateTime now
    ) {
        if (verification.getPurpose() != PhoneVerificationPurpose.SIGNUP) {
            throw new AuthException(
                    "회원가입을 위해 요청한 휴대전화 인증이 아닙니다."
            );
        }

        if (verification.getConsumedAt() != null) {
            throw new AuthException(
                    "이미 회원가입에 사용된 휴대전화 인증입니다."
            );
        }

        if (verification.getVerifiedAt() == null) {
            throw new AuthException(
                    "휴대전화 인증을 완료해 주세요."
            );
        }

        if (!verification.getVerifiedAt().plusMinutes(30).isAfter(now)) {
            throw new AuthException(
                    "휴대전화 인증 완료 후 30분이 지났습니다. 다시 인증해 주세요."
            );
        }
    }

    private void validateUniqueLocalCredentials(String loginId, String email) {
        if (userMapper.existsByLoginId(loginId)) {
            throw new AuthException(
                    "이미 사용 중인 아이디입니다."
            );
        }
        if (userMapper.existsByEmail(email)) {
            throw new AuthException(
                    "이미 가입된 이메일입니다."
            );
        }
    }

    private User addLocalLogin(
            User user,
            SignupRequest request,
            String passwordHash,
            EmailVerification emailVerification
    ) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("현재 일반 로그인을 추가할 수 없는 계정입니다.");
        }
        if (user.getLoginId() != null || user.getPasswordHash() != null) {
            throw new AuthException(
                    "이미 일반 로그인이 등록된 계정입니다. 로그인 또는 계정 찾기를 이용해 주세요."
            );
        }
        if (!request.linkExistingAccount()) {
            throw new AuthException(
                    "기존 계정에 일반 로그인을 추가하려면 계정 연결에 동의해 주세요."
            );
        }

        String requestedNickname = request.nickname().trim();
        if (!requestedNickname.equals(user.getNickname())
                && userMapper.existsByNickname(requestedNickname)) {
            throw new AuthException("이미 사용 중인 닉네임입니다.");
        }

        user.addLocalLogin(
                request.loginId(),
                passwordHash,
                emailVerification.getEmail(),
                emailVerification.getVerifiedAt()
        );
        user.changeNickname(requestedNickname);
        return user;
    }

    private User createNewUser(
            SignupRequest request,
            String passwordHash,
            EmailVerification emailVerification,
            PhoneVerification phoneVerification
    ) {
        if (userMapper.existsByNickname(request.nickname())) {
            throw new AuthException("이미 사용 중인 닉네임입니다.");
        }
        User user = User.create(
                request.loginId(),
                passwordHash,
                emailVerification.getEmail(),
                phoneVerification.getPhoneNumber(),
                request.nickname(),
                emailVerification.getVerifiedAt(),
                phoneVerification.getVerifiedAt()
        );
        return userMapper.save(user);
    }

}
