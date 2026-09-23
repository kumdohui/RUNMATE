package com.runmate.server.user.controller;

import com.runmate.server.user.dto.MentionCandidate;
import com.runmate.server.user.service.MentionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MentionController {
    private final MentionService mentionService;

    @GetMapping("/api/users/mentions")
    public List<MentionCandidate> search(@RequestParam(defaultValue = "") String nickname) {
        return mentionService.search(nickname);
    }
}
