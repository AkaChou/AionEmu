package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.arcadeupgrade.ArcadeTab;
import com.aionemu.gameserver.model.templates.arcadeupgrade.ArcadeTabItem;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.events.ArcadeUpgradeService;

/**
 * 升级街机（Arcade Upgrade）活动交互的服务端包。
 * Server packet for Arcade Upgrade event interactions.
 * <p>
 * 通过 {@code action} 区分：图标显示、会话初始化、成功判定、等级、奖励、狂热状态与奖池等。
 * Uses {@code action} for: icon display, session init, success result, level, reward, frenzy state, and prize pool.
 *
 * @author Ranastic
 */
public class SM_UPGRADE_ARCADE extends AionServerPacket {
	private int action;
	private int showicon = 1;
	private int frenzyPoints = 0;
	private boolean success = false;
	private int level;
	private ArcadeTabItem itemList;
	private int sessionId = 64519;
	private Player player;
	private int frenzyTime;
	private int frenzyCount;

	/**
	 * 图标显示。
	 * Icon display.
	 *
	 * @param showicon 是否显示图标 / whether to show the icon
	 */
	public SM_UPGRADE_ARCADE(boolean showicon) {
		this.action = 0;
		this.showicon = showicon ? 1 : 0;
	}

	/**
	 * 会话初始化（狂热点数）。
	 * Session init with frenzy points.
	 *
	 * frenzy points
	 * frenzy count
	 */
	public SM_UPGRADE_ARCADE(int frenzyPoints, int frenzyCount) {
		this.action = 1;
		this.frenzyPoints = frenzyPoints;
		this.frenzyCount = frenzyCount;
	}

	/**
	 * 通用 action。
	 * Generic action.
	 *
	 * action type
	 */
	public SM_UPGRADE_ARCADE(int action) {
		this.action = action;
	}

	/**
	 * 升级结果。
	 * Upgrade result.
	 *
	 * action type
	 * whether successful
	 * frenzy points
	 */
	public SM_UPGRADE_ARCADE(int action, boolean success, int frenzy) {
		this.action = action;
		this.success = success;
		this.frenzyPoints = frenzy;
	}

	/**
	 * 等级相关。
	 * Level-related.
	 *
	 * 玩家 / player
	 * action type
	 * level
	 */
	public SM_UPGRADE_ARCADE(Player player, int action, int level) {
		this.action = action;
		this.level = level;
		this.player = player;
	}

	/**
	 * 奖励物品。
	 * Reward item.
	 *
	 * action type
	 * reward item
	 */
	public SM_UPGRADE_ARCADE(int action, ArcadeTabItem itemList) {
		this.action = action;
		this.itemList = itemList;
	}

	/**
	 * 狂热时间与次数。
	 * Frenzy time and count.
	 *
	 * action type
	 * @param frenzyTime  狂热剩余时间 / remaining frenzy time
	 * frenzy count
	 */
	public SM_UPGRADE_ARCADE(int action, int frenzyTime, int frenzyCount) {
		this.action = action;
		this.frenzyTime = frenzyTime;
		this.frenzyCount = frenzyCount;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
		case 0:
			writeD(this.showicon);
			break;
		case 1:
			writeD(sessionId);
			writeD(frenzyPoints);
			writeD(frenzyCount);
			writeD(1);
			writeD(4);
			writeD(6);
			writeD(8);
			writeD(8);
			writeH(272);
			writeS("success_weapon01");
			writeS("success_weapon01");
			writeS("success_weapon01");
			writeS("success_weapon02");
			writeS("success_weapon02");
			writeS("success_weapon03");
			writeS("success_weapon03");
			writeS("success_weapon04");
			break;
		case 2:
			writeD(sessionId);
			break;
		case 3:
			writeC(success ? 1 : 0);
			writeD(frenzyPoints > 100 ? 100 : frenzyPoints);
			break;
		case 4:
			writeD(level);
			break;
		case 5:
			writeD(level);
			writeC(level >= 6 && !player.getUpgradeArcade().isReTry() ? 1 : 0);
			writeD(level >= 6 && !player.getUpgradeArcade().isReTry() ? 2 : 0);
			writeD(0);
			player.getUpgradeArcade().setReTry(false);
			player.getUpgradeArcade().setFailed(false);
			break;
		case 6:
			writeD(itemList.getItemId());
			writeD(itemList.getNormalCount() > 0 ? this.itemList.getNormalCount() : this.itemList.getFrenzyCount());
			writeD(0);
			break;
		case 7:
			writeD(frenzyTime);
			writeD(frenzyCount);
			break;
		case 8:
			writeD(1);
			writeD(0);
			break;
		case 10:
			List<ArcadeTab> tabs = GameFeatureServices.arcadeUpgradeService().getTabs();
			for (ArcadeTab tab : tabs) {
				writeC(tab.getArcadeTabItems().size());
			}
			for (ArcadeTab arcadetab : tabs) {
				for (ArcadeTabItem arcadetabitem : arcadetab.getArcadeTabItems()) {
					writeD(arcadetabitem.getItemId());
					writeD(arcadetabitem.getNormalCount());
					writeD(0);
					writeD(arcadetabitem.getFrenzyCount());
					writeD(0);
				}
			}
			break;
		}
	}
}
