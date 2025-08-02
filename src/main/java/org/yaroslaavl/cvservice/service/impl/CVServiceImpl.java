package org.yaroslaavl.cvservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaroslaavl.cvservice.dto.CVUploadDto;
import org.yaroslaavl.cvservice.service.CVService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CVServiceImpl implements CVService {

    @Override
    public void upload(CVUploadDto imageUploadDto) {

    }

    @Override
    public String getObject(UUID userId) {
        return "";
    }
}
