package com.runmate.server.terms.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.runmate.server.terms.domain.Term;

public interface TermRepository extends JpaRepository<Term, Long> {
    List<Term> findByTermIdIn(List<Long> termIds);
    @Query("""
            SELECT t
            FROM Term t
            WHERE t.required = true
              AND t.effectiveAt <= :now
              AND (t.expiredAt IS NULL OR t.expiredAt > :now)
            """)
    List<Term> findCurrentRequiredTerms(@Param("now") LocalDateTime now);
}