package com.scrapi.service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class PaginaWebService {

    private static final String URL = "https://manual.imperiumclassic.com/doku.php?id=criaturas&purge=true";

    public List<Map<String, Object>> scrapear() throws IOException {
        Document doc = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();

        Element table = doc.selectFirst("table");
        List<Element> filas = table.select("tr").subList(1, table.select("tr").size());

        List<Map<String, Object>> lista = new ArrayList<>();

        for (Element fila : filas) {
            Elements celdas = fila.select("td");
            Map<String, Object> criatura = new HashMap<>();

            criatura.put("nombre", celdas.get(0).text());
            criatura.put("imageUrl", celdas.get(1).selectFirst("img").absUrl("src"));
            criatura.put("hp", parseIntSafe(celdas.get(2).text()));
            criatura.put("exp", parseIntSafe(celdas.get(3).text()));
            criatura.put("oro", parseIntSafe(celdas.get(4).text()));

            // RESP
            String rawRespawn = celdas.get(5).text().toLowerCase().trim();
            int respawnMin = 0;
            int respawnMax = 0;
            String unidad = "";

            if (rawRespawn.contains("h")) unidad = "h";
            else if (rawRespawn.contains("s")) unidad = "s";
            else if (rawRespawn.contains("m")) unidad = "m";

            String clean = rawRespawn.replaceAll("[^0-9–\\-]+", "");
            String[] partes = clean.split("–|\\-");
            if (partes.length > 0) {
                respawnMin = parseIntSafe(partes[0]);
                respawnMax = partes.length > 1 ? parseIntSafe(partes[1]) : respawnMin;
            }

            criatura.put("respawnMin", respawnMin);
            criatura.put("respawnMax", respawnMax);
            criatura.put("respawnUnidad", unidad);

            // DROPS
            List<Map<String, String>> drops = new ArrayList<>();
            for (Element img : celdas.get(6).select("img")) {
                Map<String, String> drop = new HashMap<>();
                drop.put("imagenUrl", img.absUrl("src"));

                String tooltip = img.hasAttr("title") ? img.attr("title") : img.attr("alt");
                String nombre = tooltip.trim();
                String porcentaje = "";

                Pattern pattern = Pattern.compile("^(.*?)(\\d+(?:[.,]\\d+)?)(?:\\s?%)?");
                Matcher matcher = pattern.matcher(tooltip);

                if (matcher.find()) {
                    nombre = matcher.group(1).trim();
                    String raw = matcher.group(2).replaceAll("[^0-9.,]", "").trim();

                    if (!raw.isEmpty()) {
                        raw = raw.replace(",", ".");
                        try {
                            double p = Double.parseDouble(raw);
                            porcentaje = (p % 1 == 0) ? String.valueOf((int)p) : String.format("%.2f", p).replaceAll("\\.?0+$", "");
                        } catch (NumberFormatException e) {
                            porcentaje = raw.replaceAll("[^0-9.]", "");
                        }
                    }
                } else {
                    // fallback: intentar capturar solo números en el texto
                    Pattern fallback = Pattern.compile("(\\d+(?:[.,]\\d+)?)");
                    Matcher m2 = fallback.matcher(tooltip);
                    if (m2.find()) {
                        porcentaje = m2.group(1).replace(",", ".").replaceAll("[^0-9.]", "");
                    }
                }


                drop.put("nombre", nombre);
                drop.put("porcentaje", porcentaje);
                drops.add(drop);
            }

            criatura.put("drops", drops);
            lista.add(criatura);
        }

        return lista;
    }

    private Integer parseIntSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Map<String, Object>> scrapearDropsPorItem() throws IOException {
        Document doc = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();

        Element table = doc.selectFirst("table");
        List<Element> filas = table.select("tr").subList(1, table.select("tr").size());

        // Resultado final
        Map<String, Map<String, Object>> dropMap = new HashMap<>();

        for (Element fila : filas) {
            Elements celdas = fila.select("td");

            String nombreCriatura = celdas.get(0).text();
            String imagenUrlCriatura = celdas.get(1).selectFirst("img").absUrl("src");

            for (Element img : celdas.get(6).select("img")) {
                String tooltip = img.hasAttr("title") ? img.attr("title") : img.attr("alt");

                // Inicialización por defecto
                String nombreDrop = tooltip.trim();
                String porcentaje = "";
                String imagenUrlDrop = img.absUrl("src");

                // Extraer nombre + porcentaje
                Pattern pattern = Pattern.compile("^(.*?)(\\d+(?:[.,]\\d+)?)(?:\\s?%)?");
                Matcher matcher = pattern.matcher(tooltip);

                if (matcher.find()) {
                    nombreDrop = matcher.group(1).trim();
                    String raw = matcher.group(2).replace(",", ".").replaceAll("[^0-9.]", "").trim();

                    try {
                        double p = Double.parseDouble(raw);
                        porcentaje = (p % 1 == 0)
                            ? String.valueOf((int) p)
                            : String.format("%.2f", p).replaceAll("\\.?0+$", "");
                    } catch (Exception e) {
                        porcentaje = raw;
                    }
                }

                // Si el drop no existe, lo creo
                if (!dropMap.containsKey(nombreDrop)) {
                    Map<String, Object> nuevoDrop = new HashMap<>();
                    nuevoDrop.put("drop", nombreDrop);
                    nuevoDrop.put("imagenUrl", imagenUrlDrop);
                    nuevoDrop.put("criaturas", new ArrayList<Map<String, String>>());
                    dropMap.put(nombreDrop, nuevoDrop);
                }

                // Agrego la criatura al array de criaturas
                List<Map<String, String>> criaturas = (List<Map<String, String>>) dropMap.get(nombreDrop).get("criaturas");
                criaturas.add(Map.of(
                    "nombre", nombreCriatura,
                    "imagenUrl", imagenUrlCriatura,
                    "porcentaje", porcentaje
                ));
            }
        }

        return new ArrayList<>(dropMap.values());
    }





}
