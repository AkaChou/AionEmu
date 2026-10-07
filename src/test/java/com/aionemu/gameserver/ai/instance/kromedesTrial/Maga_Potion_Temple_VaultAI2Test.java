package com.aionemu.gameserver.ai.instance.kromedesTrial;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 马加之药水（730308）对话行为回归测试：入口页必须携带 questId 且以客户端契约 fail-closed；
 * SETPRO2 兜底只看钥匙——扣 1 把 + 第 2 步记 var0=2 + 切换罗勃斯汀 + 传送到真端别名点；
 * 无钥匙下发失败页；翻页/收尾按契约处理。
 * Regression coverage for Maga's Potion (730308): questId-carrying entry pages, key-only SETPRO2
 * fallback (consume one + mark var0=2 from step 1 + sync Robstin + teleport to the retail alias
 * point), the declared check-fail page, and contract-driven page turns / dialog tail.
 */
class Maga_Potion_Temple_VaultAI2Test {
	private static final Path SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/ai/instance/kromedesTrial/Maga_Potion_Temple_VaultAI2.java");
	private static final String ON_DIALOG_SELECT =
		"public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex)";

	@Test
	void entryPageCarriesTheQuestIdAndFailsClosedOnUndeclaredPages() throws IOException {
		String source = Files.readString(SOURCE);
		String start = methodBody(source, "protected void handleDialogStart(Player player)");

		assertTrue(start.contains(
			"QuestDialogContract.loadDefault().hasButtonPage(QUEST_ID, QuestDialogPage.SELECT2.id())"));
		assertTrue(start.contains(
			"new SM_DIALOG_WINDOW(getObjectId(), QuestDialogPage.SELECT2.id(), QUEST_ID)"));
		// 旧版的双参 1011（questId=0，客户端按其它进行中任务渲染导致交互死锁）必须消失。
		// The old 2-arg page 1011 (questId=0, rendered as another quest's dialog, live deadlock) must be gone.
		assertFalse(source.contains("new SM_DIALOG_WINDOW(getObjectId(), 1011)"));
	}

	@Test
	void setproFallbackConsumesTheKeyMarksTheStepAndTeleportsToTheRetailAliasPoint() throws IOException {
		String select = methodBody(Files.readString(SOURCE), ON_DIALOG_SELECT);

		assertTrue(select.contains("decreaseByItemId(RELIC_KEY_ID, 1)"));
		assertTrue(select.contains("setQuestVarById(0, 2)"));
		assertTrue(select.contains("SM_QUEST_ACTION.updateQuest("));
		assertTrue(select.contains("synchronizeRobstinNpc(player)"));
		assertTrue(select.contains("HOME_X, HOME_Y, HOME_Z, HOME_HEADING"));
	}

	@Test
	void missingKeyFallsBackToTheDeclaredCheckFailPage() throws IOException {
		String select = methodBody(Files.readString(SOURCE), ON_DIALOG_SELECT);

		assertTrue(select.contains("checkFailPage(QUEST_ID)"));
		assertTrue(select.contains("QuestDialogPage.CHECK_USER_ITEM_FAIL.id()"));
	}

	@Test
	void pageTurnsAndDialogTailAreHandledPerContract() throws IOException {
		String select = methodBody(Files.readString(SOURCE), ON_DIALOG_SELECT);

		assertTrue(select.contains("QuestDialogAction.SELECT2_1.id()"));
		assertTrue(select.contains("QuestDialogAction.SELECT1_1.id()"));
		assertTrue(select.contains("QuestDialogAction.FINISH_DIALOG.id()"));
		assertTrue(select.contains("DialogService.closeDialog(getOwner(), player)"));
		assertTrue(select.contains("QuestDialogContract.loadDefault().hasButtonPage(QUEST_ID, dialogId)"));
	}

	@Test
	void homePointMatchesTheRetailIdcromedeAlias02Point() throws ReflectiveOperationException {
		assertEquals(687.631104f, staticFloat("HOME_X"));
		assertEquals(675.972412f, staticFloat("HOME_Y"));
		assertEquals(201.040802f, staticFloat("HOME_Z"));
		// 真端别名 dir 270° → 压缩 byte heading 90（度/3，0-120）。
		// Retail alias dir 270 deg -> compressed byte heading 90 (degrees/3, 0-120).
		assertEquals((byte) 90, staticByte("HOME_HEADING"));
	}

	private static float staticFloat(String name) throws ReflectiveOperationException {
		Field field = Maga_Potion_Temple_VaultAI2.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getFloat(null);
	}

	private static byte staticByte(String name) throws ReflectiveOperationException {
		Field field = Maga_Potion_Temple_VaultAI2.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getByte(null);
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
		throw new AssertionError(signature + " body must be balanced");
	}
}
