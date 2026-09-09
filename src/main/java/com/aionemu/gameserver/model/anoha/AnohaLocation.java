package com.aionemu.gameserver.model.anoha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.anoha.AnohaTemplate;
import com.aionemu.gameserver.services.anohaservice.BerserkAnoha;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 阿诺哈位置模型。
 * Anoha Location model.
 *
 * @author Rinzler (Encom)
 */

@NoArgsConstructor
public class AnohaLocation {
	/** 返回 ID / Returns the id */
	@Getter
	protected int id;
	/** 是否激活。 / Whether Active. */
	@Getter
	protected boolean isActive;
	protected AnohaTemplate template;
	/** 返回 active anoha / Returns the active anoha */
	@Getter
	protected BerserkAnoha<AnohaLocation> activeAnoha;
	/** 返回玩家集合 / Returns the players */
	@Getter
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	@Getter
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public AnohaLocation(AnohaTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置 active anoha / Sets the active anoha */
	public void setActiveAnoha(BerserkAnoha<AnohaLocation> anoha) {
		isActive = anoha != null;
		this.activeAnoha = anoha;
	}

	/** 获取模板。 / Returns the template. */
	public final AnohaTemplate getTemplate() {
		return template;
	}
}
