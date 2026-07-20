DROP TABLE IF EXISTS `al_server_gs`.`player_luna_shop`;
CREATE TABLE `al_server_gs`.`player_luna_shop` (
	`player_id` int(10) NOT NULL,
	`free_chest` tinyint(1) NOT NULL,
	PRIMARY KEY (`player_id`),
	CONSTRAINT `player_luna_shop_player_fk` FOREIGN KEY (`player_id`) REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `al_server_gs`.`player_instance_limits` (
  `player_id` int(11) NOT NULL,
  `limit_key` int(11) NOT NULL,
  `reset_at` bigint(20) NOT NULL DEFAULT 0,
  `used` int(11) NOT NULL DEFAULT 0,
  `bonus_available` int(11) NOT NULL DEFAULT 0,
  `purchased_count` int(11) NOT NULL DEFAULT 0,
  `purchase_step` int(11) NOT NULL DEFAULT 0,
  `updated_at` bigint(20) NOT NULL,
  PRIMARY KEY (`player_id`,`limit_key`),
  CONSTRAINT `player_instance_limits_player_fk` FOREIGN KEY (`player_id`) REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `al_server_gs`.`dynamic_instances` (
  `instance_uid` bigint(20) NOT NULL AUTO_INCREMENT,
  `world_id` int(11) NOT NULL,
  `creation_id` int(11) NOT NULL,
  `client_instance_id` int(11) NOT NULL DEFAULT 0,
  `runtime_instance_id` int(11) NOT NULL,
  `owner_type` tinyint(3) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `difficulty` tinyint(3) NOT NULL DEFAULT 0,
  `status` tinyint(3) NOT NULL,
  `spawn_page` tinyint(3) NOT NULL DEFAULT 0,
  `created_at` bigint(20) NOT NULL,
  `active_until` bigint(20) NOT NULL DEFAULT 0,
  `empty_until` bigint(20) NOT NULL DEFAULT 0,
  `destroy_at` bigint(20) NOT NULL DEFAULT 0,
  `state_version` int(11) NOT NULL DEFAULT 1,
  `state_json` json NOT NULL,
  `updated_at` bigint(20) NOT NULL,
  PRIMARY KEY (`instance_uid`),
  UNIQUE KEY `dynamic_instances_runtime` (`world_id`,`runtime_instance_id`),
  KEY `dynamic_instances_status` (`status`,`destroy_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `al_server_gs`.`dynamic_instance_members` (
  `instance_uid` bigint(20) NOT NULL,
  `player_id` int(11) NOT NULL,
  `team_id_at_entry` int(11) NOT NULL DEFAULT 0,
  `side` tinyint(3) NOT NULL DEFAULT 0,
  `permitted` tinyint(1) NOT NULL DEFAULT 1,
  `joined_at` bigint(20) NOT NULL,
  `left_at` bigint(20) NOT NULL DEFAULT 0,
  `reentry_until` bigint(20) NOT NULL DEFAULT 0,
  `exit_world_id` int(11) NOT NULL DEFAULT 0,
  `exit_alias` varchar(128) NOT NULL DEFAULT '',
  `entry_limit_key` int(11) NOT NULL DEFAULT 0,
  `entry_consumed` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`instance_uid`,`player_id`),
  KEY `dynamic_instance_members_player` (`player_id`,`reentry_until`),
  CONSTRAINT `dynamic_instance_members_instance_fk` FOREIGN KEY (`instance_uid`) REFERENCES `al_server_gs`.`dynamic_instances` (`instance_uid`) ON DELETE CASCADE,
  CONSTRAINT `dynamic_instance_members_player_fk` FOREIGN KEY (`player_id`) REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE IF NOT EXISTS `al_server_gs`.`instance_reward_ledger` (
  `instance_uid` bigint(20) NOT NULL,
  `player_id` int(11) NOT NULL,
  `reward_key` varchar(128) NOT NULL,
  `status` tinyint(3) NOT NULL,
  `payload_hash` char(64) NOT NULL,
  `payload_json` json NOT NULL,
  `created_at` bigint(20) NOT NULL,
  `completed_at` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`instance_uid`,`player_id`,`reward_key`),
  CONSTRAINT `instance_reward_ledger_instance_fk` FOREIGN KEY (`instance_uid`) REFERENCES `al_server_gs`.`dynamic_instances` (`instance_uid`) ON DELETE CASCADE,
  CONSTRAINT `instance_reward_ledger_player_fk` FOREIGN KEY (`player_id`) REFERENCES `al_server_gs`.`players` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
