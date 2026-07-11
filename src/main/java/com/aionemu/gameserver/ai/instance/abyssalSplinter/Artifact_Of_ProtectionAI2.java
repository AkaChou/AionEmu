package com.aionemu.gameserver.ai.instance.abyssalSplinter;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Abyssal Splinter 副本 NPC AI：Artifact Of Protection（@AIName "Artifact_Of_Protection"），继承 NpcAI2。
 * Abyssal Splinter instance NPC AI: Artifact Of Protection (@AIName "Artifact_Of_Protection"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Artifact_Of_Protection")
public class Artifact_Of_ProtectionAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
		    switch (getNpcId()) {
				case 700856: //Artifact Of Protection.
				    switch (player.getWorldId()) {
					    case 300220000: //Abyssal Splinter.
							announceArtifactProtector();
							spawn(216952, 329.61923f, 733.8852f, 197.60815f, (byte) 80); //Yamennes Blindsight.
						break;
					    case 300600000: //Unstable Abyssal Splinter.
							announceArtifactProtector();
							spawn(219555, 329.61923f, 733.8852f, 197.60815f, (byte) 80); //Durable Yamennes Blindsight.
						break;
					}
				break;
			}
		} else if (dialogId == 10001) {
		    switch (getNpcId()) {
				case 700856: //Artifact Of Protection.
				    switch (player.getWorldId()) {
					    case 300220000: //Abyssal Splinter.
							announceFerociousProtector();
							spawn(216960, 329.61923f, 733.8852f, 197.60815f, (byte) 80); //Yamennes Painflare.
						break;
					    case 300600000: //Unstable Abyssal Splinter.
							announceFerociousProtector();
							spawn(219563, 329.61923f, 733.8852f, 197.60815f, (byte) 80); //Unstable Yamennes Painflare.
						break;
					}
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}
	
	private void announceArtifactProtector() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 出现了神器守护者！ / An Artifact Protector has appeared!
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdD_Wakeup);
			}
		});
	}
	private void announceFerociousProtector() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 出现了凶猛的神器守护者！ / A Ferocious Artifact Protector has appeared!
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdDH_Wakeup);
			}
		});
	}
}
