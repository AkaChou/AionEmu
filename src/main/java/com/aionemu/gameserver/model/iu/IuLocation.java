package com.aionemu.gameserver.model.iu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.iu.IuTemplate;
import com.aionemu.gameserver.services.iuservice.Iu;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * IU 活动位置模型。
 * Iu Location model.
 *
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class IuLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected IuTemplate template;
	/** 返回激活的 iu / Returns the active iu */
	protected Iu<IuLocation> activeIu;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回已生成对象列表 / Returns the spawned */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public IuLocation(IuTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置激活的 iu / Sets the active iu */
	public void setActiveIu(Iu<IuLocation> iu) {
		isActive = iu != null;
		this.activeIu = iu;
	}

	/** 获取模板。 / Returns the template. */
	public final IuTemplate getTemplate() {
		return template;
	}
}
