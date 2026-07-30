package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.FlyPathData;

import jakarta.xml.bind.JAXBContext;

class QuestGraphFlightReferenceCatalogTest {
	private static final Path FORMAL_FLY_PATHS = Path.of("src/main/resources/aion/data/static_data/flypath_template.xml");

	@Test
	void buildsTheExactFormalFlyPathClosureIncludingHighIds() throws Exception {
		FlyPathData flyPaths = (FlyPathData) JAXBContext.newInstance(FlyPathData.class).createUnmarshaller()
			.unmarshal(FORMAL_FLY_PATHS.toFile());
		Set<Integer> references = QuestGraphFlightReferenceCatalog.build(flyPaths);

		assertEquals(316, references.size());
		assertTrue(references.containsAll(Set.of(1, 17, 31, 139, 279, 412, 423)));
		assertTrue(flyPaths.containsPath(423));
		assertFalse(references.contains(0));
		assertFalse(references.contains(417));
		assertFalse(references.contains(424));
		assertFalse(flyPaths.containsPath(424));
		assertThrows(UnsupportedOperationException.class, () -> references.add(424));
	}

	@Test
	void rejectsDuplicateIdsFromTheExplicitGenerationInput() throws Exception {
		String entry = "<flypath_location id=\"31\" sx=\"1\" sy=\"2\" sz=\"3\" sworld=\"4\" "
			+ "ex=\"5\" ey=\"6\" ez=\"7\" eworld=\"8\" time=\"9\"/>";
		FlyPathData duplicate = (FlyPathData) JAXBContext.newInstance(FlyPathData.class).createUnmarshaller()
			.unmarshal(new StringReader("<flypath_template>" + entry + entry + "</flypath_template>"));

		assertThrows(IllegalStateException.class, () -> QuestGraphFlightReferenceCatalog.build(duplicate));
	}
}
