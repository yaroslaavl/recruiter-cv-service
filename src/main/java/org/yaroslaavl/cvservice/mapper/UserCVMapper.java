package org.yaroslaavl.cvservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.yaroslaavl.cvservice.database.entity.UserCV;
import org.yaroslaavl.cvservice.dto.CVSummaryDto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserCVMapper {

    @Mapping(target = "cvId", source = "id")
    CVSummaryDto toSummaryDto(UserCV userCV);

    List<CVSummaryDto> toSummaryDto(List<UserCV> userCvs);
}
