package com.aionemu.gameserver.dao.mysql8;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

/**
 * @author lord_rex
 * Updated for MySQL 8 - Fixed connection leaks
 */
@Slf4j
public class MySQL8PlayerRecipesDAO extends PlayerRecipesDAO {

	
	private static final String SELECT_QUERY = "SELECT `recipe_id` FROM player_recipes WHERE `player_id`=?";
	private static final String ADD_QUERY = "INSERT INTO player_recipes (`player_id`, `recipe_id`) VALUES (?, ?)";
	private static final String DELETE_QUERY = "DELETE FROM player_recipes WHERE `player_id`=? AND `recipe_id`=?";

	@Override
	public RecipeList load(final int playerId) {
		final HashSet<Integer> recipeList = new HashSet<Integer>();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {
			
			ps.setInt(1, playerId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					recipeList.add(rs.getInt("recipe_id"));
				}
			}
		} catch (SQLException e) {
			log.error("Error loading recipes for player: " + playerId, e);
		}
		return new RecipeList(recipeList);
	}

	@Override
	public boolean addRecipe(final int playerId, final int recipeId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(ADD_QUERY)) {
			
			ps.setInt(1, playerId);
			ps.setInt(2, recipeId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error adding recipe for player: " + playerId + ", recipe: " + recipeId, e);
			return false;
		}
	}

	@Override
	public boolean delRecipe(final int playerId, final int recipeId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
			
			ps.setInt(1, playerId);
			ps.setInt(2, recipeId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error deleting recipe for player: " + playerId + ", recipe: " + recipeId, e);
			return false;
		}
	}

	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}