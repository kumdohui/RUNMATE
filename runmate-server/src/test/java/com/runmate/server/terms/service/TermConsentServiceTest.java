package com.runmate.server.terms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.runmate.server.auth.dto.AuthDtos.TermConsentRequest;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.terms.domain.Term;
import com.runmate.server.terms.mapper.TermMapper;

@ExtendWith(MockitoExtension.class)
class TermConsentServiceTest {
    private static final String HASH = "a".repeat(64);

    @Mock
    private TermMapper termMapper;

    private TermConsentService service;

    @BeforeEach
    void setUp() {
        service = new TermConsentService(termMapper);
    }

    @Test
    void acceptsConsentWhenContentHashMatches() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        Term term = term(1L, HASH, now);
        when(termMapper.findByTermIdIn(List.of(1L))).thenReturn(List.of(term));
        when(termMapper.findCurrentRequiredTerms(now)).thenReturn(List.of(term));

        List<Term> result = service.validate(
                List.of(new TermConsentRequest(1L, HASH)),
                now
        );

        assertThat(result).containsExactly(term);
    }

    @Test
    void rejectsConsentWhenContentHashChanged() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        Term term = term(1L, HASH, now);
        when(termMapper.findByTermIdIn(List.of(1L))).thenReturn(List.of(term));

        assertThatThrownBy(() -> service.validate(
                List.of(new TermConsentRequest(1L, "b".repeat(64))),
                now
        ))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("약관 내용이 변경");
    }

    private Term term(Long id, String hash, LocalDateTime now) {
        Term term = mock(Term.class);
        when(term.getTermId()).thenReturn(id);
        when(term.getContentHash()).thenReturn(hash);
        when(term.isEffectiveAt(now)).thenReturn(true);
        return term;
    }
}
