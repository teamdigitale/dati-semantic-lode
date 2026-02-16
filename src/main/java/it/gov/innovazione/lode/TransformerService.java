package it.gov.innovazione.lode;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;

@Service
public class TransformerService {

    private Transformer transformer;

    @Value("${externalURL}")
    private String externalURL;
    @Value("${webvowl}")
    private String webvowl;

    @SneakyThrows
    private static Source fromClassPath(String href) {
        return new StreamSource(
                new ClassPathResource("xsl-transformers/" + href).getInputStream());
    }

    @PostConstruct
    @SneakyThrows
    void initializeTransformer() {
        String cssLocation = externalURL + "/static/";
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transformerFactory.setURIResolver((href, base) -> fromClassPath(href));
        transformer = transformerFactory.newTransformer(fromClassPath("extraction.xsl"));
        transformer.setParameter("css-location", cssLocation);
        transformer.setParameter("source", externalURL + "/source");
        transformer.setParameter("lode-external-url", externalURL);
        transformer.setParameter("webvowl", webvowl);
    }

    @SneakyThrows
    String applyXSLTTransformation(String content, String url, String lang) {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        transformer.setParameter("lang", lang);
        transformer.setParameter("ontology-url", url);

        StreamSource inputSource = new StreamSource(new StringReader(content));

        transformer.transform(inputSource, new StreamResult(output));

        return output.toString(Charset.defaultCharset());
    }
}
