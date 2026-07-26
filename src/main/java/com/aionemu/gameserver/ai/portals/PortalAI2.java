package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalUse;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.PortalService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 传送门/传送点 AI：Portal（@AIName "portal"），继承 ActionItemNpcAI2。
 * Portal/teleporter AI: Portal (@AIName "portal"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("portal")
public class PortalAI2 extends ActionItemNpcAI2
{
	protected PortalUse portalUse;
	protected TeleporterTemplate teleportTemplate;
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return true;
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		teleportTemplate = DataManager.TELEPORTER_DATA.getTeleporterTemplateByNpcId(getNpcId());
		portalUse = DataManager.PORTAL2_DATA.getPortalUse(getNpcId());
		switch (getNpcId()) {
		    case 802219: //Advance Corridor [Arcadian Fortress].
			case 802221: //Advance Corridor [Umbral Fortress].
			case 802223: //Advance Corridor [Eternum Fortress].
			case 802225: //Advance Corridor [Skyclash Fortress].
			    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						startLifeTask();
					}
				}, 1000);
			break;
        }
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			        @Override
			        public void visit(Player player) {
						AI2Actions.deleteOwner(PortalAI2.this);
						// 进阶走廊关闭后，你将返回所用入口。 / You will be returned to the entrance you used upon closure of the Advance Corridor.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_SVS_DIRECT_PORTAL_CLOSE_COMPULSION_TELEPORT);
			        }
				});
			}
		}, 600000); //10 Minutes.
	}
	
	@Override
	protected void handleDialogStart(Player player) {
		AI2Actions.selectDialog(this, player, 0, -1);
		if (getTalkDelay() != 0) {
			super.handleDialogStart(player);
		} else {
			handleUseItemFinish(player);
		}
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		if (portalUse != null) {
				PortalPath portalPath = portalUse.getPortalPath(player.getRace(), player.getWorldId());
			if (portalPath != null) {
				PortalService.port(portalPath, player, getObjectId());
			}
		} else if (teleportTemplate != null) {
			TeleportLocation loc = teleportTemplate.getTeleLocIdData().getTelelocations().get(0);
			if (loc != null) {
				TeleportService2.teleport(teleportTemplate, loc.getLocId(), player, getOwner(), TeleportAnimation.BEAM_ANIMATION);
			}
		}
	}
}
