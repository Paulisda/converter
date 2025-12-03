package de.paul.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegClient {
    private final String ffmpegPath;

    public FfmpegClient(@Value("${converter.ffmpeg.path:ffmpeg}") String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public ConvertedFile convert(MultipartFile input, String targetMimeType, List<String> commands, String defaultName, String outputExt) throws IOException {
        // Input- und Output-Tempfiles
        String originalName = input.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = defaultName;
        }

        String inputExt = getExtension(originalName);
        if (inputExt.isEmpty()) {
            inputExt = "dat";
        }

        final String suffix = "." + outputExt;

        final Path inputTempFile = Files.createTempFile("upload_", "." + inputExt);
        final Path outputTempFile = Files.createTempFile("converted_", suffix);

        try {
            // Upload-Datei in Tempfile schreiben
            Files.write(inputTempFile, input.getBytes());

            // FFmpeg-Kommando bauen
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-y"); // ohne Rückfrage überschreiben
            command.add("-i");
            command.add(inputTempFile.toAbsolutePath().toString());
            command.addAll(commands);
            command.add(outputTempFile.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // stderr in stdout mergen (für Log)
            Process process = pb.start();

            // FFmpeg-Output lesen (zum Debuggen useful)
            String ffmpegOutput = readProcessOutput(process.getInputStream());

            // Timeout, z. B. 5 Minuten
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg hat zu lange gebraucht (Timeout).");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "FFmpeg-Konvertierung fehlgeschlagen (exitCode=" + exitCode + "):\n" + ffmpegOutput);
            }

            // Konvertierte Datei einlesen
            byte[] outputBytes = Files.readAllBytes(outputTempFile);

            // Neuer Dateiname: original_basename_converted.mp4
            String baseName = stripExtension(originalName);
            String newFileName = baseName + "_converted" + suffix;

            return new ConvertedFile(newFileName, targetMimeType, outputBytes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg-Konvertierung wurde unterbrochen.", e);
        } finally {
            // Tempfiles aufräumen
            try {
                Files.deleteIfExists(inputTempFile);
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(outputTempFile);
            } catch (IOException ignored) {
            }
        }
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    private String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }
}
