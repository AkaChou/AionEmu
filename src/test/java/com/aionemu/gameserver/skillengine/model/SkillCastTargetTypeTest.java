package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SkillCastTargetTypeTest {

	@Test
	void objectTargetCastTypesAcceptedFromClientAreEchoedByCastPackets() throws IOException {
		String clientPacket = read("src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_CASTSPELL.java");
		String startPacket = read("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_CASTSPELL.java");
		String resultPacket = read("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_CASTSPELL_RESULT.java");
		String skill = read("src/main/java/com/aionemu/gameserver/skillengine/model/Skill.java");

		assertTrue(clientPacket.contains("case 4:") && clientPacket.contains("case 87:"));
		assertObjectTargetCases(startPacket);
		assertObjectTargetCases(resultPacket);
		assertObjectTargetCases(slice(skill, "private void startCast()", "} else if (skillMethod == SkillMethod.ITEM"));
		assertObjectTargetCases(slice(skill, "private void sendCastspellEnd", "} else if (skillMethod == SkillMethod.ITEM"));
	}

	private static void assertObjectTargetCases(String source) {
		assertTrue(source.contains("case 4:"), "target type 4 should be serialized as object-target cast");
		assertTrue(source.contains("case 87:"), "target type 87 should be serialized as object-target cast");
	}

	private static String slice(String source, String start, String end) {
		int startIndex = source.indexOf(start);
		int endIndex = source.indexOf(end, startIndex);
		return source.substring(startIndex, endIndex);
	}

	private static String read(String path) throws IOException {
		return Files.readString(Path.of(path));
	}
}
