package com.aionemu.gameserver.model.gameobjects.player.emotion;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dao.PlayerEmotionListDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION_LIST;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 表情列表。
 * Emotion List game object.
 *
 * @author MrPoke
 */
public class EmotionList {
	private Map<Integer, Emotion> emotions;
	private Player owner;

	/**
	 * 构造表情列表。
	 * Constructs an emotion list.
	 *
	 * @param owner 所属玩家 / owning player
	 */
	public EmotionList(Player owner) {
		this.owner = owner;
	}

	/** 添加。 / Add. */
	public void add(int emotionId, int dispearTime, boolean isNew) {
		if (emotions == null) {
			emotions = new HashMap<Integer, Emotion>();
		}
		Emotion emotion = new Emotion(emotionId, dispearTime);
		emotions.put(emotionId, emotion);

		if (isNew) {
			if (emotion.getExpireTime() != 0) {
				GameTaskManagerServices.expireTimerTask().addTask(emotion, owner);
			}
			DAOManager.getDAO(PlayerEmotionListDAO.class).insertEmotion(owner, emotion);
			PacketSendUtility.sendPacket(owner, new SM_EMOTION_LIST((byte) 1, Collections.singletonList(emotion)));
		}
	}

	/** 移除。 / Remove. */
	public void remove(int emotionId) {
		emotions.remove(emotionId);
		DAOManager.getDAO(PlayerEmotionListDAO.class).deleteEmotion(owner.getObjectId(), emotionId);
		PacketSendUtility.sendPacket(owner, new SM_EMOTION_LIST((byte) 0, getEmotions()));
	}

	/** 是否包含。 / Contains. */
	public boolean contains(int emotionId) {
		if (emotions == null) {
			return false;
		}
		return emotions.containsKey(emotionId);
	}

	/**
	 * 判断表情是否可使用。
	 * Returns whether the emotion can be used.
	 *
	 * @param emotionId 表情 ID / emotion id
	 * @return 是否可使用 / whether use
	 */
	public boolean canUse(int emotionId) {
		return emotionId < 64 || emotionId > 155 || (emotions != null && emotions.containsKey(emotionId))
				|| owner.havePermission(MembershipConfig.EMOTIONS_ALL);
	}

	/** 返回 emotions / Returns the emotions */
	public Collection<Emotion> getEmotions() {
		if (emotions == null) {
			return Collections.emptyList();
		}
		return emotions.values();
	}
}
