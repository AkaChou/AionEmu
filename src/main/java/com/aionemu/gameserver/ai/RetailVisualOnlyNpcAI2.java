package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 原版“仅外观”NPC AI：只作为客户端视觉对象存在，不响应对话、不发默认 HTML 页。
 * Retail visual-only NPC AI: exists as a client-side visual object only; it never opens a dialog
 * and never sends a default HTML page.
 * <p>
 * 用于 pattern 刷出的“点燃后外观”替换体（如万圣节蜡烛链的 {@code World_event_halloween_candle_0N_A}）。
 * 原版这类对象只换贴图，必须静默；此前落到 {@code general} 的默认对话流程，客户端会弹“加载失败”。
 * Used for "lit appearance" replacements spawned by patterns (e.g. the Halloween candle chain's
 * {@code World_event_halloween_candle_0N_A}). Retail swaps the mesh silently; without this AI they fell
 * through to the {@code general} default dialog, where the client shows a load failure.
 */
@AIName("retail_visual_only")
public class RetailVisualOnlyNpcAI2 extends GeneralNpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		// 纯外观对象不弹任何窗口。 / A pure visual object never opens a window.
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return false;
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
