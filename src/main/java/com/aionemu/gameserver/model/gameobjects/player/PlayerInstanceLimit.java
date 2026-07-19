package com.aionemu.gameserver.model.gameobjects.player;

public final class PlayerInstanceLimit {
	private final int limitKey;
	private long resetAt;
	private int used;
	private int bonusAvailable;
	private int purchasedCount;
	private int purchaseStep;

	public PlayerInstanceLimit(int limitKey, long resetAt, int used, int bonusAvailable, int purchasedCount,
			int purchaseStep) {
		this.limitKey = limitKey;
		this.resetAt = resetAt;
		this.used = used;
		this.bonusAvailable = bonusAvailable;
		this.purchasedCount = purchasedCount;
		this.purchaseStep = purchaseStep;
	}

	public int getLimitKey() {
		return limitKey;
	}

	public long getResetAt() {
		return resetAt;
	}

	public void setResetAt(long resetAt) {
		this.resetAt = resetAt;
	}

	public int getUsed() {
		return used;
	}

	public void setUsed(int used) {
		this.used = used;
	}

	public int getBonusAvailable() {
		return bonusAvailable;
	}

	public void setBonusAvailable(int bonusAvailable) {
		this.bonusAvailable = bonusAvailable;
	}

	public int getPurchasedCount() {
		return purchasedCount;
	}

	public void setPurchasedCount(int purchasedCount) {
		this.purchasedCount = purchasedCount;
	}

	public int getPurchaseStep() {
		return purchaseStep;
	}

	public void setPurchaseStep(int purchaseStep) {
		this.purchaseStep = purchaseStep;
	}
}
