package com.runmate.server.terms.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.runmate.server.terms.dto.CurrentTermResponse;
import com.runmate.server.terms.mapper.TermMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TermService {
    private final TermMapper termMapper;

    @Transactional(readOnly = true)
    public List<CurrentTermResponse> current() {
        return termMapper.findCurrentTerms(LocalDateTime.now()).stream()
                .map(term -> new CurrentTermResponse(
                        term.getTermId(),
                        term.getTermCode().name(),
                        term.getVersion(),
                        term.getTitle(),
                        term.getContentUrl(),
                        term.getContentHash(),
                        Boolean.TRUE.equals(term.getRequired())
                ))
                .toList();
    }
}
