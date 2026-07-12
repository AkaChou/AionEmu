package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Timestamp;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.pet.PetDopingBag;
import com.aionemu.gameserver.model.templates.pet.PetFunctionType;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.toypet.PetAdoptionService;
import com.aionemu.gameserver.services.toypet.PetFeedProgress;
import com.aionemu.gameserver.services.toypet.PetHungryLevel;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

/**
 * 宠物公共数据。
 * Pet Common Data game object.
 */

public class PetCommonData extends VisibleObjectTemplate implements IExpirable {
	private int decoration;
	private String name;
	private final int petId;
	private Timestamp birthday;
	PetFeedProgress feedProgress = null;
	PetDopingBag dopingBag = null;
	private volatile boolean cancelFeed = false;
	private boolean feedingTime = true;
	private long curentTime;
	private final int petObjectId;
	private final int masterObjectId;
	private long startMoodTime;
	private int shuggleCounter;
	private int lastSentPoints;
	private long moodCdStarted;
	private long giftCdStarted;
	private int expireTime;
	private Timestamp despawnTime;
	private boolean isLooting = false;
	private boolean isBuffing = false;
	private boolean isSelling = false;

	public PetCommonData(int petId, int masterObjectId, int expireTime) {
		this.petObjectId = GameWorldBootstrapServices.idFactory().nextId();
		this.petId = petId;
		this.masterObjectId = masterObjectId;
		this.expireTime = expireTime;
		PetTemplate template = DataManager.PET_DATA.getPetTemplate(petId);
		if (template.ContainsFunction(PetFunctionType.FOOD)) {
			int flavourId = template.getPetFunction(PetFunctionType.FOOD).getId();
			int lovedLimit = DataManager.PET_FEED_DATA.getFlavourById(flavourId).getLovedFoodLimit();
			feedProgress = new PetFeedProgress((byte) (lovedLimit & 0xFF));
		}
		if (template.ContainsFunction(PetFunctionType.DOPING)) {
			dopingBag = new PetDopingBag();
		}
	}

	/** 返回 decoration / Returns the decoration */
	public final int getDecoration() {
		return decoration;
	}

	/** 设置 decoration / Sets the decoration */
	public final void setDecoration(int decoration) {
		this.decoration = decoration;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public final String getName() {
		return name;
	}

	/** 设置名称。 / Sets the name. */
	public final void setName(String name) {
		this.name = name;
	}

	/** 返回 pet id / Returns the pet id */
	public final int getPetId() {
		return petId;
	}

	/** 返回 birthday / Returns the birthday */
	public int getBirthday() {
		if (birthday == null) {
			return 0;
		}
		return (int) (birthday.getTime() / 1000);
	}

	/** 返回 birthday timestamp / Returns the birthday timestamp */
	public Timestamp getBirthdayTimestamp() {
		return birthday;
	}

	/** 设置 birthday / Sets the birthday */
	public void setBirthday(Timestamp birthday) {
		this.birthday = birthday;
	}

	/** 返回 curent time / Returns the curent time */
	public long getCurentTime() {
		return curentTime;
	}

	/** 设置 curent time / Sets the curent time */
	public void setCurentTime(long curentTime) {
		this.curentTime = curentTime;
	}

	/** 设置 is feeding time / Sets the is feeding time */
	public void setIsFeedingTime(boolean food) {
		this.feedingTime = food;
	}

	/**
	 * @return Whether feeding time
	 */
	public boolean isFeedingTime() {
		return feedingTime;
	}

	/** 返回 cancel feed / Returns the cancel feed */
	public boolean getCancelFeed() {
		return cancelFeed;
	}

	/** 设置 cancel feed / Sets the cancel feed */
	public void setCancelFeed(boolean cancelFeed) {
		this.cancelFeed = cancelFeed;
	}

	/** 设置 feeding time / Sets the feeding time */
	public void setFeedingTime(boolean feedingTime) {
		this.feedingTime = feedingTime;
	}

	/** 设置 re food time / Sets the re food time */
	public void setReFoodTime(final long reFoodTime) {
		setFeedingTime(false);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				feedingTime = true;
				curentTime = 0;
				feedProgress.setHungryLevel(PetHungryLevel.HUNGRY);
			}
		}, reFoodTime);
	}

	/** 返回时间 / Returns the time*/
	public long getTime() {
		long time = System.currentTimeMillis() - curentTime;
		if (time < 0 || time > 600000) {
			curentTime = 0;
			time = 0;
		}
		return 600000 - time == 600000 ? 0 : 600000 - time;
	}

	/** 返回对象 ID / Returns the object id */
	public int getObjectId() {
		return petObjectId;
	}

	/** 返回 master object id / Returns the master object id */
	public int getMasterObjectId() {
		return masterObjectId;
	}

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return petId;
	}

	/** 返回名称 ID / Returns the name id */
	@Override
	public int getNameId() {
		return 0;
	}

	/** 返回 mood start time / Returns the mood start time */
	public final long getMoodStartTime() {
		return startMoodTime;
	}

	/** 返回 shuggle counter / Returns the shuggle counter */
	public final int getShuggleCounter() {
		return shuggleCounter;
	}

	/** 设置 shuggle counter / Sets the shuggle counter */
	public final void setShuggleCounter(int shuggleCounter) {
		this.shuggleCounter = shuggleCounter;
	}

	/** 返回 mood points / Returns the mood points */
	public final int getMoodPoints(boolean forPacket) {
		if (startMoodTime == 0) {
			startMoodTime = System.currentTimeMillis();
		}
		int points = Math.round((System.currentTimeMillis() - startMoodTime) / 1000f) + shuggleCounter * 1000;
		if (forPacket && points > 9000) {
			return 9000;
		}
		return points;
	}

	/** 返回 last sent points / Returns the last sent points */
	public final int getLastSentPoints() {
		return lastSentPoints;
	}

	/** 设置 last sent points / Sets the last sent points */
	public final void setLastSentPoints(int points) {
		lastSentPoints = points;
	}

	/** Increase shuggle counter / Increase shuggle counter */
	public final boolean increaseShuggleCounter() {
		if (getMoodRemainingTime() > 0) {
			return false;
		}
		this.moodCdStarted = System.currentTimeMillis();
		this.shuggleCounter++;
		return true;
	}

	/** 清除 moodstatistics / Clear mood statistics */
	public final void clearMoodStatistics() {
		this.startMoodTime = 0;
		this.shuggleCounter = 0;
	}

	/** 设置 start mood time / Sets the start mood time */
	public final void setStartMoodTime(long startMoodTime) {
		this.startMoodTime = startMoodTime;
	}

	/** 返回 mood cd started / Returns the mood cd started */
	public long getMoodCdStarted() {
		return moodCdStarted;
	}

	/** 设置 mood cd started / Sets the mood cd started */
	public void setMoodCdStarted(long moodCdStarted) {
		this.moodCdStarted = moodCdStarted;
	}

	/** 返回 mood remaining time / Returns the mood remaining time */
	public int getMoodRemainingTime() {
		long stop = moodCdStarted + 600000;
		long remains = stop - System.currentTimeMillis();
		if (remains <= 0) {
			setMoodCdStarted(0);
			return 0;
		}
		return (int) (remains / 1000);
	}

	/** 返回 gift cd started / Returns the gift cd started */
	public long getGiftCdStarted() {
		return giftCdStarted;
	}

	/** 设置 gift cd started / Sets the gift cd started */
	public void setGiftCdStarted(long giftCdStarted) {
		this.giftCdStarted = giftCdStarted;
	}

	/** 返回 gift remaining time / Returns the gift remaining time */
	public int getGiftRemainingTime() {
		long stop = giftCdStarted + 3600 * 1000;
		long remains = stop - System.currentTimeMillis();
		if (remains <= 0) {
			setGiftCdStarted(0);
			return 0;
		}
		return (int) (remains / 1000);
	}

	/** 返回消失时间 / Returns the despawn time*/
	public Timestamp getDespawnTime() {
		return despawnTime;
	}

	/** 设置 despawn time / Sets the despawn time */
	public void setDespawnTime(Timestamp despawnTime) {
		this.despawnTime = despawnTime;
	}

	/**
	 * Save pet mood data
	 */
	public void savePetMoodData() {
		DAOManager.getDAO(PlayerPetsDAO.class).savePetMoodData(this);
	}

	/** 返回 feed progress / Returns the feed progress */
	public PetFeedProgress getFeedProgress() {
		return feedProgress;
	}

	/** 设置 is looting / Sets the is looting */
	public void setIsLooting(boolean isLooting) {
		this.isLooting = isLooting;
	}

	/**
	 * @return Whether looting
	 */
	public boolean isLooting() {
		return this.isLooting;
	}

	/** 返回 doping bag / Returns the doping bag */
	public PetDopingBag getDopingBag() {
		return dopingBag;
	}

	/** 设置 is buffing / Sets the is buffing */
	public void setIsBuffing(boolean isBuffing) {
		this.isBuffing = isBuffing;
	}

	/**
	 * @return Whether buffing
	 */
	public boolean isBuffing() {
		return this.isBuffing;
	}

	/** 设置 is selling / Sets the is selling */
	public void setIsSelling(boolean isSelling) {
		this.isSelling = isSelling;
	}

	/** 是否出售 / Whether selling */
	public boolean isSelling() {
		return this.isSelling;
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return expireTime;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
		if (player == null) {
			return;
		}
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_PET_ABANDON_EXPIRE_TIME_COMPLETE(name));
		PetAdoptionService.surrenderPet(player, petId);
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int time) {
	}
}
