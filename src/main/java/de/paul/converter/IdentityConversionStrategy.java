package de.paul.converter;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class IdentityConversionStrategy implements FileConversionStrategy {

    @Override
    public boolean supports(String sourceMimeType, String targetMimeType) {
        // Platzhalter: erlaubt jede Kombination (für Demo).
        // In echt: nur bestimmte Kombinationen, z. B.
        // return sourceMimeType.startsWith("image/") && targetMimeType.startsWith("image/");
        return true;
    }


    @Override
    public ConvertedFile convert(MultipartFile input, String targetMimeType)
            throws java.io.IOException {

        // TODO: hier echte Konvertierung einbauen.
        // Aktuell: gibt einfach die Original-Datei zurück (KEINE echte Konvertierung).
        byte[] bytes = input.getBytes();

        String originalName = input.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "file";
        }

        String fileName = originalName + ".converted";

        return new ConvertedFile(fileName, targetMimeType, bytes);
    }
}
