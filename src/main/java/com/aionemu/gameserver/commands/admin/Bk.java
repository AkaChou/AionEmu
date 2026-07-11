package com.aionemu.gameserver.commands.admin;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.database.DB;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.IUStH;
import com.aionemu.commons.database.ParamReadStH;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 书签管理命令（{@code //bk}）：添加、删除、传送与列表。
 * Bookmark admin command ({@code //bk}): add, delete, teleport and list.
 *
 * @author Mrakobes
 * @modified antness
 */
@Slf4j
public class Bk extends AdminCommand {

	ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();
	private String bookmark_name = "";

	/**
	 * 注册命令名为 {@code bk}。
	 * Registers the command name {@code bk}.
	 */
	public Bk() {
		super("bk");
	}

	/**
	 * 执行书签操作：{@code add|del|tele|list}。
	 * Executes bookmark actions: {@code add|del|tele|list}.
	 *
	 * admin
	 * parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax //bk <add|del|tele|list>");
			return;
		}

		if (params[0].equals("add"))
			try {
				bookmark_name = params[1].toLowerCase();
				if (isBookmarkExists(bookmark_name, player.getObjectId())) {
					PacketSendUtility.sendMessage(player, "Bookmark " + bookmark_name + " already exists !");
					return;
				}

				final float x = player.getX();
				final float y = player.getY();
				final float z = player.getZ();
				final int char_id = player.getObjectId();
				final int world_id = player.getWorldId();

				DB.insertUpdate("INSERT INTO bookmark (" + "`name`,`char_id`, `x`, `y`, `z`,`world_id` )" + " VALUES "
					+ "(?, ?, ?, ?, ?, ?)", new IUStH() {

					@Override
					public void handleInsertUpdate(PreparedStatement ps) throws SQLException {
						ps.setString(1, bookmark_name);
						ps.setInt(2, char_id);
						ps.setFloat(3, x);
						ps.setFloat(4, y);
						ps.setFloat(5, z);
						ps.setInt(6, world_id);
						ps.execute();
					}
				});

				PacketSendUtility
					.sendMessage(player, "Bookmark " + bookmark_name + " sucessfully added to your bookmark list!");

				updateInfo(player.getObjectId());
			}
			catch (Exception e) {
				PacketSendUtility.sendMessage(player, "syntax //bk <add|del|tele> <bookmark name>");
				return;
			}
		else if (params[0].equals("del")) {
			Connection con = null;
			try {
				bookmark_name = params[1].toLowerCase();
				con = DatabaseFactory.getConnection();

				PreparedStatement statement = con.prepareStatement("DELETE FROM bookmark WHERE name = ?");
				statement.setString(1, bookmark_name);
				statement.executeUpdate();
				statement.close();
			}
			catch (Exception e) {
				PacketSendUtility.sendMessage(player, "syntax //bk <add|del|tele> <bookmark name>");
				return;
			}
			finally {
				DatabaseFactory.close(con);
				PacketSendUtility.sendMessage(player, "Bookmark " + bookmark_name
					+ " sucessfully removed from your bookmark list!");
				updateInfo(player.getObjectId());
			}
		}
		else if (params[0].equals("tele"))
			try {

				if (params[1].equals("") || params[1] == null) {
					PacketSendUtility.sendMessage(player, "syntax //bk <add|del|tele> <bookmark name>");
					return;
				}

				updateInfo(player.getObjectId());

				bookmark_name = params[1].toLowerCase();
				Bookmark tele_bk = null;
				try {
					tele_bk = selectByName(bookmark_name);
				}
				finally {
					if (tele_bk != null) {
						TeleportService2.teleportTo(player, tele_bk.getWorld_id(), tele_bk.getX(), tele_bk.getY(), tele_bk.getZ());
						PacketSendUtility.sendMessage(player, "Teleported to bookmark " + tele_bk.getName() + " location");
					}
				}
			}
			catch (Exception e) {
				PacketSendUtility.sendMessage(player, "syntax //bk <add|del|tele> <bookmark name>");
				return;
			}
		else if (params[0].equals("list")) {
			updateInfo(player.getObjectId());
			PacketSendUtility.sendMessage(player, "=====Bookmark list begin=====");
			for (Bookmark b : bookmarks) {
				String chatLink = ChatUtil.position(b.getName(), b.getWorld_id(), b.getX(), b.getY(), b.getZ());
				PacketSendUtility.sendMessage(player, " = " + chatLink + " =  " + WorldMapType.getWorld(b.getWorld_id())
					+ "  ( " + b.getX() + " ," + b.getY() + " ," + b.getZ() + " )");
			}
			PacketSendUtility.sendMessage(player, "=====Bookmark list end=======");
		}
	}

	/**
	 * 从数据库重新加载书签列表。
	 * Reloads the bookmark list from the database.
	 *
	 * @param objId 角色对象 ID / character object id
	 */
	public void updateInfo(final int objId) {
		bookmarks.clear();

		DB.select("SELECT * FROM `bookmark` where char_id= ?", new ParamReadStH() {

			@Override
			public void setParams(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, objId);
			}

			@Override
			public void handleRead(ResultSet rset) throws SQLException {
				while (rset.next()) {
					String name = rset.getString("name");
					float x = rset.getFloat("x");
					float y = rset.getFloat("y");
					float z = rset.getFloat("z");
					int world_id = rset.getInt("world_id");
					bookmarks.add(new Bookmark(x, y, z, world_id, name));
				}
			}
		});
	}

	/**
	 * 按名称查找书签。
	 * Selects a bookmark by name.
	 *
	 * @param bk_name 书签名称 / bookmark name
	 * @return 匹配的书签，不存在则为 null / matching bookmark, or null
	 */
	public Bookmark selectByName(String bk_name) {
		for (Bookmark b : bookmarks)
			if (b.getName().equals(bk_name))
				return b;
		return null;
	}

	/**
	 * 判断指定角色是否已存在同名书签。
	 * Checks whether a bookmark name already exists for the character.
	 *
	 * @param bk_name 书签名称 / bookmark name
	 * @param objId 角色对象 ID / character object id
	 * @return 已存在则为 true / true if exists
	 */
	public boolean isBookmarkExists(final String bk_name, final int objId) {
		Connection con = null;
		int bkcount = 0;
		try {
			con = DatabaseFactory.getConnection();
			PreparedStatement statement = con
				.prepareStatement("SELECT count(id) as bkcount FROM bookmark WHERE ? = name AND char_id = ?");
			statement.setString(1, bk_name);
			statement.setInt(2, objId);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
				bkcount = rset.getInt("bkcount");
			rset.close();
			statement.close();
		}
		catch (Exception e) {
			log.error(I18n.get("log.0de52ee75b14", e));
		}
		finally {
			DatabaseFactory.close(con);
		}
		return bkcount > 0;
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 * admin
	 * error message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //bk <add|del|tele|list>");
	}
}

/**
 * 书签坐标数据。
 * Bookmark coordinate data.
 */
class Bookmark {

	private String name;
	private float x;
	private float y;
	private float z;
	private int world_id;

	/**
	 * 构造书签。
	 * Constructs a bookmark.
	 *
	 * @param x X 坐标 / X coordinate
	 * @param y Y 坐标 / Y coordinate
	 * @param z Z 坐标 / Z coordinate
	 * @param world_id 世界地图 ID / world map id
	 * @param name 书签名称 / bookmark name
	 */
	public Bookmark(float x, float y, float z, int world_id, String name) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.world_id = world_id;
		this.name = name;
	}

	/**
	 * 获取书签名称。
	 * Gets the bookmark name.
	 *
	 * name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取 X 坐标。
	 * Gets the X coordinate.
	 *
	 * X 坐标 / X coordinate
	 */
	public float getX() {
		return x;
	}

	/**
	 * 获取 Y 坐标。
	 * Gets the Y coordinate.
	 *
	 * Y 坐标 / Y coordinate
	 */
	public float getY() {
		return y;
	}

	/**
	 * 获取 Z 坐标。
	 * Gets the Z coordinate.
	 *
	 * Z 坐标 / Z coordinate
	 */
	public float getZ() {
		return z;
	}

	/**
	 * 获取世界地图 ID。
	 * Gets the world map id.
	 *
	 * 世界 ID / world id
	 */
	public int getWorld_id() {
		return world_id;
	}
}
