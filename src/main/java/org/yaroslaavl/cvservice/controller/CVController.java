package org.yaroslaavl.cvservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaroslaavl.cvservice.dto.CVSummaryDto;
import org.yaroslaavl.cvservice.dto.CVUploadDto;
import org.yaroslaavl.cvservice.service.MinioCVService;
import org.yaroslaavl.cvservice.validation.CVUpload;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cv")
public class CVController {

    private final MinioCVService minioCVService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> upload(@RequestParam("cv") @CVUpload MultipartFile cv,
                       @RequestParam("isMain") Boolean isMain) {
        CVUploadDto cvUploadDto = new CVUploadDto(cv, isMain);

        minioCVService.upload(cvUploadDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{cvId}/candidate")
    public ResponseEntity<String> getCvForCandidate(@PathVariable UUID cvId,
                                                    @RequestParam("isMain") boolean isMain) {
        return ResponseEntity.ok(minioCVService.getCvForCandidate(cvId, isMain));
    }

    @GetMapping("/{cvId}/recruiter")
    public ResponseEntity<String> getCvForRecruiter(@PathVariable UUID cvId) {
        return ResponseEntity.ok(minioCVService.getCvForRecruiter(cvId));
    }

    @GetMapping("/info")
    public ResponseEntity<List<CVSummaryDto>> findAllCandidateCvs() {
        return ResponseEntity.ok(minioCVService.findAllCandidateCvs());
    }

    @DeleteMapping("/{isMain}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> remove(@PathVariable boolean isMain) {
        minioCVService.remove(isMain);
        return ResponseEntity.noContent().build();
    }
}

