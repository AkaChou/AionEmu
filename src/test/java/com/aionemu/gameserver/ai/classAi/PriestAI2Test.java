package com.aionemu.gameserver.ai.classAi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Servant;

class PriestAI2Test {

	@Test
	void servantsAreLevelMatchedCreatorScopedAndRaceSafe() throws Exception {
		assertEquals(280638, PriestAI2.getServantNpcId(280635));
		assertEquals(280639, PriestAI2.getServantNpcId(280636));
		assertEquals(280640, PriestAI2.getServantNpcId(280637));
		assertEquals(281301, PriestAI2.getServantNpcId(281300));
		assertEquals(0, PriestAI2.getServantNpcId(1));

		ObjenesisStd objenesis = new ObjenesisStd();
		Npc owner = objenesis.newInstance(Npc.class);
		Npc otherOwner = objenesis.newInstance(Npc.class);
		Servant servant = objenesis.newInstance(Servant.class);
		servant.setCreator(owner);

		assertTrue(PriestAI2.isOwnedServant(servant, owner));
		assertFalse(PriestAI2.isOwnedServant(servant, otherOwner));
		assertFalse(PriestAI2.isOwnedServant(owner, owner));

		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/ai/classAi/PriestAI2.java"));
		assertTrue(source.contains("synchronized (servantLock)"));
		assertTrue(source.contains("canSpawnServants = false;\n\t\t\tcancelPhaseTask();\n\t\t\tdeleteHelpers();"));

		String skills = Files.readString(Path.of("src/main/resources/aion/definitions/compact/npc-skills.xml"));
		assertTrue(skills.contains("281301"));
	}
}
