package com.aionemu.gameserver.model.moltenus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.moltenus.MoltenusTemplate;
import com.aionemu.gameserver.services.moltenusservice.MoltenusFight;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 熔岩魔位置模型。
 * Moltenus Location model.
 *
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class MoltenusLocation {
	/** 位置 ID / Location id */
	protected int id;
	/** 是否激活 / Whether active */
	protected boolean isActive;
	/** 位置模板 / Location template */
	protected MoltenusTemplate template;
	/** 当前进行的熔岩魔战斗 / Active moltenus fight */
	protected MoltenusFight<MoltenusLocation> activeMoltenus;
	/** 位置内的玩家映射 / Players in this location */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 已刷出的实体列表 / Spawned entities */
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	/**
	 * 以模板构造位置。
	 * Constructs a location from a template.
	 *
	 * @param template 位置模板 / location template
	 */
	public MoltenusLocation(MoltenusTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置 active moltenus / Sets the active moltenus */
	public void setActiveMoltenus(MoltenusFight<MoltenusLocation> moltenus) {
		isActive = moltenus != null;
		this.activeMoltenus = moltenus;
	}

	/** 获取模板。 / Returns the template. */
	public final MoltenusTemplate getTemplate() {
		return template;
	}
}
