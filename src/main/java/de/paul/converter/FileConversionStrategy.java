package de.paul.converter;

import org.springframework.web.multipart.MultipartFile;

public interface FileConversionStrategy {
    boolean supports(String sourceMimeType, String targetMimeType);

    ConvertedFile convert(MultipartFile input, String targetMimeType) throws java.io.IOException;
}
