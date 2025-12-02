package de.paul.converter;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ConverterService {

    private final List<FileConversionStrategy> strategies;

    // alle Strategien werden per Spring DI eingesammelt
    public ConverterService(List<FileConversionStrategy> strategies) {
        this.strategies = strategies;
    }

    public ConvertedFile convert(MultipartFile file, String targetMimeType) throws IOException {
        String sourceMimeType = file.getContentType(); // kann null sein
        String originalFilename = file.getOriginalFilename();

        if (sourceMimeType == null) {
            // Notfall: über Dateiendung bestimmen, oder "application/octet-stream"
            sourceMimeType = "application/octet-stream";
        }

        for (FileConversionStrategy strategy : strategies) {
            if (strategy.supports(sourceMimeType, targetMimeType)) {
                return strategy.convert(file, targetMimeType);
            }
        }

        throw new IllegalArgumentException(
                "Konvertierung von " + sourceMimeType + " nach " + targetMimeType + " wird nicht unterstützt.");
    }
}
