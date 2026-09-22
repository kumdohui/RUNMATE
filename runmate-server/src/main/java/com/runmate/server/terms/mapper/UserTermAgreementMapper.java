package com.runmate.server.terms.mapper;

import com.runmate.server.terms.domain.UserTermAgreement;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserTermAgreementMapper {
    int insert(UserTermAgreement agreement);

    default List<UserTermAgreement> saveAll(List<UserTermAgreement> agreements) {
        agreements.forEach(this::insert);
        return agreements;
    }
}



