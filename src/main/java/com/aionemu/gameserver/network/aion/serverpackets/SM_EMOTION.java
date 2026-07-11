package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 生物情绪/动作状态包：跳跃、飞行、攻击模式、表情等，按 EmotionType 写入附加字段。
 * Creature emotion/action packet (jump, fly, attack mode, emote, …) with type-specific fields.
 */
public class SM_EMOTION extends AionServerPacket {
	private int senderObjectId;
	private EmotionType emotionType;
	private int emotion;
	private int targetObjectId;
	private float speed;
	private int state;
	private int baseAttackSpeed;
	private int currentAttackSpeed;
	private float x;
	private float y;
	private float z;
	private byte heading;

	/**
	 * 仅情绪类型，无附加目标。
	 * Emotion type only, no extra target.
	 */
	public SM_EMOTION(Creature creature, EmotionType emotionType) {
		this(creature, emotionType, 0, 0);
	}

	/**
	 * 标准生物情绪包，附带目标与当前攻速/移速。
	 * Standard creature emotion with target and current attack/move speed.
	 */
	public SM_EMOTION(Creature creature, EmotionType emotionType, int emotion, int targetObjectId) {
		this.senderObjectId = creature.getObjectId();
		this.emotionType = emotionType;
		this.emotion = emotion;
		this.targetObjectId = targetObjectId;
		this.state = creature.getState();
		Stat2 aSpeed = creature.getGameStats().getAttackSpeed();
		this.baseAttackSpeed = aSpeed.getBase();
		this.currentAttackSpeed = aSpeed.getCurrent();
		this.speed = creature.getGameStats().getMovementSpeedFloat();
	}

	/**
	 * 按对象 ID 与状态写入的简化情绪包。
	 * Simplified emotion by object id and state.
	 */
	public SM_EMOTION(int Objid, EmotionType emotionType, int state) {
		this.senderObjectId = Objid;
		this.emotionType = emotionType;
		this.state = state;
	}

	/**
	 * 玩家带坐标的情绪（如坐下椅子）。
	 * Player emotion with coordinates (e.g. chair sit).
	 */
	public SM_EMOTION(Player player, EmotionType emotionType, int emotion, float x, float y, float z, byte heading,
			int targetObjectId) {
		this.senderObjectId = player.getObjectId();
		this.emotionType = emotionType;
		this.emotion = emotion;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.targetObjectId = targetObjectId;
		this.state = player.getState();
		this.speed = player.getGameStats().getMovementSpeedFloat();
		Stat2 aSpeed = player.getGameStats().getAttackSpeed();
		this.baseAttackSpeed = aSpeed.getBase();
		this.currentAttackSpeed = aSpeed.getCurrent();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(senderObjectId);
		writeC(emotionType.getTypeId());
		writeH(state);
		writeF(speed);
		switch (emotionType) {
		case SELECT_TARGET:
		case JUMP:
		case SIT:
		case STAND:
		case LAND_FLYTELEPORT:
		case WINDSTREAM_START_BOOST:
		case WINDSTREAM_END_BOOST:
		case FLY:
		case LAND:
		case ATTACKMODE:
		case NEUTRALMODE:
		case WALK:
		case RUN:
		case OPEN_PRIVATESHOP:
		case CLOSE_PRIVATESHOP:
		case POWERSHARD_ON:
		case POWERSHARD_OFF:
		case ATTACKMODE2:
		case NEUTRALMODE2:
		case START_FEEDING:
		case END_FEEDING:
		case END_SPRINT:
		case WINDSTREAM_END:
		case WINDSTREAM_EXIT:
		case WINDSTREAM_STRAFE:
		case END_DUEL:
		case PET_SNUGGLE:
		case PET_EMOTION_2:
		case PET_EMOTION_3:
		case PET_EMOTION_4:
		case GLIDING:
		case GLIDING_END:
			break;
		case DIE:
		case START_LOOT:
		case END_LOOT:
		case END_QUESTLOOT:
		case OPEN_DOOR:
			writeD(targetObjectId);
			break;
		case CHAIR_SIT:
		case CHAIR_UP:
			writeF(x);
			writeF(y);
			writeF(z);
			writeC(heading);
			break;
		case START_FLYTELEPORT:
			writeD(emotion);
			break;
		case WINDSTREAM:
			writeD(emotion);
			writeD(targetObjectId);
			break;
		case RIDE:
		case RIDE_END:
			if (targetObjectId != 0) {
				writeD(targetObjectId);
			}
			writeH(0);
			writeC(0);
			writeD(0x3F);
			writeD(0x3F);
			writeC(0x40);
			break;
		case START_SPRINT:
			writeD(0);
			break;
		case RESURRECT:
			writeD(0);
			break;
		case EMOTE:
			writeD(targetObjectId);
			writeH(emotion);
			writeC(1);
			break;
		case START_EMOTE2:
			writeH(baseAttackSpeed);
			writeH(currentAttackSpeed);
			writeC(0);
			break;
		default:
			if (targetObjectId != 0) {
				writeD(targetObjectId);
			}
		}
	}
}
