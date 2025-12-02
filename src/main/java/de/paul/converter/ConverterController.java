package de.paul.converter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ConverterController {

    private final ConverterService converterService;

    public ConverterController(ConverterService converterService) {
        this.converterService = converterService;
    }

    // Liefert die index.html aus /static (Spring Boot macht das automatisch)
    @GetMapping("/")
    public String home(Model model) {
        return "redirect:/index.html";
    }

    @PostMapping("/api/convert")
    @ResponseBody
    public ResponseEntity<byte[]> convertMedia(@RequestParam("file") MultipartFile file,
            @RequestParam("targetType") String targetMimeType) throws Exception {

        ConvertedFile converted = converterService.convert(file, targetMimeType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + converted.fileName() + "\"")
                .contentType(MediaType.parseMediaType(converted.mimeType()))
                .body(converted.bytes());
    }
}
