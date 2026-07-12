package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;

final class NpcSkillDefinitionLoader {

	static NpcSkillData load(File file) {
		Map<String, List<NpcSkillTemplate>> groups = new HashMap<>();
		List<NpcSkillTemplates> assignments = new ArrayList<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			String groupId = null;
			List<NpcSkillTemplate> skills = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					if (reader.getLocalName().equals("group")) {
						groupId = attribute(reader, "id");
						skills = new ArrayList<>();
					} else if (reader.getLocalName().equals("skill") && skills != null) {
						String skillId = attribute(reader, "id");
						if (skillId != null) {
							skills.add(new NpcSkillTemplate(parseInt(skillId, "skill id"),
								parseInt(attribute(reader, "level"), 1, "skill level"),
								parseInt(attribute(reader, "probability"), "skill probability")));
						}
					} else if (reader.getLocalName().equals("assign")) {
						String assignedGroup = attribute(reader, "group");
						List<NpcSkillTemplate> assignedSkills = groups.get(assignedGroup);
						if (assignedSkills == null) {
							throw new IllegalStateException("Unknown NPC skill group: " + assignedGroup);
						}
						for (String npcId : attribute(reader, "npc_ids").trim().split("\\s+")) {
							assignments.add(new NpcSkillTemplates(parseInt(npcId, "NPC id"), assignedSkills));
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("group")) {
					groups.put(groupId, List.copyOf(skills));
					groupId = null;
					skills = null;
				}
			}
			reader.close();
			return new NpcSkillData(assignments);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load NPC skill definitions from " + file.getPath(), e);
		}
	}

	private static String attribute(XMLStreamReader reader, String name) {
		return reader.getAttributeValue(null, name);
	}

	private static int parseInt(String value, String field) {
		if (value == null) {
			throw new IllegalStateException("Missing " + field);
		}
		return parseInt(value, 0, field);
	}

	private static int parseInt(String value, int defaultValue, String field) {
		try {
			return value == null || value.isEmpty() ? defaultValue : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Invalid " + field + ": " + value, e);
		}
	}
}
