package org.yaroslaavl.cvservice.service;

import org.yaroslaavl.cvservice.dto.CVSummaryDto;
import org.yaroslaavl.cvservice.dto.CVUploadDto;

import java.util.List;
import java.util.UUID;

public interface MinioCVService {

    void upload(CVUploadDto cvUploadDto);

    void remove(boolean isMain);

    List<CVSummaryDto> findAllCandidateCvs();

    String getCvForCandidate(UUID cvId, boolean isMain);

    String getCvForRecruiter(UUID cvId);
}
