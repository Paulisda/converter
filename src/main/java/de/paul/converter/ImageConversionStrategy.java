package de.paul.converter;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ImageConversionStrategy implements FileConversionStrategy {

    // Unterstützte Ziel-MimeTypes → ImageIO-Formatnamen
    private static final Map<String, String> MIME_TO_FORMAT = new HashMap<>();

    static {
        MIME_TO_FORMAT.put("image/png", "png");
        MIME_TO_FORMAT.put("image/jpeg", "jpg");
        // WEBP geht nur mit zusätzlicher ImageIO-Extension (z. B. TwelveMonkeys + WebP-Plugin)
        // MIME_TO_FORMAT.put("image/webp", "webp");
    }

    @Override
    public boolean supports(String sourceMimeType, String targetMimeType) {
        if (sourceMimeType == null || targetMimeType == null) {
            return false;
        }

        // Nur Bilder konvertieren
        // z. B. image/png → image/jpeg oder image/jpeg → image/png
        return sourceMimeType.startsWith("image/")
                && MIME_TO_FORMAT.containsKey(targetMimeType);
    }

    @Override
    public ConvertedFile convert(MultipartFile input, String targetMimeType) throws IOException {
        // Bild einlesen
        BufferedImage image = ImageIO.read(input.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException(
                    "Die hochgeladene Datei scheint kein unterstütztes Bild zu sein.");
        }

        String formatName = MIME_TO_FORMAT.get(targetMimeType);
        if (formatName == null) {
            throw new IllegalArgumentException(
                    "Ziel-Mime-Type wird von der ImageConversionStrategy nicht unterstützt: " + targetMimeType);
        }

        // In gewünschtes Format schreiben
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean success = ImageIO.write(image, formatName, baos);
        if (!success) {
            throw new IllegalStateException(
                    "Konvertierung nach " + targetMimeType + " ist mit ImageIO fehlgeschlagen.");
        }

        byte[] bytes = baos.toByteArray();

        // Dateinamen bauen
        String originalName = input.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "image";
        }

        String baseName = originalName;
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalName.substring(0, dotIndex);
        }

        String extension = switch (targetMimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            // case "image/webp" -> "webp";
            default -> "bin";
        };

        String newFileName = baseName + "_converted." + extension;

        return new ConvertedFile(newFileName, targetMimeType, bytes);
    }
}
