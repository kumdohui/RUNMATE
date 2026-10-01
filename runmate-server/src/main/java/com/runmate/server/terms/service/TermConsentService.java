package com.runmate.server.terms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.auth.dto.AuthDtos.TermConsentRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.terms.domain.Term;
import com.runmate.server.terms.mapper.TermMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TermConsentService {
    private final TermMapper termMapper;

    @Transactional(readOnly = true)
    public List<Term> validate(
            List<TermConsentRequest> consents,
            LocalDateTime now
    ) {
        Map<Long, TermConsentRequest> consentByTermId = consents.stream()
                .collect(Collectors.toMap(
                        TermConsentRequest::termId,
                        Function.identity(),
                        (left, right) -> {
                            throw new AuthException("같은 약관 동의가 중복되어 있습니다.");
                        }
                ));

        List<Term> terms = termMapper.findByTermIdIn(
                consentByTermId.keySet().stream().toList()
        );
        if (terms.size() != consentByTermId.size()) {
            throw new AuthException("존재하지 않는 약관이 포함되어 있습니다.");
        }

        boolean containsInvalidTerm = terms.stream()
                .anyMatch(term -> !term.isEffectiveAt(now));
        if (containsInvalidTerm) {
            throw new AuthException("현재 동의할 수 없는 약관이 포함되어 있습니다.");
        }

        boolean containsChangedContent = terms.stream()
                .anyMatch(term -> !hashMatches(
                        term.getContentHash(),
                        consentByTermId.get(term.getTermId()).contentHash()
                ));
        if (containsChangedContent) {
            throw new AuthException("약관 내용이 변경되었습니다. 새로고침 후 다시 동의해 주세요.");
        }

        Set<Long> requiredTermIds = termMapper
                .findCurrentRequiredTerms(now)
                .stream()
                .map(Term::getTermId)
                .collect(Collectors.toSet());
        if (!consentByTermId.keySet().containsAll(requiredTermIds)) {
            throw new AuthException("필수 약관에 모두 동의해 주세요.");
        }
        return terms;
    }

    private boolean hashMatches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                actual.toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }
}
