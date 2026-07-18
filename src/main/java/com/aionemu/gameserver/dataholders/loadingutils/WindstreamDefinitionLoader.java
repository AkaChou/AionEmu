package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.model.flypath.FlyPathType;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.templates.windstreams.Location2D;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamRoute;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class WindstreamDefinitionLoader {
	private static final float SOURCE_ENDPOINT_TOLERANCE = 50;

	private WindstreamDefinitionLoader() {
	}

	public static WindstreamData load(File flyPathFile, File windFile, File idMappingsFile) {
		try {
			Map<String, Element> documents = documents(parse(flyPathFile));
			documents.putAll(documents(parse(windFile)));
			Map<String, List<Integer>> worldIds = worldIds(parse(idMappingsFile));
			// AionEmu still exposes these two pre-5.8 IDs for the same client levels.
			worldIds.computeIfAbsent("tiamat_down", key -> new ArrayList<>()).add(600040000);
			worldIds.computeIfAbsent("ldf5a", key -> new ArrayList<>()).add(600050000);

			Map<Integer, List<WindstreamRoute>> routesByMap = new LinkedHashMap<>();
			Element flyPaths = requiredDocument(documents, "fly_path.xml");
			for (Element group : descendants(flyPaths, "path_group")) {
				Element wind = child(group, "wind");
				if (wind == null) {
					continue;
				}
				int groupId = integer(group, "group_id");
				String worldName = text(requiredChild(requiredChild(group, "start"), "world"));
				List<Integer> mapIds = worldIds.get(normalize(worldName));
				if (mapIds == null || mapIds.isEmpty()) {
					throw new IllegalStateException("No world ID for windstream world " + worldName);
				}
				String windPathFile = text(requiredChild(wind, "file"));
				String documentName = "windpath/" + windPathFile.substring(0, windPathFile.lastIndexOf('.')) + ".xml";
				List<Point3D> sourcePoints = points(requiredDocument(documents, documentName));
				int durationMillis = Math.round(decimal(group, "fly_time") * 1000);
				verifyEndpoints(groupId, group, sourcePoints);
				List<Point3D> points = samplePoints(sourcePoints);
				for (int mapId : mapIds) {
					routesByMap.computeIfAbsent(mapId, key -> new ArrayList<>())
						.add(new WindstreamRoute(mapId, groupId, durationMillis, points));
				}
			}

			List<WindstreamTemplate> templates = new ArrayList<>();
			List<WindstreamRoute> routes = new ArrayList<>();
			for (Map.Entry<Integer, List<WindstreamRoute>> entry : routesByMap.entrySet()) {
				List<Location2D> locations = new ArrayList<>();
				for (WindstreamRoute route : entry.getValue()) {
					// The Iluma/Norsvold invasion paths are enabled by their existing NPC AIs.
					int state = route.getId() == 301 || route.getId() == 302 ? 0 : 1;
					locations.add(new Location2D(route.getId(), state, FlyPathType.ONE_WAY));
					routes.add(route);
				}
				templates.add(new WindstreamTemplate(entry.getKey(), locations));
			}
			if (routes.isEmpty()) {
				throw new IllegalStateException("No windstream routes found in " + flyPathFile + " / " + windFile);
			}
			return new WindstreamData(templates, routes);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load windstreams from definitions", e);
		}
	}

	private static Document parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		try (InputStream fileStream = new FileInputStream(file);
			 InputStream stream = file.getName().endsWith(".gz") ? new GZIPInputStream(fileStream) : fileStream) {
			return factory.newDocumentBuilder().parse(stream);
		}
	}

	private static Map<String, Element> documents(Document document) {
		Map<String, Element> result = new HashMap<>();
		for (Element element : descendants(document.getDocumentElement(), "static_document")) {
			result.put(normalize(element.getAttribute("name")), element);
		}
		return result;
	}

	private static Map<String, List<Integer>> worldIds(Document idMappings) {
		Element worldIds = requiredDocument(documents(idMappings), "id/worldid.xml");
		Map<String, List<Integer>> result = new HashMap<>();
		for (Element data : descendants(worldIds, "data")) {
			String name = text(data);
			if (!name.isEmpty()) {
				result.computeIfAbsent(normalize(name), key -> new ArrayList<>()).add(Integer.parseInt(data.getAttribute("id")));
			}
		}
		return result;
	}

	private static List<Point3D> points(Element document) {
		List<Point3D> result = new ArrayList<>();
		for (Element point : descendants(document, "Point")) {
			String[] coordinates = point.getAttribute("Pos").split(",");
			if (coordinates.length != 3) {
				throw new IllegalStateException("Invalid windstream point " + point.getAttribute("Pos"));
			}
			result.add(new Point3D(Float.parseFloat(coordinates[0]), Float.parseFloat(coordinates[1]), Float.parseFloat(coordinates[2])));
		}
		if (result.size() < 2) {
			throw new IllegalStateException("Windstream route has fewer than two points");
		}
		return result;
	}

	private static List<Point3D> samplePoints(List<Point3D> sourcePoints) {
		List<Point3D> result = new ArrayList<>((sourcePoints.size() + 9) / 10);
		for (int i = 0; i < sourcePoints.size(); i += 10) {
			result.add(sourcePoints.get(i));
		}
		return result;
	}

	private static void verifyEndpoints(int groupId, Element group, List<Point3D> points) {
		Point3D start = point(requiredChild(group, "start"));
		Point3D end = point(requiredChild(group, "end"));
		if (MathUtil.getDistance(start, points.getFirst()) > SOURCE_ENDPOINT_TOLERANCE
			|| MathUtil.getDistance(end, points.getLast()) > SOURCE_ENDPOINT_TOLERANCE) {
			throw new IllegalStateException("Windstream " + groupId + " points do not match fly_path.xml endpoints");
		}
	}

	private static Point3D point(Element element) {
		return new Point3D(decimal(element, "x"), decimal(element, "y"), decimal(element, "z"));
	}

	private static int integer(Element parent, String name) {
		return Integer.parseInt(text(requiredChild(parent, name)));
	}

	private static float decimal(Element parent, String name) {
		return Float.parseFloat(text(requiredChild(parent, name)));
	}

	private static Element requiredDocument(Map<String, Element> documents, String name) {
		Element result = documents.get(normalize(name));
		if (result == null) {
			throw new IllegalStateException("Missing definition document " + name);
		}
		return result;
	}

	private static Element requiredChild(Element parent, String name) {
		Element result = child(parent, name);
		if (result == null) {
			throw new IllegalStateException("Missing " + name + " under " + parent.getTagName());
		}
		return result;
	}

	private static Element child(Element parent, String name) {
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && element.getTagName().equals(name)) {
				return element;
			}
		}
		return null;
	}

	private static List<Element> descendants(Element parent, String name) {
		NodeList nodes = parent.getElementsByTagName(name);
		List<Element> result = new ArrayList<>(nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i++) {
			result.add((Element) nodes.item(i));
		}
		return result;
	}

	private static String text(Element element) {
		return element.getTextContent().trim();
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT);
	}
}
