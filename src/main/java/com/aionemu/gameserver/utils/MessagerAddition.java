package com.aionemu.gameserver.utils;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 消息发送辅助类：面向玩家/全服的多种聊天样式快捷封装。
 * Messaging helper: convenience wrappers for player and server-wide chat styles.
 *
 * @author Rinzler (Encom)
 */
public class MessagerAddition {

	/**
	 * 占位保护方法（无逻辑）。
	 * Placeholder guard method (no-op).
	 */
	protected void DEEPINSIDE() {
	}

	/**
	 * 向玩家发送居中亮黄公告。
	 * Send a bright-yellow center announcement to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void announce(Player player, String msg) {
		PacketSendUtility.sendBrightYellowMessageOnCenter(player, msg);
	}

	/**
	 * 向玩家发送普通金色消息。
	 * Send a normal golden message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void message(Player player, String msg) {
		PacketSendUtility.sendMessage(player, msg);
	}

	/**
	 * 向玩家发送白色消息。
	 * Send a white message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void whiteMsg(Player player, String msg) {
		PacketSendUtility.sendWhiteMessage(player, msg);
	}

	/**
	 * 向玩家发送居中白色消息。
	 * Send a white center message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void whiteMsgOnCtr(Player player, String msg) {
		PacketSendUtility.sendWhiteMessageOnCenter(player, msg);
	}

	/**
	 * 向玩家发送黄色消息。
	 * Send a yellow message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void yellowMsg(Player player, String msg) {
		PacketSendUtility.sendYellowMessage(player, msg);
	}

	/**
	 * 向玩家发送居中黄色消息。
	 * Send a yellow center message to a player.
	 *
	 * Target player
	 * @param msg 消息内容 / Message text
	 */
	public static void yellowMsgOnCtr(Player player, String msg) {
		PacketSendUtility.sendYellowMessageOnCenter(player, msg);
	}

	/**
	 * 向全服玩家发送居中亮黄公告（可延迟）。
	 * Broadcast a bright-yellow center announcement to all players (optional delay).
	 *
	 * @param msg 消息内容 / Message text
	 * @param delay 延迟毫秒，0 表示立即 / Delay in ms; 0 means immediate
	 */
	public static void announceAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player sender) {
							PacketSendUtility.sendBrightYellowMessageOnCenter(sender, msg);
							return;
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player sender) {
					PacketSendUtility.sendBrightYellowMessageOnCenter(sender, msg);
					return;
				}
			});
		}
	}

	/**
	 * 向全服玩家发送普通消息（可延迟）。
	 * Broadcast a normal message to all players (optional delay).
	 *
	 * @param msg 消息内容 / Message text
	 * @param delay 延迟毫秒，0 表示立即 / Delay in ms; 0 means immediate
	 */
	public static void messageToAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player sender) {
							PacketSendUtility.sendMessage(sender, msg);
							return;
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player sender) {
					PacketSendUtility.sendMessage(sender, msg);
					return;
				}
			});
		}
	}

	/**
	 * 向全服玩家发送白色消息（可延迟）。
	 * Broadcast a white message to all players (optional delay).
	 *
	 * @param msg 消息内容 / Message text
	 * @param delay 延迟毫秒，0 表示立即 / Delay in ms; 0 means immediate
	 */
	public static void whiteMsgToAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player sender) {
							PacketSendUtility.sendWhiteMessage(sender, msg);
							return;
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player sender) {
					PacketSendUtility.sendWhiteMessage(sender, msg);
					return;
				}
			});
		}
	}

	/**
	 * 向全服玩家发送居中白色公告（可延迟）。
	 * Broadcast a white center announcement to all players (optional delay).
	 *
	 * @param msg 消息内容 / Message text
	 * @param delay 延迟毫秒，0 表示立即 / Delay in ms; 0 means immediate
	 */
	public static void whiteAnnounceToAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player sender) {
							PacketSendUtility.sendWhiteMessageOnCenter(sender, msg);
							return;
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player sender) {
					PacketSendUtility.sendWhiteMessageOnCenter(sender, msg);
					return;
				}
			});
		}
	}

	/**
	 * 向全服玩家发送黄色消息（可延迟）。
	 * Broadcast a yellow message to all players (optional delay).
	 *
	 * @param msg 消息内容 / Message text
	 * @param delay 延迟毫秒，0 表示立即 / Delay in ms; 0 means immediate
	 */
	public static void yellowMsgToAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player sender) {
							PacketSendUtility.sendYellowMessage(sender, msg);
							return;
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player sender) {
					PacketSendUtility.sendYellowMessage(sender, msg);
					return;
				}
			});
		}
	}

	/**
	 * 向全服玩家发送居中黄色公告（可延迟）。
	 * Broadcast a yellow center announcement to all players (optional delay).
	 *
	 * @param msg 消息内容 / Message text
	 * @param delay 延迟毫秒，0 表示立即 / Delay in ms; 0 means immediate
	 */
	public static void yellowAnnounceToAll(final String msg, int delay) {
		if (delay > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player sender) {
							PacketSendUtility.sendYellowMessageOnCenter(sender, msg);
							return;
						}
					});
				}
			}, delay);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player sender) {
					PacketSendUtility.sendYellowMessageOnCenter(sender, msg);
					return;
				}
			});
		}
	}

	/**
	 * 全服全局广播（前缀 {@code [Global]:}）。
	 * Server-wide global broadcast with {@code [Global]:} prefix.
	 *
	 * @param msg 消息内容 / Message text
	 */
	public static void global(final String msg) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player sender) {
				PacketSendUtility.sendBrightYellowMessageOnCenter(sender, "[Global]:" + msg);
				return;
			}
		});
	}

	/**
	 * 全服注意广播（前缀 {@code [Attention]:}）。
	 * Server-wide attention broadcast with {@code [Attention]:} prefix.
	 *
	 * @param msg 消息内容 / Message text
	 */
	public static void attention(final String msg) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player sender) {
				PacketSendUtility.sendBrightYellowMessageOnCenter(sender, "[Attention]:" + msg);
				return;
			}
		});
	}
}
