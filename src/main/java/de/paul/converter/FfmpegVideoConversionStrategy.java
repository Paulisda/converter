package de.paul.converter;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FfmpegVideoConversionStrategy implements FileConversionStrategy {

    // MIME → Dateiendung
    private static final Map<String, String> VIDEO_MIME_TO_EXT = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov",
            "video/x-matroska", "mkv"
    );

    private final FfmpegClient ffmpegClient;

    public FfmpegVideoConversionStrategy(final FfmpegClient ffmpegClient) {
        this.ffmpegClient = ffmpegClient;
    }

    @Override
    public boolean supports(String sourceMimeType, String targetMimeType) {
        if (sourceMimeType == null || targetMimeType == null) {
            return false;
        }

        return sourceMimeType.startsWith("video/")
                && VIDEO_MIME_TO_EXT.containsKey(targetMimeType);
    }

    @Override
    public ConvertedFile convert(MultipartFile input, String targetMimeType) throws IOException {
        String outputExt = VIDEO_MIME_TO_EXT.get(targetMimeType);
        if (outputExt == null) {
            throw new IllegalArgumentException(
                    "Ziel-Mime-Type wird von FfmpegVideoConversionStrategy nicht unterstützt: " + targetMimeType
            );
        }

        List<String> extraArgs = new ArrayList<>();

        if ("video/webm".equals(targetMimeType)) {
            // WebM: VP9 + Opus (sauberer WebM-Container)
            extraArgs.add("-c:v");
            extraArgs.add("libvpx-vp9");
            extraArgs.add("-b:v");
            extraArgs.add("2M");
            extraArgs.add("-c:a");
            extraArgs.add("libopus");
            extraArgs.add("-b:a");
            extraArgs.add("160k");
        } else {
            // MP4 / MOV / MKV: H.264 + AAC
            extraArgs.add("-c:v");
            extraArgs.add("libx264");
            extraArgs.add("-preset");
            extraArgs.add("veryfast");
            extraArgs.add("-crf");
            extraArgs.add("23"); // Qualität (niedriger = bessere Qualität / größere Datei)

            extraArgs.add("-c:a");
            extraArgs.add("aac");
            extraArgs.add("-b:a");
            extraArgs.add("192k");
        }

        return ffmpegClient.convert(input, targetMimeType, extraArgs, "video", outputExt);
    }
}
