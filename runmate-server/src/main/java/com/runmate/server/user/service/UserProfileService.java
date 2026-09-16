package com.runmate.server.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.domain.SocialAccount;
import com.runmate.server.user.domain.SocialProvider;
import com.runmate.server.user.dto.UserProfileDtos.ProfileResponse;
import com.runmate.server.user.dto.UserProfileDtos.ChangePasswordRequest;
import com.runmate.server.user.dto.UserProfileDtos.WithdrawRequest;
import com.runmate.server.user.dto.UserProfileDtos.UpdateProfileRequest;
import com.runmate.server.user.repository.SocialAccountRepository;
import com.runmate.server.user.repository.UserRepository;
import com.runmate.server.common.storage.S3ImageStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ProfileImageUrlResolver profileImageUrlResolver;
    private final PasswordEncoder passwordEncoder;
    private final S3ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public ProfileResponse get(Long userId) { return response(findUser(userId)); }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        String nickname = request.nickname().trim();
        int length = nickname.codePointCount(0, nickname.length());
        if (length < 2 || length > 20) throw new AuthException("닉네임은 2~20자로 입력해 주세요.");
        userRepository.findByNickname(nickname)
                .filter(owner -> !owner.getUserId().equals(userId))
                .ifPresent(owner -> { throw new AuthException("이미 사용 중인 닉네임입니다."); });
        String bio = request.bio() == null ? null : request.bio().trim();
        user.changeNickname(nickname);
        user.changeBio(bio == null || bio.isBlank() ? null : bio);
        return response(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (user.getPasswordHash() == null) throw new AuthException("일반 로그인이 연결되지 않은 계정입니다.");
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AuthException("현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }
        user.resetPassword(passwordEncoder.encode(request.newPassword()), LocalDateTime.now());
    }

    @Transactional
    public ProfileResponse disconnectSocialAccount(Long userId, String providerName) {
        User user = findUser(userId);
        SocialProvider provider;
        try {
            provider = SocialProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AuthException("지원하지 않는 소셜 로그인입니다.");
        }

        SocialAccount account = socialAccountRepository.findByUserAndProvider(user, provider)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "연결된 소셜 계정을 찾을 수 없습니다."));
        int loginMethodCount = socialAccountRepository.findByUser(user).size()
                + (user.getLoginId() != null && user.getPasswordHash() != null ? 1 : 0);
        if (loginMethodCount <= 1) {
            throw new AuthException("마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 방법을 먼저 연결해 주세요.");
        }

        socialAccountRepository.delete(account);
        socialAccountRepository.flush();
        return response(user);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findUser(userId);
        if (!"회원 탈퇴".equals(request.confirmation().trim())) {
            throw new AuthException("확인란에 '회원 탈퇴'를 정확히 입력해 주세요.");
        }
        if (user.getPasswordHash() != null) {
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new AuthException("현재 비밀번호가 일치하지 않습니다.");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        String profileImageKey = user.getProfileImageStorageKey();
        socialAccountRepository.deleteAll(socialAccountRepository.findByUser(user));
        user.anonymizeAndWithdraw(now);
        userRepository.saveAndFlush(user);
        if (profileImageKey != null && !profileImageKey.isBlank()) imageStorageService.delete(profileImageKey);
    }

    private ProfileResponse response(User user) {
        List<String> providers = socialAccountRepository.findProvidersByUserId(user.getUserId())
                .stream().map(Enum::name).toList();
        return new ProfileResponse(user.getUserId(), user.getLoginId(), user.getEmail(),
                user.getPhoneNumber(), user.getNickname(), user.getBio(),
                profileImageUrlResolver.resolve(user),
                user.getLoginId() != null && user.getPasswordHash() != null,
                providers, user.getCreatedAt());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
