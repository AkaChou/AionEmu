package com.aionemu.gameserver.model.towerofeternity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.towerofeternity.TowerOfEternityTemplate;
import com.aionemu.gameserver.services.towerofeternityservice.TowerOfEternity;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 永恒之塔位置，用于 towerofeternity 相关逻辑。
 * Tower Of Eternity Location for towerofeternity logic.
 */

@Getter
@NoArgsConstructor
public class TowerOfEternityLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected TowerOfEternityTemplate template;
	/** 返回当前永恒之塔 / Returns the active tower of eternity */
	protected TowerOfEternity<TowerOfEternityLocation> activeTowerOfEternity;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public TowerOfEternityLocation(TowerOfEternityTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置当前的永恒之塔 / Sets the active tower of eternity */
	public void setActiveTowerOfEternity(TowerOfEternity<TowerOfEternityLocation> towerOfEternity) {
		isActive = towerOfEternity != null;
		this.activeTowerOfEternity = towerOfEternity;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return template.getWorldId();
	}

	/** 获取模板。 / Returns the template. */
	public final TowerOfEternityTemplate getTemplate() {
		return template;
	}
}
