package com.aionemu.gameserver.model.anoha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.anoha.AnohaTemplate;
import com.aionemu.gameserver.services.anohaservice.BerserkAnoha;

/**
 * 阿诺哈位置模型。
 * Anoha Location model.
 *
 * @author Rinzler (Encom)
 */

public class AnohaLocation {
	protected int id;
	protected boolean isActive;
	protected AnohaTemplate template;
	protected BerserkAnoha<AnohaLocation> activeAnoha;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public AnohaLocation() {
	}

	public AnohaLocation(AnohaTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active anoha / Sets the active anoha */
	public void setActiveAnoha(BerserkAnoha<AnohaLocation> anoha) {
		isActive = anoha != null;
		this.activeAnoha = anoha;
	}

	/** 返回 active anoha / Returns the active anoha */
	public BerserkAnoha<AnohaLocation> getActiveAnoha() {
		return activeAnoha;
	}

	/** 获取模板。 / Returns the template. */
	public final AnohaTemplate getTemplate() {
		return template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回是否已刷新 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}
}
