package de.paul.converter;

public record ConvertedFile(
        String fileName,
        String mimeType,
        byte[] bytes
) {}
