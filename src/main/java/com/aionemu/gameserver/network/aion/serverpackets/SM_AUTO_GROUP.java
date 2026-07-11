package com.aionemu.gameserver.network.aion.serverpackets;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 自动组队/副本匹配与战场匹配 UI 状态的服务端包（窗口、等待、入场、失败等）。
 * instance-match and battleground-match UI state
 * (entry, waiting, enter, fail windows, etc.).
 */
public class SM_AUTO_GROUP extends AionServerPacket {
	private byte windowId;
	private int instanceMaskId;
	private int mapId;
	private int messageId;
	private int titleId;
	private int waitTime;
	private boolean close;
	String name = StringUtils.EMPTY;
	public static final byte wnd_EntryIcon = 6;

	/**
	 * 按实例 mask 初始化副本匹配基础字段。
	 * Initializes instance-match base fields from the instance mask id.
	 *
	 * instance mask id
	 */
	public SM_AUTO_GROUP(int instanceMaskId) {
		this.isBG = false;
		AutoGroupType agt = AutoGroupType.getAGTByMaskId(instanceMaskId);
		this.instanceMaskId = instanceMaskId;
		this.messageId = agt.getNameId();
		this.titleId = agt.getTitleId();
		this.mapId = agt.getInstanceMapId();
	}

	/**
	 * instance mask id
	 * @param windowId 客户端窗口 ID / client window id
	 */
	public SM_AUTO_GROUP(int instanceMaskId, Number windowId) {
		this(instanceMaskId);
		this.windowId = windowId.byteValue();
		this.isBG = false;
	}

	/**
	 * instance mask id
	 * @param windowId 客户端窗口 ID / client window id
	 * @param close 是否关闭入口图标 / whether to close the entry icon
	 */
	public SM_AUTO_GROUP(int instanceMaskId, Number windowId, boolean close) {
		this(instanceMaskId);
		this.windowId = windowId.byteValue();
		this.close = close;
		this.isBG = false;
	}

	/**
	 * instance mask id
	 * @param windowId 客户端窗口 ID / client window id
	 * @param waitTime 等待时间（秒） / wait time in seconds
	 * @param name 显示名称 / display name
	 */
	public SM_AUTO_GROUP(int instanceMaskId, Number windowId, int waitTime, String name) {
		this(instanceMaskId);
		this.windowId = windowId.byteValue();
		this.waitTime = waitTime;
		this.name = name;
		this.isBG = false;
	}

	// 用于战场系统 / For BG System
	private boolean isBG;
	private int option = 0;
	private int extraOption = 0;
	private int worldId = 0;
	private int specialOption = 0;

	/**
	 * 战场入口显示/隐藏。
	 * Battleground entry show/hide.
	 *
	 * world map id
	 * @param show 是否显示入口 / whether to show the entry
	 */
	public SM_AUTO_GROUP(int worldId, boolean show) {
		this.isBG = true;
		this.option = 6;
		this.extraOption = 1;
		this.worldId = worldId;
		this.specialOption = show ? 1 : 0;
	}

	/**
	 * 战场组队选择/提示消息。
	 * prompt message. / prompt message.
	 *
	 * world map id
	 * @param teamChoice 是否为队伍选择模式 / whether team-choice mode
	 * message id
	 */
	public SM_AUTO_GROUP(int worldId, boolean teamChoice, int messageId) {
		this.isBG = true;
		this.option = 0;
		this.extraOption = teamChoice ? 2 : 0;
		this.worldId = worldId;
		this.messageId = messageId;
	}

	/**
	 * 战场通用选项包。
	 * Generic battleground option packet.
	 *
	 * main option
	 * world map id
	 * special option
	 */
	public SM_AUTO_GROUP(int option, int worldId, int specialOption) {
		this.isBG = true;
		this.option = option;
		this.extraOption = 0;
		this.worldId = worldId;
		this.specialOption = specialOption;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if (!isBG) {
			writeD(this.instanceMaskId);
			writeC(this.windowId);
			writeD(this.mapId);
			switch (this.windowId) {
			case 0: // Request Entry
				writeD(this.messageId);
				writeD(this.titleId);
				writeD(0);
				break;
			case 1: // Waiting Window
				writeD(0);
				writeD(0);
				writeD(this.waitTime);
				break;
			case 2: // Cancel Looking
				writeD(0);
				writeD(0);
				writeD(0);
				break;
			case 3: // Pass Window
				writeD(0);
				writeD(0);
				writeD(this.waitTime);
				break;
			case 4: // Enter Window
				writeD(0);
				writeD(0);
				writeD(0);
				break;
			case 5: // After You Click Enter
				writeD(0);
				writeD(0);
				writeD(0);
				break;
			case wnd_EntryIcon:
				writeD(this.messageId);
				writeD(this.titleId);
				writeD(this.close ? 0 : 1);
				break;
			case 7: // Failed Window
				writeD(this.messageId);
				writeD(this.titleId);
				writeD(0);
				break;
			case 8:
				writeD(0);
				writeD(0);
				writeD(this.waitTime);
				break;
			}
			writeC(0);
			writeS(this.name);
		} else {
			writeD(extraOption);
			writeC(option);
			writeD(worldId);
			writeD(titleId);
			writeD(messageId);
			writeD(specialOption);
			writeH(0);
			writeC(0);
		}
	}
}
