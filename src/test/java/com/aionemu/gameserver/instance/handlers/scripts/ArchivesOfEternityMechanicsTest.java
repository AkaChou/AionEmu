package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * 永恒档案库副本机制回归审计。
 * Regression audit for Archives Of Eternity instance mechanics.
 */
class ArchivesOfEternityMechanicsTest {

	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ArchivesOfEternityInstance.java");
	private static final Path LEVER_PATTERNS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/npcaipatterns_ideternity_kgw.xml");
	private static final Path SHIELD_PATTERNS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/npcaipatterns_ideternity_ssh.xml");

	@Test
	void enteringTheInstanceUsesTheEnterLifecycleHook() throws Exception {
		String source = Files.readString(HANDLER);
		int methodStart = source.indexOf("public void onEnterInstance(Player player)");
		int methodEnd = source.indexOf("private void spawnLibraryGuardianRace()", methodStart);
		String method = source.substring(methodStart, methodEnd);

		assertTrue(method.contains("super.onEnterInstance(player);"));
		assertFalse(method.contains("super.onInstanceCreate(instance);"));
	}

	@Test
	void leverAndShieldProgressionHasOneRuntimeOwner() throws Exception {
		String source = Files.readString(HANDLER);
		for (int npcId = 703009; npcId <= 703016; npcId++) {
			assertFalse(source.contains("case " + npcId + ":"), "duplicate Lever owner: " + npcId);
		}
		for (int npcId = 703017; npcId <= 703020; npcId++) {
			assertFalse(source.contains("deleteNpc(" + npcId + ")"), "duplicate Shield owner: " + npcId);
		}

		Document levers = parse(LEVER_PATTERNS);
		Document shields = parse(SHIELD_PATTERNS);
		for (int room = 1; room <= 4; room++) {
			String ordinal = ordinal(room);
			String shieldPattern = "IDEternity_01_" + ordinal + "_Shield_Remove";
			String messageType = Integer.toString(1000 + room);
			for (String leverType : new String[] { "Physical", "Magic" }) {
				String leverPattern = "IDEternity_01_" + ordinal + "_Lever_" + leverType;
				assertEquals(1, count(levers, "//npc_ai_pattern[name='" + leverPattern
					+ "']//on_die//message_type[text()='" + messageType + "']"));
			}
			assertEquals(2, count(shields, "//npc_ai_pattern[name='" + shieldPattern
				+ "']//on_message//message_type[text()='" + messageType + "']"));
			assertEquals(1, count(shields, "//npc_ai_pattern[name='" + shieldPattern
				+ "']//on_message//despawn_self"));
		}
	}

	@Test
	void secretTreasureCreatesOneRetailCryptographCube() throws Exception {
		String source = Files.readString(HANDLER);
		assertEquals(8, occurrences(source, "new SecretCubeSpot("));
		assertEquals(1, occurrences(source,
			"spawn(806139, cubeSpot.x(), cubeSpot.y(), cubeSpot.z(), MathUtil.convertDegreeToHeading(cubeSpot.heading()))"));
		assertFalse(source.contains("220334"), "Mimic must not replace the selected Cryptograph Cube");
		assertFalse(source.contains("deleteNpc(806139)"), "the selected cube must not be deleted by a second random branch");

		assertTrue(source.contains("(byte) 120"));
		assertTrue(source.contains("(byte) 240"));
		assertTrue(source.contains("(byte) 300"));
		assertTrue(source.contains("(byte) 180"));
	}

	private static Document parse(Path path) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
	}

	private static int count(Document document, String expression) throws Exception {
		return ((Double) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.NUMBER))
			.intValue();
	}

	private static int occurrences(String value, String needle) {
		int count = 0;
		int offset = 0;
		while ((offset = value.indexOf(needle, offset)) >= 0) {
			count++;
			offset += needle.length();
		}
		return count;
	}

	private static String ordinal(int room) {
		return switch (room) {
			case 1 -> "1st";
			case 2 -> "2nd";
			case 3 -> "3rd";
			case 4 -> "4th";
			default -> throw new IllegalArgumentException("Unsupported room: " + room);
		};
	}
}
