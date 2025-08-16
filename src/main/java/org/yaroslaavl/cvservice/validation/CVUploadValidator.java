package org.yaroslaavl.cvservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.web.multipart.MultipartFile;
import org.yaroslaavl.cvservice.exception.InvalidTypeException;
import org.yaroslaavl.cvservice.exception.NotPDFException;
import org.yaroslaavl.cvservice.exception.NotReadableException;
import org.yaroslaavl.cvservice.exception.PDFSizeException;

import java.io.IOException;
import java.util.Objects;

public class CVUploadValidator implements ConstraintValidator<CVUpload, MultipartFile> {

    private static final String END = ".pdf";
    private static final String TYPE = "application/pdf";

    @Override
    public boolean isValid(MultipartFile pdf, ConstraintValidatorContext constraintValidatorContext) {
        if (!Objects.requireNonNull(pdf.getOriginalFilename()).toLowerCase().endsWith(END)) {
            throw new NotPDFException("The file is not pdf");
        }

        if (!TYPE.equalsIgnoreCase(pdf.getContentType())) {
            throw new InvalidTypeException("Invalid type");
        }

        try (PDDocument document = PDDocument.load(pdf.getInputStream())) {
            if (document.getNumberOfPages() == 0 || document.getNumberOfPages() > 5) {
                throw new PDFSizeException("PDF is empty or too large. PDF size is " + document.getNumberOfPages());
            }

            return true;
        } catch (IOException e) {
            throw new NotReadableException("The file with name: " + pdf.getName() + " cannot be read");
        }
    }
}
