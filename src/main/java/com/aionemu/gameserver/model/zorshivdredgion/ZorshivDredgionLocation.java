package com.aionemu.gameserver.model.zorshivdredgion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zorshivdredgion.ZorshivDredgionTemplate;
import com.aionemu.gameserver.services.zorshivdredgionservice.ZorshivDredgion;

/**
 * 佐希夫无畏舰位置，用于 zorshivdredgion 相关逻辑。
 * Zorshiv Dredgion Location for zorshivdredgion logic.
 *
 * @author Rinzler (Encom)
 */

public class ZorshivDredgionLocation {
	protected int id;
	protected boolean isActive;
	protected ZorshivDredgionTemplate template;
	protected ZorshivDredgion<ZorshivDredgionLocation> activeZorshivDredgion;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public ZorshivDredgionLocation() {
	}

	public ZorshivDredgionLocation(ZorshivDredgionTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置激活的无畏舰 / Sets the active zorshiv dredgion */
	public void setActiveZorshivDredgion(ZorshivDredgion<ZorshivDredgionLocation> zorshivDredgion) {
		isActive = zorshivDredgion != null;
		this.activeZorshivDredgion = zorshivDredgion;
	}

	/** 返回当前佐希夫无畏舰 / Returns the active zorshiv dredgion */
	public ZorshivDredgion<ZorshivDredgionLocation> getActiveZorshivDredgion() {
		return activeZorshivDredgion;
	}

	/** 获取模板。 / Returns the template. */
	public final ZorshivDredgionTemplate getTemplate() {
		return template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return template.getName();
	}

	/** 返回已生成对象列表 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}
}
