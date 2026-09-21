package com.aionemu.gameserver.model.idiandepths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.idiandepths.IdianDepthsTemplate;
import com.aionemu.gameserver.services.idiandepthsservice.IdianDepths;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 伊迪安深渊位置，用于 idiandepths 相关逻辑。
 * Idian Depths Location for idiandepths logic.
 *
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class IdianDepthsLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected IdianDepthsTemplate template;
	/** 返回当前伊迪安深渊 / Returns the active idian depths */
	protected IdianDepths<IdianDepthsLocation> activeIdianDepths;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public IdianDepthsLocation(IdianDepthsTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置当前的伊迪安深渊 / Sets the active idian depths */
	public void setActiveIdianDepths(IdianDepths<IdianDepthsLocation> idianDepths) {
		isActive = idianDepths != null;
		this.activeIdianDepths = idianDepths;
	}

	/** 获取模板。 / Returns the template. */
	public final IdianDepthsTemplate getTemplate() {
		return template;
	}
}
