package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.PetEmote;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步宠物表情/动作（含移动停驻与目标点）的服务端包。
 * Server packet that synchronizes a pet emote/action (including move-stop and move-to).
 *
 * @author ATracer
 */
public class SM_PET_EMOTE extends AionServerPacket {

	private Pet pet;
	private PetEmote emote;
	private final float x, y, z, x2, y2, z2;
	private final byte heading;
	private int emotionId, param1;

	/**
	 * 仅表情、无坐标的构造。
	 * Emote-only constructor without coordinates.
	 *
	 * pet
	 * @param emote 表情类型 / emote type
	 */
	public SM_PET_EMOTE(Pet pet, PetEmote emote) {
		this(pet, emote, 0, 0, 0, (byte) 0);
	}

	/**
	 * 带当前位置与朝向的表情包（如 MOVE_STOP）。
	 * Emote packet with current position and heading (e.g. MOVE_STOP).
	 *
	 * pet
	 * @param emote 表情类型 / emote type
	 * @param x 当前位置 X / current X
	 * @param y 当前位置 Y / current Y
	 * @param z 当前位置 Z / current Z
	 * @param h 朝向 / heading
	 */
	public SM_PET_EMOTE(Pet pet, PetEmote emote, float x, float y, float z, byte h) {
		this(pet, emote, x, y, z, 0, 0, 0, h);
	}

	/**
	 * 带起止坐标的移动表情包（如 MOVETO）。
	 * Move emote packet with start and destination coordinates (e.g. MOVETO).
	 *
	 * pet
	 * @param emote 表情类型 / emote type
	 * @param x 起点 X / start X
	 * @param y 起点 Y / start Y
	 * @param z 起点 Z / start Z
	 * @param x2 终点 X / destination X
	 * @param y2 终点 Y / destination Y
	 * @param z2 终点 Z / destination Z
	 * @param h 朝向 / heading
	 */
	public SM_PET_EMOTE(Pet pet, PetEmote emote, float x, float y, float z, float x2, float y2, float z2, byte h) {
		this.pet = pet;
		this.emote = emote;
		this.x = x;
		this.y = y;
		this.z = z;
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
		this.heading = h;
	}

	/**
	 * 带情绪参数的表情包。
	 * Emote packet with emotion parameters.
	 *
	 * pet
	 * @param emote 表情类型 / emote type
	 * emotion id
	 * @param param1 附加参数（如心情增量） / extra parameter (e.g. happiness added)
	 */
	public SM_PET_EMOTE(Pet pet, PetEmote emote, int emotionId, int param1) {
		this(pet, emote);
		this.emotionId = emotionId;
		this.param1 = param1;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(pet.getObjectId());
		writeC(emote.getEmoteId());
		switch (emote) {
		case MOVE_STOP:
			writeF(x);
			writeF(y);
			writeF(z);
			writeC(heading);
			break;
		case MOVETO:
			writeF(x);
			writeF(y);
			writeF(z);
			writeC(heading);
			writeF(x2);
			writeF(y2);
			writeF(z2);
			break;
		default:
			writeC(emotionId);
			writeC(param1); // happinessAdded?
			break;
		}
	}
}
