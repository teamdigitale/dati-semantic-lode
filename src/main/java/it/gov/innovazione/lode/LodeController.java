package it.gov.innovazione.lode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URL;
import java.util.Collection;
import java.util.List;

@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:}")
public class LodeController {

    private static final Collection<String> ALLOWED_LANGS = List.of("it", "en", "de", "fr");
    private final Extractor extractor;
    private final TransformerService transformerService;

    @GetMapping(value = "extract", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    String extract(
            @RequestParam URL url,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        if (!ALLOWED_LANGS.contains(lang)) {
            throw new IllegalArgumentException("Unsupported language: " + lang + " - Supported languages are: " + ALLOWED_LANGS);
        }
        return transformerService.applyXSLTTransformation(
                extractor.extract(url),
                url.toString(),
                lang);
    }

    @GetMapping(value = "source", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String source(@RequestParam URL url) {
        return extractor.extract(url);
    }
}
