package com.scrapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ImageDownloader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void download(String imageUrl, String destFolder) {
        try {
            // 1) Decodificamos la URL real (si era fetch.php?media=...)
            String realUrl = decodeMediaUrl(imageUrl);
            logger.info("Descargando imagen desde: {}", realUrl);

            // 2) Con Jsoup pedimos el binario (ignoreContentType IMPORTANTE)
            Connection.Response res = Jsoup.connect(realUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .followRedirects(true)
                .execute();

            // 3) Extraemos el nombre de archivo igual que antes
            String fileName = extractFileName(new URL(realUrl));
            logger.debug("Nombre de archivo: {}", fileName);

            // 4) Creamos carpeta si hace falta
            Path folder = Paths.get(destFolder);
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
                logger.debug("  → Carpeta creada: {}", folder);
            }

            Path target = folder.resolve(fileName);
            if (Files.exists(target)) {
                logger.info("  → Ya existe {}, salto descarga", target);
                return;
            }

            // 5) Guardamos el binario
            try (InputStream in = res.bodyStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                logger.info("✔ Imagen guardada en {}", target);
            }

        } catch (IOException e) {
            logger.error("✖ Error descargando {}: ", imageUrl, e);
        } catch (Exception e) {
            logger.error("✖ Error genérico en ImageDownloader:", e);
        }
    }

    private String extractFileName(URL url) throws Exception {
        String path = url.getPath();
        String name = Paths.get(path).getFileName().toString();
        if ("fetch.php".equalsIgnoreCase(name)) {
            String query = url.getQuery();
            String media = Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("media="))
                .map(p -> p.substring(6))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No encontró parámetro media"));
            String decoded = URLDecoder.decode(media, StandardCharsets.UTF_8.name());
            return Paths.get(new URI(decoded).getPath()).getFileName().toString();
        }
        return name;
    }

    private String decodeMediaUrl(String fetchUrl) {
        try {
            URL u = new URL(fetchUrl);
            String q = u.getQuery();
            if (q == null) return fetchUrl;
            for (String part : q.split("&")) {
                if (part.startsWith("media=")) {
                    return URLDecoder.decode(part.substring(6), StandardCharsets.UTF_8.name());
                }
            }
        } catch (Exception e) {
            // si algo falla, devolvemos la URL original
        }
        return fetchUrl;
    }
}
