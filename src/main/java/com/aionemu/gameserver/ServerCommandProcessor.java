package com.aionemu.gameserver;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameAdminPanelShutdownRequest;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 简易 AWT 管理面板：关服、在线查询、踢人、发物、公告与监狱等控制台操作。
 * Lightweight AWT admin panel for shutdown, online lookup, kick, item grant, announce and prison ops.
 *
 * @author PenguinJoe
 * @author yayaya
 */
@Slf4j
public class ServerCommandProcessor {

	/**
	 * 管理面板窗口。
	 * Admin panel frame.
	 */
	Frame f = new Frame("Ya-admin panel 5.8");

	/**
	 * 玩家名输入框。
	 * Player-name input field.
	 */
	final TextField playerNameFieled = new TextField();
	/**
	 * 物品 ID 输入框。
	 * Item-id input field.
	 */
	final TextField itemID = new TextField();
	/**
	 * 公告内容输入框。
	 * Announce-message input field.
	 */
	final TextField messageAnnounce = new TextField();

	/**
	 * 构建并显示管理面板 UI，绑定各按钮动作。
	 * Builds and shows the admin panel UI and binds button actions.
	 */
	public void startAdminPanel() {
		f.setBackground(Color.black);
		// 水平位置/垂直位置/尺寸/上下尺寸 / posHoriz/pos Vert/size/sizeUpDown
		Button shutdown = new Button("Shutdown");
		shutdown.setBounds(20, 40, 60, 20);

		Button online = new Button("Online");
		online.setBounds(80, 40, 60, 20);

		Button who = new Button("Who");
		who.setBounds(140, 40, 60, 20);

		Button add = new Button("AddItem");
		add.setBounds(20, 80, 60, 20);

		Button kick = new Button("Kick");
		kick.setBounds(80, 80, 60, 20);

		Button sPrison = new Button("SPrison");
		sPrison.setBounds(140, 80, 60, 20);

		Button rPrison = new Button("RPrison");
		rPrison.setBounds(200, 80, 60, 20);

		playerNameFieled.setBounds(270, 80, 100, 20);
		playerNameFieled.setText("Player Name");

		itemID.setBounds(380, 80, 100, 20);
		itemID.setText("Iteam ID");

		Button announce = new Button("Announce");
		announce.setBounds(20, 120, 60, 20);

		messageAnnounce.setBounds(90, 120, 230, 20);
		messageAnnounce.setText("Announce message");

		f.add(playerNameFieled);
		f.add(itemID);
		f.add(messageAnnounce);

		f.add(shutdown);
		f.add(online);
		f.add(who);
		f.add(add);
		f.add(kick);
		f.add(announce);
		f.add(sPrison);
		f.add(rPrison);

		shutdown.addActionListener(al_shutdown);
		online.addActionListener(al_online);
		who.addActionListener(al_who);
		add.addActionListener(al_add);
		kick.addActionListener(al_kick);
		announce.addActionListener(al_announce);
		sPrison.addActionListener(al_sendPrison);
		rPrison.addActionListener(al_rescuePrison);

		f.setSize(600, 400);
		f.setLayout(null);
		f.setVisible(true);
		// 关闭窗口 / close frame

		f.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				log.info(I18n.get("log.51b52f400cb6"));
			}
		});
	}

	/**
	 * 关服按钮监听。
	 * Shutdown button listener.
	 */
	ActionListener al_shutdown = new ActionListener() {

	@Override
	public void actionPerformed(ActionEvent e) {
		GameAdminPanelShutdownRequest.shutdown();
	}
};

	/**
	 * 在线人数查询监听。
	 * Online-count query listener.
	 */
	ActionListener al_online = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			int playerCount = DAOManager.getDAO(PlayerDAO.class).getOnlinePlayerCount();
			if (playerCount == 1) {
				log.info(I18n.get("log.b5cec4ab4481", (playerCount)));
			} else {
				log.info(I18n.get("log.4fd6572503cf", (playerCount)));
			}

		}
	};

	/**
	 * 列出在线玩家监听。
	 * List-online-players listener.
	 */
	ActionListener al_who = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			Collection<Player> players = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers();
			if (players.isEmpty()) {
				log.info(I18n.get("log.b09026e40703"));
				return;
			}
			for (Player player : players) {
				log.info(I18n.get("log.28f1440a2d52", player.getName(), player.getCommonData().getRace().name(), player.getAcountName()));
			}
		}
	};

	/**
	 * 给指定玩家发放物品监听。
	 * Grant-item-to-player listener.
	 */
	ActionListener al_add = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int i = Integer.parseInt(itemID.getText());
			int itemId = i;
			long itemCount = 1;

			Player receiver;
			String playerName = playerNameFieled.getText();

			receiver = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerName);

			if (itemID.getText() != null) {
				if (i != 0) {
					if (DataManager.ITEM_DATA.getItemTemplate(itemId) == null) {
						log.info(I18n.get("log.60fb69b4774c", itemId));
						return;
					}
					ItemService.addItem(receiver, itemId, itemCount);
					log.info(I18n.get("log.df6292dc9697", itemId, playerName, itemCount));
				} else {
					log.info(I18n.get("log.50592272cfc3"));
				}
			} else {
				log.info(I18n.get("log.93ffb7a90c17", playerName));
			}

		}
	};

	/**
	 * 踢出玩家（或全部非 GM）监听。
	 * Kick player (or all non-GMs) listener.
	 */
	ActionListener al_kick = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (playerNameFieled.getText() != null && "All".equalsIgnoreCase(playerNameFieled.getText())) {
				for (final Player player : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
					if (!player.isGM()) {
						player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
						log.info(I18n.get("log.9d66bf0d9920", player.getName()));
					}
				}
			} else {
				Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerNameFieled.getText());
				if (player == null) {
					log.info(I18n.get("log.e98bca3b524f"));
					return;
				}
				player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
				log.info(I18n.get("log.c55d9b1d9649", player.getName()));
			}
		}
	};

	/**
	 * 全服公告监听。
	 * Server-wide announce listener.
	 */
	ActionListener al_announce = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (playerNameFieled.getText() != null || messageAnnounce.getText() != "Announce message") {
				Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

				while (iter.hasNext()) {
					PacketSendUtility.sendBrightYellowMessageOnCenter(iter.next(), messageAnnounce.getText());
				}
			}
		}
	};

	/**
	 * 送入监狱监听。
	 * Send-to-prison listener.
	 */
	ActionListener al_sendPrison = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (playerNameFieled.getText() != null) {
				try {
					Player playerToPrison = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerNameFieled.getText());
					int delay = 30;
					String reason = "Ban from Admin";

					if (playerToPrison != null) {
						PunishmentService.setIsInPrison(playerToPrison, true, delay, reason);
						log.info(I18n.get("log.8c0b8cb89af5", playerToPrison.getName(), delay, reason));
					}
				} catch (Exception eo) {

				}
			}
		}
	};

	/**
	 * 从监狱救出监听。
	 * Rescue-from-prison listener.
	 */
	ActionListener al_rescuePrison = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (playerNameFieled.getText() != null) {
				try {
					Player playerFromPrison = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerNameFieled.getText());

					if (playerFromPrison != null) {
						PunishmentService.setIsInPrison(playerFromPrison, false, 0, "");
						log.info(I18n.get("log.dd4fb905c8ca", playerFromPrison.getName()));
					}
				} catch (NoSuchElementException nsee) {
				} catch (Exception ee) {
				}
			}
		}
	};

}
