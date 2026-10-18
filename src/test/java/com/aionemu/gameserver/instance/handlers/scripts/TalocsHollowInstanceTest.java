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
	void handlerRegistersGuaranteedDropsOnce() throws IOException {
		String onDropRegistered = methodBody(Files.readString(SOURCE), "public void onDropRegistered(Npc npc)");

		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 185000088, 1);"));
		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 164000137, 1);"));
		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 185000108, 1);"));
		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 164000139, 1);"));
		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 164000138, 1);"));
		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 190080005, 2);"));
		assertTrue(onDropRegistered.contains("registerGuaranteedDrop(dropItems, npc, 190200000, 50);"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 185000088, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 164000137, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 185000108, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 164000139, 1)"));
		assertFalse(onDropRegistered.contains("regDropItem(1, 0, npcId, 164000138, 1)"));
	}

	@Test
	void guaranteedDropReplacesLowChanceRetailEntry() throws IOException {
		String helper = methodBody(Files.readString(SOURCE),
				"private void registerGuaranteedDrop(Set<DropItem> dropItems, Npc npc, int itemId, long count)");

		assertTrue(helper.contains("removeIf(dropItem -> dropItem.getDropTemplate().getItemId() == itemId)"),
			"零售低概率条目必须先移除，避免保底失效");
		assertTrue(helper.contains("regDropItem(1, 0, npc.getObjectId(), itemId, count)"),
			"保底条目必须以 100% 登记在正确对象上");
	}

	@Test
	void baseDropDataKeepsOneEntryPerSpecialItem() throws IOException {
		String npcDrops = Files.readString(NPC_DROPS);

		assertSingleDropEntry(npcDrops, 215456, 185000088);
		assertSingleDropEntry(npcDrops, 215456, 164000137);
		assertSingleDropEntry(npcDrops, 215478, 185000108);
		assertSingleDropEntry(npcDrops, 215478, 164000139);
		assertSingleDropEntry(npcDrops, 215482, 164000138);
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

	private static void assertSingleDropEntry(String npcDrops, int npcId, int itemId) {
		String npcDrop = npcDropBody(npcDrops, npcId);
		String item = "item_id=\"" + itemId + "\"";
		int firstOccurrence = npcDrop.indexOf(item);
		int lastOccurrence = npcDrop.lastIndexOf(item);

		assertTrue(firstOccurrence >= 0);
		assertEquals(firstOccurrence, lastOccurrence);
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
