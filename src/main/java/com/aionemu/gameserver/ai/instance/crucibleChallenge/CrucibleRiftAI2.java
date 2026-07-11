package com.aionemu.gameserver.ai.instance.crucibleChallenge;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Crucible Challenge 副本 NPC AI：Crucible Rift（@AIName "cruciblerift"），继承 ActionItemNpcAI2。
 * Crucible Challenge instance NPC AI: Crucible Rift (@AIName "cruciblerift"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("cruciblerift")
public class CrucibleRiftAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
			case 730459: //Crucible Rift.
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			break;
			case 730460: //Crucible Rift.
				TeleportService2.teleportTo(player, 300320000, getPosition().getInstanceId(), 1759.5004f, 1273.5414f, 389.11743f, (byte) 10);
				spawn(205679, 1765.522f, 1282.1051f, 389.11743f, (byte) 0);
				AI2Actions.deleteOwner(this);
			break;
		}
	}
	
	@Override
	protected void handleSpawned() {
		switch (getNpcId()) {
			case 730459: //Crucible Rift.
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						announceCrucibleRift1();
					}
				}, 2000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						announceCrucibleRift2();
					}
				}, 6000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						announceCrucibleRift3();
					}
				}, 10000);
			break;
		}
		super.handleSpawned();
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && getNpcId() == 730459) { //Crucible Rift.
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
			TeleportService2.teleportTo(player, 300320000, getPosition().getInstanceId(), 1807.0531f, 306.2831f, 469.25f, (byte) 54);
			switch (player.getRace()) {
			    case ELYOS:
			        spawn(218200, 1765.4385f, 315.67407f, 469.25f, (byte) 114); //Rank 5, Asmodian Soldier Mediatec.
			    break;
				case ASMODIANS:
			        spawn(218192, 1765.4385f, 315.67407f, 469.25f, (byte) 114); //Rank 5, Elyos Soldier Odos.
			    break;
			}
			AI2Actions.deleteOwner(this);
		}
		return true;
	}
	
	private void announceCrucibleRift1() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 万克特里斯特消失处出现试炼裂隙。我得去调查！ / A Crucible Rift has appeared at the spot where Vanktrist vanished. I'd better go investigate!
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111482, player.getObjectId(), 2));
				}
			}
		});
	}
	
	private void announceCrucibleRift2() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 嗯，正如我所料……提亚马特的龙族已渗透试炼场。 / Hmm, just as I suspected... Tiamat's Balaur have infiltrated the Crucible.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111483, player.getObjectId(), 2));
				}
			}
		});
	}
	
	private void announceCrucibleRift3() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 奇怪。看起来像试炼场……但不是我们的。不知道属于谁？ / Weird. It looks like a Crucible... just not OUR Crucible. I wonder who it belongs to?
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111484, player.getObjectId(), 2));
				}
			}
		});
	}
}
