package org.yaroslaavl.cvservice.service;

import org.yaroslaavl.cvservice.dto.CVUploadDto;

import java.util.UUID;

public interface CVService {

    void upload(CVUploadDto imageUploadDto);

    String getObject(UUID userId);
}
