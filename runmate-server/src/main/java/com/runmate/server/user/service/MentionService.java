package com.runmate.server.user.service;

import com.runmate.server.user.dto.MentionCandidate;
import com.runmate.server.user.mapper.UserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MentionService {
    private static final int LIMIT = 8;

    private final UserMapper userMapper;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    @Transactional(readOnly = true)
    public List<MentionCandidate> search(String nickname) {
        String keyword = nickname == null ? "" : nickname.trim();
        if (keyword.isEmpty() || keyword.length() > 20 || !keyword.matches("[0-9A-Za-z가-힣_]+")) {
            return List.of();
        }
        return userMapper.searchForMention(keyword, LIMIT).stream()
                .map(row -> new MentionCandidate(
                        row.userId(),
                        row.nickname(),
                        profileImageUrlResolver.resolve(row.profileImageStorageKey(), row.profileImageUrl())))
                .toList();
    }
}
