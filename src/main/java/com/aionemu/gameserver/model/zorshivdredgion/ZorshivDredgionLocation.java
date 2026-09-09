package com.aionemu.gameserver.model.zorshivdredgion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zorshivdredgion.ZorshivDredgionTemplate;
import com.aionemu.gameserver.services.zorshivdredgionservice.ZorshivDredgion;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 佐希夫无畏舰位置，用于 zorshivdredgion 相关逻辑。
 * Zorshiv Dredgion Location for zorshivdredgion logic.
 *
 * @author Rinzler (Encom)
 */

@NoArgsConstructor
public class ZorshivDredgionLocation {
	/** 返回 ID / Returns the id */
	@Getter
	protected int id;
	/** 是否激活。 / Whether Active. */
	@Getter
	protected boolean isActive;
	protected ZorshivDredgionTemplate template;
	/** 返回当前佐希夫无畏舰 / Returns the active zorshiv dredgion */
	@Getter
	protected ZorshivDredgion<ZorshivDredgionLocation> activeZorshivDredgion;
	/** 返回玩家集合 / Returns the players */
	@Getter
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回已生成对象列表 / Returns the spawned */
	@Getter
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public ZorshivDredgionLocation(ZorshivDredgionTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置激活的无畏舰 / Sets the active zorshiv dredgion */
	public void setActiveZorshivDredgion(ZorshivDredgion<ZorshivDredgionLocation> zorshivDredgion) {
		isActive = zorshivDredgion != null;
		this.activeZorshivDredgion = zorshivDredgion;
	}

	/** 获取模板。 / Returns the template. */
	public final ZorshivDredgionTemplate getTemplate() {
		return template;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return template.getName();
	}
}
