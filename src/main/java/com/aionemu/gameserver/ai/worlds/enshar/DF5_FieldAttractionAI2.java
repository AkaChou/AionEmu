package com.aionemu.gameserver.ai.worlds.enshar;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Enshar 区域 NPC AI：DF5 Field Attraction（@AIName "df5_field_attraction"），继承 NpcAI2。
 * Enshar zone NPC AI: DF5 Field Attraction (@AIName "df5_field_attraction"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("df5_field_attraction")
public class DF5_FieldAttractionAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	/**
	 * 处理对话选择：按 NPC 将玩家传送到对应的飞行路径起始点。
	 * Handles dialog selection: starts the matching flight path for the player by NPC.
	 *
	 * @param player 对话玩家 / dialog player
	 * @param dialogId 对话框选项 ID / dialog option ID
	 * @param questId 任务 ID / quest ID
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @return 始终为 true / always true
	 */
    @Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
        if (dialogId == 10000) {
            switch (getNpcId()) {
                case 804822: // 遗弃的水桶 / Abandoned Bucket
                    player.setState(CreatureState.FLIGHT_TELEPORT);
					player.unsetState(CreatureState.ACTIVE);
					player.setFlightTeleportId(286001);
					PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 286001, 0));
                break;
                case 804823: // 摇摆的蘑菇 / Bobbling Mushroom
                    player.setState(CreatureState.FLIGHT_TELEPORT);
					player.unsetState(CreatureState.ACTIVE);
					player.setFlightTeleportId(284001);
					PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 284001, 0));
                break;
                case 804824: // 软软的水母 / Squishy Jellyfish
                    player.setState(CreatureState.FLIGHT_TELEPORT);
					player.unsetState(CreatureState.ACTIVE);
					player.setFlightTeleportId(283001);
					PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 283001, 0));
                break;
                case 804825: // 鲜嫩的水母 / Tender Jellyfish
                    player.setState(CreatureState.FLIGHT_TELEPORT);
					player.unsetState(CreatureState.ACTIVE);
					player.setFlightTeleportId(285001);
					PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 285001, 0));
                break;
            }
        }
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }
}
