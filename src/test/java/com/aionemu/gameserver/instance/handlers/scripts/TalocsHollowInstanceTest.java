package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TalocsHollowInstanceTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java");
	private static final Path NPC_DROPS = Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml");

	@Test
	void handlerDeduplicatesSpecialDropsPresentInBaseData() throws IOException {
		String onDropRegistered = methodBody(Files.readString(SOURCE), "public void onDropRegistered(Npc npc)");

		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 185000088, 1);"));
		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 164000137, 1);"));
		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 185000108, 1);"));
		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 164000139, 1);"));
		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 164000138, 1);"));
		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 190080005, 2);"));
		assertTrue(onDropRegistered.contains("registerDropItemIfAbsent(dropItems, npc, 190200000, 50);"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 185000088, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 164000137, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 185000108, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 164000139, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 164000138, 1)"));
	}

	@Test
	void fallbackRegistersOnlyMissingItemsAgainstTheCorrectObjectId() throws IOException {
		String helper = methodBody(Files.readString(SOURCE),
				"private void registerDropItemIfAbsent(Set<DropItem> dropItems, Npc npc, int itemId, long count)");
		int existingDropCheck = helper.indexOf("getDropTemplate().getItemId() == itemId");
		int addDrop = helper.indexOf(".add(", existingDropCheck);

		assertTrue(existingDropCheck >= 0);
		assertTrue(addDrop > existingDropCheck);
		assertTrue(helper.contains("npc.getObjectId()"));
	}

	@Test
	void baseDropDataProvidesTheGuaranteedSpecialItemsOnlyOnce() throws IOException {
		String npcDrops = Files.readString(NPC_DROPS);

		assertSingleGuaranteedDrop(npcDrops, 215456, 185000088);
		assertSingleGuaranteedDrop(npcDrops, 215456, 164000137);
		assertSingleGuaranteedDrop(npcDrops, 215478, 185000108);
		assertSingleGuaranteedDrop(npcDrops, 215478, 164000139);
		assertSingleGuaranteedDrop(npcDrops, 215482, 164000138);
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, signature + " must have a method body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}

	private static void assertSingleGuaranteedDrop(String npcDrops, int npcId, int itemId) {
		String npcDrop = npcDropBody(npcDrops, npcId);
		String item = "item_id=\"" + itemId + "\"";
		int firstOccurrence = npcDrop.indexOf(item);
		int lastOccurrence = npcDrop.lastIndexOf(item);

		assertTrue(firstOccurrence >= 0);
		assertEquals(firstOccurrence, lastOccurrence);
		assertTrue(npcDrop.substring(firstOccurrence, npcDrop.indexOf("/>", firstOccurrence))
				.contains("chance=\"100.00\" min_amount=\"1\" max_amount=\"1\""));
	}

	private static String npcDropBody(String source, int npcId) {
		String openingTag = "<npc_drop npc_id=\"" + npcId + "\">";
		int bodyStart = source.indexOf(openingTag);
		assertTrue(bodyStart >= 0, openingTag + " must exist");
		bodyStart += openingTag.length();
		int bodyEnd = source.indexOf("</npc_drop>", bodyStart);
		assertTrue(bodyEnd >= 0, "</npc_drop> must exist after " + openingTag);
		return source.substring(bodyStart, bodyEnd);
	}
}
