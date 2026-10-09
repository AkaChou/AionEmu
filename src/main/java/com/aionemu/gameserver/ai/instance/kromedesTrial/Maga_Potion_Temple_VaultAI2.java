package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.KromedesTrialInstance;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Maga Potion Temple Vault（@AIName "maga_potion_1"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Maga Potion Temple Vault (@AIName "maga_potion_1"), extends NpcAI2.
 * @author Encom
 */
@AIName("maga_potion_1")
public class Maga_Potion_Temple_VaultAI2 extends NpcAI2
{
	private static final int QUEST_ID = 18602;
	private static final int RELIC_KEY_ID = 185000109;
	/**
	 * 原版 idcromede_alias_02 落点（Map/Worlds/idcromede/world.xml）：「拿出药水，瞬间移动到宅邸」的目标坐标/朝向。
	 * 注意朝向单位：原版 dir 为度数（270°），服务端存储用压缩 byte heading（度/3，0-120），故 270° = 90。
	 * The retail idcromede_alias_02 point (Map/Worlds/idcromede/world.xml): where "teleport to the manor" lands.
	 * Heading units: retail 'dir' is degrees (270°); the server stores a compressed byte heading (deg/3, 0-120),
	 * so 270° maps to 90.
	 */
	private static final float HOME_X = 687.631104f;
	private static final float HOME_Y = 675.972412f;
	private static final float HOME_Z = 201.040802f;
	private static final byte HOME_HEADING = (byte) 90;

	@Override
    protected void handleDialogStart(Player player) {
        QuestState questState = player.getQuestStateList().getQuestState(QUEST_ID);
        boolean potionStep = questState != null && questState.getStatus() == QuestStatus.START
                && questState.getQuestVarById(0) == 1;
        boolean hasKey = player.getInventory().getFirstItemByItemId(RELIC_KEY_ID) != null;
        // 原版 730308（fun_896.cpp FUN_180f961e0）：用药水入口只看钥匙（任务第 2 步时同页）。页面必须携带
        // questId，否则客户端会按其它进行中任务渲染该页（2026-10-07 实机：页 1011 被渲染成 18605 的对话，
        // 玩家点到别任务的按钮后无人认领、交互死锁）。
        // Retail 730308 (fun_896.cpp FUN_180f961e0): the potion entry keys on the relic key alone (same page
        // while the quest sits at step 1). The page must carry questId, or the client renders another quest's
        // dialog from it (live 2026-10-07: page 1011 rendered as the 18605 conversation and deadlocked).
        if ((potionStep || hasKey)
                && QuestDialogContract.loadDefault().hasButtonPage(QUEST_ID, QuestDialogPage.SELECT2.id())) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), QuestDialogPage.SELECT2.id(), QUEST_ID));
            return;
        }
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
    }

	@Override
	public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, resolveQuestId(player, questId), dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env)) {
			if (dialogId == QuestDialogAction.SETPRO2.id()
				&& getPosition().getWorldMapInstance().getInstanceHandler()
					instanceof KromedesTrialInstance instanceHandler) {
				instanceHandler.synchronizeRobstinNpc(player);
			}
			return true;
		}
		// 引擎不认领时的兜底（对齐原版 FUN_180f961e0）：SETPRO2「拿出药水，瞬间移动到宅邸」只看钥匙——
		// 有钥匙：扣 1 把 +（任务在第 2 步时）记 var0=2 + 切换罗勃斯汀 + 传送到宅邸；无钥匙：失败页。
		// The engine-miss fallback mirroring retail FUN_180f961e0: SETPRO2 keys on the relic key alone —
		// with a key: consume one, mark var0=2 (only from step 1), sync Robstin, teleport to the manor;
		// without a key: the client-declared check-fail page.
		if (dialogId == QuestDialogAction.SETPRO2.id()) {
			if (player.getInventory().getFirstItemByItemId(RELIC_KEY_ID) == null) {
				int failPage = QuestDialogContract.loadDefault().checkFailPage(QUEST_ID);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(),
					failPage > 0 ? failPage : QuestDialogPage.CHECK_USER_ITEM_FAIL.id(), QUEST_ID));
				return true;
			}
			// 先扣物（布尔守卫）；AI 兜底为非事务路径，随周期存盘落库。
			// Consume first (boolean guard); the AI fallback is non-transactional and persists on periodic save.
			if (!player.getInventory().decreaseByItemId(RELIC_KEY_ID, 1)) {
				return true;
			}
			QuestState questState = player.getQuestStateList().getQuestState(QUEST_ID);
			if (questState != null && questState.getStatus() == QuestStatus.START
					&& questState.getQuestVarById(0) == 1) {
				questState.setQuestVarById(0, 2);
				PacketSendUtility.sendPacket(player, SM_QUEST_ACTION.updateQuest(
					QUEST_ID, questState.getStatus(), questState.getQuestVars().getQuestVars()));
			}
			if (getPosition().getWorldMapInstance().getInstanceHandler()
					instanceof KromedesTrialInstance instanceHandler) {
				instanceHandler.synchronizeRobstinNpc(player);
			}
			TeleportService2.teleportTo(player, 300230000, getPosition().getInstanceId(),
				HOME_X, HOME_Y, HOME_Z, HOME_HEADING);
			return true;
		}
		// 「仔细查看水晶球」翻页（SELECT2_1）与首屏翻页（SELECT1_1）在引擎未认领时按契约原样回发，
		// 收尾动作（FINISH_DIALOG）关窗——回发前以客户端契约 fail-closed，避免下发未声明页触发 load fail。
		// The "inspect the crystal" page turns (SELECT2_1/SELECT1_1) echo per contract when the engine misses;
		// the finish action closes the window. Undeclared pages fail closed to avoid a client load fail.
		if ((dialogId == QuestDialogAction.SELECT2_1.id() || dialogId == QuestDialogAction.SELECT1_1.id())
				&& QuestDialogContract.loadDefault().hasButtonPage(QUEST_ID, dialogId)) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, QUEST_ID));
			return true;
		}
		if (dialogId == QuestDialogAction.FINISH_DIALOG.id()) {
			DialogService.closeDialog(getOwner(), player);
			return true;
		}
		return true;
	}

	/**
	 * 客户端有时不会把任务 ID 带回连续任务页；活动步骤明确时补回本 NPC 的任务 owner。
	 * The client may omit the quest ID on a follow-up page; restore this NPC's quest owner while its step is active.
	 */
	private int resolveQuestId(Player player, int questId) {
		if (questId != 0) {
			return questId;
		}
		QuestState questState = player.getQuestStateList().getQuestState(QUEST_ID);
		return questState != null && questState.getStatus() == QuestStatus.START
			&& questState.getQuestVarById(0) == 1 ? QUEST_ID : 0;
	}
}
