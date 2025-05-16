package com.scrapi.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	// Rutas relativas en tu carpeta `public/`
	private static final String CREATURES_WEB_PATH = "/images/criaturas/";
	private static final String DROPS_WEB_PATH = "/images/drops/";

	public List<Map<String, Object>> scrapear() throws IOException {
		Document doc = Jsoup.connect(URL).userAgent("Mozilla/5.0").timeout(10_000).get();

		Element table = doc.selectFirst("table");
		List<Element> filas = table.select("tr").subList(1, table.select("tr").size());

		List<Map<String, Object>> lista = new ArrayList<>();

		for (Element fila : filas) {
			Elements celdas = fila.select("td");
			Map<String, Object> criatura = new HashMap<>();

			criatura.put("nombre", celdas.get(0).text());

			// Construyo URL local de la imagen de criatura
			String fetchCriUrl = celdas.get(1).selectFirst("img").absUrl("src");
			String criFile = getFileName(fetchCriUrl);
			criatura.put("imageUrl", CREATURES_WEB_PATH + criFile);

			criatura.put("hp", parseIntSafe(celdas.get(2).text()));
			criatura.put("exp", parseIntSafe(celdas.get(3).text()));
			criatura.put("oro", parseIntSafe(celdas.get(4).text()));

			// Respawn
			String rawResp = celdas.get(5).text().toLowerCase().trim();
			String[] maxMinResp = rawResp.split("-");
			
			String minRawResp = maxMinResp[0].trim();
			String maxRawResp = maxMinResp[1].trim();
			
			criatura.put("respawnMinSeg", this.parseSegundos(minRawResp));
			criatura.put("respawnMaxSeg", this.parseSegundos(maxRawResp));
			criatura.put("respawnMin", minRawResp);
			criatura.put("respawnMax", maxRawResp);

			// Drops
			List<Map<String, String>> drops = new ArrayList<>();
			for (Element img : celdas.get(6).select("img")) {
				String tooltip = img.hasAttr("title") ? img.attr("title") : img.attr("alt");

				// parseo nombre y porcentaje correctamente
				AbstractMap.SimpleEntry<String, String> parsed = parseNombreYPorcentaje(tooltip);
				String nombre = parsed.getKey();
				String porcentaje = parsed.getValue();

				// URL local del drop
				String fetchDropUrl = img.absUrl("src");
				String dropFile = getFileName(fetchDropUrl);

				Map<String, String> drop = new HashMap<>();
				drop.put("imagenUrl", DROPS_WEB_PATH + dropFile);
				drop.put("nombre", nombre);
				drop.put("porcentaje", porcentaje);
				drops.add(drop);
			}
			criatura.put("drops", drops);

			lista.add(criatura);
		}
		return lista;
	}

	private int parseSegundos(String rawResp) {
		
		int segundosTotales = 0;
		//Primero verifiar si el string tiene un espacio.
		if(rawResp.contains(" ")) {
			//Significa que hay que sumar dos valores distintos.
			String[] partes = rawResp.split(" ");
			segundosTotales = this.calcularSegundos(partes[0]);
			segundosTotales += this.calcularSegundos(partes[1]);
			return segundosTotales;
		}
		segundosTotales += this.calcularSegundos(rawResp);
		return segundosTotales;
	}

	private int calcularSegundos(String tiempo) {
		int segundos = 0;
		if(tiempo.contains("h")) {
			String cleanTiempo = tiempo.replace("h", "");
			int tiempoLimpio = Integer.parseInt(cleanTiempo);
			segundos = tiempoLimpio * 3600;
		}
		if(tiempo.contains("m")) {
			String cleanTiempo = tiempo.replace("m", "");
			int tiempoLimpio = Integer.parseInt(cleanTiempo);
			segundos = tiempoLimpio * 60;
		}
		if(tiempo.contains("s")) {
			String cleanTiempo = tiempo.replace("s", "");
			int tiempoLimpio = Integer.parseInt(cleanTiempo);
			segundos = tiempoLimpio;
		}
		return segundos;
	}

	public List<Map<String, Object>> scrapearDropsPorItem() throws IOException {
		Document doc = Jsoup.connect(URL).userAgent("Mozilla/5.0").timeout(10_000).get();

		Element table = doc.selectFirst("table");
		List<Element> filas = table.select("tr").subList(1, table.select("tr").size());

		Map<String, Map<String, Object>> dropMap = new HashMap<>();

		for (Element fila : filas) {
			Elements celdas = fila.select("td");
			String nombreCriatura = celdas.get(0).text();

			// URL local de la criatura
			String fetchCriUrl = celdas.get(1).selectFirst("img").absUrl("src");
			String criFile = getFileName(fetchCriUrl);
			String criLocal = CREATURES_WEB_PATH + criFile;

			for (Element img : celdas.get(6).select("img")) {
				String tooltip = img.hasAttr("title") ? img.attr("title") : img.attr("alt");

				// parseo nombreDrop y porcentaje
				AbstractMap.SimpleEntry<String, String> parsed = parseNombreYPorcentaje(tooltip);
				String nombreDrop = parsed.getKey();
				String porcentaje = parsed.getValue();

				// URL local del drop
				String fetchDropUrl = img.absUrl("src");
				String dropFile = getFileName(fetchDropUrl);
				String dropLocal = DROPS_WEB_PATH + dropFile;

				// inicializar drop si es primera vez
				dropMap.computeIfAbsent(nombreDrop, k -> {
					Map<String, Object> m = new HashMap<>();
					m.put("drop", nombreDrop);
					m.put("imagenUrl", dropLocal);
					m.put("criaturas", new ArrayList<Map<String, String>>());
					return m;
				});

				@SuppressWarnings("unchecked")
				List<Map<String, String>> criaturas = (List<Map<String, String>>) dropMap.get(nombreDrop)
						.get("criaturas");

				Map<String, String> entry = new HashMap<>();
				entry.put("nombre", nombreCriatura);
				entry.put("imagenUrl", criLocal);
				entry.put("porcentaje", porcentaje);
				criaturas.add(entry);
			}
		}

		return new ArrayList<>(dropMap.values());
	}

	/**
	 * Extrae nombre y porcentaje (último número antes de '%'), preservando
	 * modificadores internos como "(RM +20)". Ej: "Gorro Mágico (RM +20) 0.1 %" →
	 * key="Gorro Mágico (RM +20)", value="0.1"
	 */
	private AbstractMap.SimpleEntry<String, String> parseNombreYPorcentaje(String tooltip) {
		String nombre = tooltip.trim();
		String porcentaje = "";

		// regex: todo hasta la última parte no numérica, luego el número final, luego
		// opcional '%'
		Pattern pat = Pattern.compile("^(.*\\D)\\s+(\\d+(?:[.,]\\d+)?)\\s*%$");
		Matcher m = pat.matcher(tooltip);
		if (m.matches()) {
			nombre = m.group(1).trim();
			porcentaje = m.group(2).replace(",", ".");
		}
		return new AbstractMap.SimpleEntry<>(nombre, porcentaje);
	}

	private String getFileName(String fetchUrl) {
		try {
			String realUrl = decodeMediaUrl(fetchUrl);
			URL u = new URL(realUrl);
			return Paths.get(u.getPath()).getFileName().toString();
		} catch (Exception e) {
			String afterSlash = fetchUrl.substring(fetchUrl.lastIndexOf('/') + 1);
			return afterSlash.contains("?") ? afterSlash.substring(0, afterSlash.indexOf('?')) : afterSlash;
		}
	}

	private String decodeMediaUrl(String fetchUrl) {
		try {
			URL u = new URL(fetchUrl);
			String q = u.getQuery();
			if (q == null)
				return fetchUrl;
			for (String part : q.split("&")) {
				if (part.startsWith("media=")) {
					return URLDecoder.decode(part.substring(6), StandardCharsets.UTF_8.name());
				}
			}
		} catch (Exception ignored) {
		}
		return fetchUrl;
	}

	private Integer parseIntSafe(String val) {
		try {
			return Integer.parseInt(val.trim());
		} catch (Exception e) {
			return 0;
		}
	}
}
