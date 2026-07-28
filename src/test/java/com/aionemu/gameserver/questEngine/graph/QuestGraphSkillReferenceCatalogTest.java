package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;

/** 验证当前 skill-use owner 的技能引用全部存在于正式技能数据。 / Verifies all current skill-use owner references exist in formal skill data. */
class QuestGraphSkillReferenceCatalogTest {

	/** 加载 26 个 XML owner 与两个 Java owner 并验证完整 skill 引用闭包。 / Loads 26 XML owners plus two Java owners and verifies complete skill reference closure. */
	@Test
	void currentSkillUseOwnersHaveFormalSkillReferences() throws Exception {
		SkillData skillData = loadFormalSkillData();
		Set<Integer> references = QuestGraphSkillReferenceCatalog.build(skillData);
		Set<Integer> ownerSkillIds = new LinkedHashSet<>();
		int owners = 0;
		for (File file : List.of(
				new File("src/main/resources/aion/data/static_data/quest_script_data/sanctum.xml"),
				new File("src/main/resources/aion/data/static_data/quest_script_data/pandaemonium.xml"))) {
			owners += readSkillUseOwners(file, ownerSkillIds);
		}
		ownerSkillIds.addAll(Set.of(9832, 9833, 9834));

		assertEquals(26, owners);
		assertTrue(references.containsAll(ownerSkillIds));
		assertTrue(ownerSkillIds.containsAll(Set.of(599, 799, 890, 9832, 9833, 9834, 9912)));
	}

	/** 从仓库正式 definitions bundle 加载技能并恢复调用方配置。 / Loads skills from the formal repository definitions bundle and restores caller configuration. */
	private static SkillData loadFormalSkillData() {
		String property = "aion.game.definitions.dir";
		String previous = System.getProperty(property);
		System.setProperty(property, "src/main/resources/aion/definitions");
		try {
			return new XmlDataLoader().loadSkillData();
		} finally {
			if (previous == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, previous);
			}
		}
	}

	/** 使用 StAX 只读取 skill_use 下的强类型技能 ID 列表。 / Uses StAX to read only typed skill ids nested under skill_use owners. */
	private static int readSkillUseOwners(File file, Set<Integer> skillIds) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		int owners = 0;
		boolean insideSkillUse = false;
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			try {
				while (reader.hasNext()) {
					int token = reader.next();
					if (token == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("skill_use")) {
						insideSkillUse = true;
						owners++;
					} else if (token == XMLStreamConstants.START_ELEMENT && insideSkillUse && reader.getLocalName().equals("skill")) {
						for (String value : reader.getAttributeValue(null, "ids").trim().split("\\s+")) {
							skillIds.add(Integer.parseInt(value));
						}
					} else if (token == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("skill_use")) {
						insideSkillUse = false;
					}
				}
			} finally {
				reader.close();
			}
		}
		return owners;
	}
}
