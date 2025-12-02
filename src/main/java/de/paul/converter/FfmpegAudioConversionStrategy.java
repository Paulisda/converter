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
public class FfmpegAudioConversionStrategy implements FileConversionStrategy {

    private final String ffmpegPath;

    public FfmpegAudioConversionStrategy(@Value("${converter.ffmpeg.path:ffmpeg}") String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public boolean supports(String sourceMimeType, String targetMimeType) {
        if (sourceMimeType == null || targetMimeType == null) {
            return false;
        }

        // Unterstützt alle Audio-Quellen → MP3 oder WAV
        return sourceMimeType.startsWith("audio/")
                && (targetMimeType.equals("audio/mpeg")  // MP3
                || targetMimeType.equals("audio/wav")); // WAV
    }

    @Override
    public ConvertedFile convert(MultipartFile input, String targetMimeType) throws IOException {
        String originalName = input.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "audio";
        }

        String inputExt = getExtension(originalName);
        if (inputExt.isEmpty()) {
            inputExt = "dat";
        }

        // Dateiendung und ffmpeg-spezifische Einstellungen je nach Ziel-MimeType
        String outputExt;
        List<String> extraArgs = new ArrayList<>();

        switch (targetMimeType) {
            case "audio/mpeg": // MP3
                outputExt = "mp3";
                // Nur Audio, keine Video-Streams (-vn)
                extraArgs.add("-vn");
                extraArgs.add("-acodec");
                extraArgs.add("libmp3lame");
                extraArgs.add("-b:a");
                extraArgs.add("192k"); // Bitrate
                break;

            case "audio/wav": // WAV (PCM)
                outputExt = "wav";
                extraArgs.add("-vn");
                extraArgs.add("-acodec");
                extraArgs.add("pcm_s16le");
                extraArgs.add("-ar");
                extraArgs.add("44100");
                extraArgs.add("-ac");
                extraArgs.add("2");
                break;

            default:
                throw new IllegalArgumentException(
                        "Ziel-Mime-Type wird von FfmpegAudioConversionStrategy nicht unterstützt: " + targetMimeType
                );
        }

        Path inputTempFile = Files.createTempFile("upload_audio_", "." + inputExt);
        Path outputTempFile = Files.createTempFile("converted_audio_", "." + outputExt);

        try {
            Files.write(inputTempFile, input.getBytes());

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-y");
            command.add("-i");
            command.add(inputTempFile.toAbsolutePath().toString());

            // ffmpeg-Args abhängig vom Ziel
            command.addAll(extraArgs);

            command.add(outputTempFile.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String ffmpegOutput = readProcessOutput(process.getInputStream());

            // Timeout z.B. 2 Minuten – nach Bedarf anpassen
            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg Audio-Konvertierung hat zu lange gebraucht (Timeout).");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "FFmpeg Audio-Konvertierung fehlgeschlagen (exitCode=" + exitCode + "):\n" + ffmpegOutput
                );
            }

            byte[] outputBytes = Files.readAllBytes(outputTempFile);

            String baseName = stripExtension(originalName);
            String newFileName = baseName + "_converted." + outputExt;

            return new ConvertedFile(newFileName, targetMimeType, outputBytes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg Audio-Konvertierung wurde unterbrochen.", e);
        } finally {
            try {
                Files.deleteIfExists(inputTempFile);
            } catch (IOException ignored) {}
            try {
                Files.deleteIfExists(outputTempFile);
            } catch (IOException ignored) {}
        }
    }

    private static String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

    private static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }
}
