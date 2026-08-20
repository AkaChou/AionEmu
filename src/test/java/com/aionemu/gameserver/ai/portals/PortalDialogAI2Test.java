package com.aionemu.gameserver.ai.portals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestDialog;

class PortalDialogAI2Test {

	@Test
	void fissureOrbTriesTheQuestStartActionBeforeShowingThePortalMenu() {
		assertEquals(List.of(QuestDialog.START_DIALOG.id()), PortalDialogAI2.questFirstDialogIds(834194, 29));
	}

	@Test
	void entranceExitWithTheSameNpcTemplateKeepsItsNormalPortalMenu() {
		assertEquals(List.of(), PortalDialogAI2.questFirstDialogIds(834194, 278));
		assertEquals(List.of(), PortalDialogAI2.questFirstDialogIds(834195, 29));
	}
}
