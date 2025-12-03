package de.paul.converter;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FfmpegAudioConversionStrategy implements FileConversionStrategy {

    // MIME → Dateiendung
    private static final Map<String, String> AUDIO_MIME_TO_EXT = Map.of(
            "audio/mpeg", "mp3",
            "audio/wav", "wav",
            "audio/ogg", "ogg"
    );

    private final FfmpegClient ffmpegClient;

    public FfmpegAudioConversionStrategy(final FfmpegClient ffmpegClient) {
        this.ffmpegClient = ffmpegClient;
    }

    @Override
    public boolean supports(String sourceMimeType, String targetMimeType) {
        if (sourceMimeType == null || targetMimeType == null) {
            return false;
        }

        return sourceMimeType.startsWith("audio/")
                && AUDIO_MIME_TO_EXT.containsKey(targetMimeType);
    }

    @Override
    public ConvertedFile convert(MultipartFile input, String targetMimeType) throws IOException {
        String outputExt = AUDIO_MIME_TO_EXT.get(targetMimeType);
        if (outputExt == null) {
            throw new IllegalArgumentException(
                    "Ziel-Mime-Type wird von FfmpegAudioConversionStrategy nicht unterstützt: " + targetMimeType
            );
        }

        List<String> extraArgs = new ArrayList<>();

        switch (targetMimeType) {
            case "audio/mpeg" -> {
                // MP3
                extraArgs.add("-vn");
                extraArgs.add("-acodec");
                extraArgs.add("libmp3lame");
                extraArgs.add("-b:a");
                extraArgs.add("192k");
            }
            case "audio/wav" -> {
                // WAV (PCM)
                extraArgs.add("-vn");
                extraArgs.add("-acodec");
                extraArgs.add("pcm_s16le");
                extraArgs.add("-ar");
                extraArgs.add("44100");
                extraArgs.add("-ac");
                extraArgs.add("2");
            }
            case "audio/ogg" -> {
                extraArgs.add("-vn");
                extraArgs.add("-acodec");
                extraArgs.add("libvorbis");
                extraArgs.add("-q:a");
                extraArgs.add("5");
            }
            default -> throw new IllegalArgumentException(
                    "Ziel-Mime-Type wird von FfmpegAudioConversionStrategy nicht unterstützt: " + targetMimeType
            );
        }

        return ffmpegClient.convert(input, targetMimeType, extraArgs, "audio", outputExt);
    }
}
