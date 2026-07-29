package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.RecipeData;

import jakarta.xml.bind.JAXBContext;

/** 验证当前 work_order owner 的配方引用全部存在于正式 recipe 数据。 / Verifies all current work_order recipe references exist in formal recipe data. */
class QuestGraphRecipeReferenceCatalogTest {

	/** 加载 574 个 work_order owner 并验证完整 recipe 引用闭包。 / Loads 574 work_order owners and verifies complete recipe reference closure. */
	@Test
	void currentWorkOrderOwnersHaveFormalRecipeReferences() throws Exception {
		File recipesFile = new File("src/main/resources/aion/data/static_data/recipe/recipe_templates.xml");
		RecipeData recipes = (RecipeData) JAXBContext.newInstance(RecipeData.class).createUnmarshaller().unmarshal(recipesFile);
		Set<Integer> references = QuestGraphRecipeReferenceCatalog.build(recipes);
		Set<Integer> ownerRecipeIds = new LinkedHashSet<>();
		int owners = readWorkOrderOwners(
			new File("src/main/resources/aion/data/static_data/quest_script_data/work_order.xml"), ownerRecipeIds);

		assertEquals(574, owners);
		assertTrue(references.containsAll(ownerRecipeIds));
		assertTrue(ownerRecipeIds.containsAll(Set.of(155004001, 155009001, 155009287)));
	}

	/** 使用 StAX 只读取 work_order 的强类型 recipe ID。 / Uses StAX to read only typed recipe ids from work_order owners. */
	private static int readWorkOrderOwners(File file, Set<Integer> recipeIds) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		int owners = 0;
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("work_order")) {
						owners++;
						recipeIds.add(Integer.parseInt(reader.getAttributeValue(null, "recipe_id")));
					}
				}
			} finally {
				reader.close();
			}
		}
		return owners;
	}
}
