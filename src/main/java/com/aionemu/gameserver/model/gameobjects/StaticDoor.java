package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.EnumSet;

import com.aionemu.gameserver.controllers.StaticObjectController;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.staticdoor.StaticDoorState;
import com.aionemu.gameserver.model.templates.staticdoor.StaticDoorTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 静态 Door 游戏对象。
 * Static Door game object.
 *
 * @author MrPoke
 */
public class StaticDoor extends StaticObject {

	private EnumSet<StaticDoorState> states;

	/**
	 * 构造静态门。
	 * Constructs a static door.
	 *
	 * @param objectId 对象 ID / object id
	 * @param controller 静态对象控制器 / static-object controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate 静态门模板 / static-door template
	 */
	public StaticDoor(int objectId, StaticObjectController controller, SpawnTemplate spawnTemplate,
			StaticDoorTemplate objectTemplate, int instanceId) {
		super(objectId, controller, spawnTemplate, objectTemplate);
		states = EnumSet.copyOf(getObjectTemplate().getInitialStates());
	}

	/**
	 * 是否处于开启状态。
	 * Whether the door is open.
	 *
	 * @return 是否开启 / whether open
	 */
	public boolean isOpen() {
		return states.contains(StaticDoorState.OPENED);
	}

	/** 返回状态集合 / Returns the states */
	public EnumSet<StaticDoorState> getStates() {
		return states;
	}

	/**
	 * 设置门的开启状态并广播表情包。
	 * Sets the door's open state and broadcasts the emotion packet.
	 *
	 * @param open 是否开启 / whether open
	 */
	public void setOpen(boolean open) {
		EmotionType emotion;
		int packetState = 0; // 不重要，与内部状态类似 / not important, similar to internal state
		if (open) {
			emotion = EmotionType.OPEN_DOOR;
			states.remove(StaticDoorState.CLICKABLE);
			states.add(StaticDoorState.OPENED); // 1001
			packetState = 0x9;
		} else {
			emotion = EmotionType.CLOSE_DOOR;
			if (getObjectTemplate().getInitialStates().contains(StaticDoorState.CLICKABLE)) {
				states.add(StaticDoorState.CLICKABLE);
			}
			states.remove(StaticDoorState.OPENED); // 1010
			packetState = 0xA;
		}
		GameWorldServices.geoService().setDoorState(getWorldId(), getInstanceId(), getSpawn().getStaticId(), open);
		// int stateFlags = StaticDoorState.getFlags(states);
		PacketSendUtility.broadcastPacket(this, new SM_EMOTION(this.getSpawn().getStaticId(), emotion, packetState));
	}

	/** 更换状态 / Change state. */
	public void changeState(boolean open, int state) {
		state = state & 0xF;
		StaticDoorState.setStates(state, states);
		EmotionType emotion = open ? emotion = EmotionType.OPEN_DOOR : EmotionType.CLOSE_DOOR;
		PacketSendUtility.broadcastPacket(this, new SM_EMOTION(this.getSpawn().getStaticId(), emotion, state));
	}

	/** 获取对象模板。 / Returns the object template. */
	@Override
	public StaticDoorTemplate getObjectTemplate() {
		return (StaticDoorTemplate) super.getObjectTemplate();
	}
}
