-- AionEmu web shop schema (database: aion_emu)
-- 自建网页商城的业务表。发货复用既有 web_reward 表（见 V1__init.sql），本文件不重复定义。
-- Business tables of the self-hosted web shop. Delivery reuses the existing web_reward table
-- (defined in V1__init.sql) and is therefore not redefined here.
--
-- 物品目录（shop_item_catalog）的 12.8 万行由 scripts/sql/import-shop-catalog.py 从
-- aion-shop 的静态数据一次性导入，刻意不写进本迁移：那会产出 20MB+ 的 SQL。
-- The 128k rows of the item catalog (shop_item_catalog) are imported once by
-- scripts/sql/import-shop-catalog.py from the aion-shop static data, deliberately not inlined here:
-- it would produce 20MB+ of SQL.

SET NAMES utf8mb4;

-- shop_category —— 商城分类树（由目录 category_path 聚合而来，供侧栏与筛选使用）。
-- shop_category -- shop category tree, aggregated from the catalog's category_path, used by the sidebar.
CREATE TABLE `shop_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `path` varchar(191) NOT NULL,
  `parent_path` varchar(191) NOT NULL DEFAULT '',
  `depth` tinyint NOT NULL DEFAULT '1',
  `item_count` int NOT NULL DEFAULT '0',
  `sort` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_category_path` (`path`),
  KEY `idx_shop_category_parent` (`parent_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- shop_item_catalog —— 物品目录（aion-shop 的 Items_zh 静态数据全量迁入）。
-- shop_item_catalog -- item catalog (full migration of the aion-shop Items_zh static data).
CREATE TABLE `shop_item_catalog` (
  `item_id` int NOT NULL,
  `name` varchar(128) NOT NULL DEFAULT '',
  `name_zh` varchar(191) NOT NULL DEFAULT '',
  `item_level` int NOT NULL DEFAULT '0',
  `category1` varchar(64) NOT NULL DEFAULT '',
  `category2` varchar(64) NOT NULL DEFAULT '',
  `category3` varchar(64) NOT NULL DEFAULT '',
  `category_path` varchar(191) NOT NULL DEFAULT '',
  `icon_file` varchar(191) NOT NULL DEFAULT '',
  `gender_limit` varchar(16) NOT NULL DEFAULT '',
  `race_limit` varchar(16) NOT NULL DEFAULT '',
  `class_limit` varchar(64) NOT NULL DEFAULT '',
  PRIMARY KEY (`item_id`),
  KEY `idx_shop_item_category_path` (`category_path`),
  KEY `idx_shop_item_level` (`item_level`),
  KEY `idx_shop_item_name_zh` (`name_zh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- shop_product —— 上架商品。item_id 唯一，指向 shop_item_catalog。
-- shop_product -- listed product. item_id is unique and references shop_item_catalog.
CREATE TABLE `shop_product` (
  `id` int NOT NULL AUTO_INCREMENT,
  `item_id` int NOT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `purchase_limit` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `sort` int NOT NULL DEFAULT '0',
  `description` varchar(512) NOT NULL DEFAULT '',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_product_item` (`item_id`),
  KEY `idx_shop_product_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- shop_order —— 订单头。shop_order_item 与之 1..N。
-- shop_order -- order header; shop_order_item hangs off it 1..N.
CREATE TABLE `shop_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account_id` int NOT NULL,
  `account_name` varchar(45) NOT NULL,
  `player_id` int NOT NULL,
  `player_name` varchar(50) NOT NULL,
  `total_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop_order_account` (`account_id`),
  KEY `idx_shop_order_player` (`player_id`),
  KEY `idx_shop_order_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- shop_order_item —— 订单行。web_reward 的行与之同事务写入。
-- shop_order_item -- order line. The web_reward row is written in the same transaction.
CREATE TABLE `shop_order_item` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `product_id` int NOT NULL,
  `item_id` int NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  KEY `idx_shop_order_item_order` (`order_id`),
  CONSTRAINT `shop_order_item_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `shop_order` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
