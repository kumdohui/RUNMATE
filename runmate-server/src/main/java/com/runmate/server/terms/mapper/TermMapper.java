package com.runmate.server.terms.mapper;

import java.time.LocalDateTime;
import java.util.List;
import com.runmate.server.terms.domain.Term;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TermMapper {
    List<Term> findByTermIdIn(@Param("termIds") List<Long> termIds);
    List<Term> findCurrentRequiredTerms(@Param("now") LocalDateTime now);
    List<Term> findCurrentTerms(@Param("now") LocalDateTime now);
}


