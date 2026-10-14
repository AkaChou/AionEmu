# SQL 脚本 | SQL Scripts

[中文](#中文) | [English](#english)

本目录存放**可手动执行**的数据库 SQL；运行时建表不由这里负责——Flyway 会在服务端启动时自动执行。

## 中文

### 存量迁移（有旧库 `al_server_gs` / `al_server_ls`）

**先启动一次游戏服务端**（Flyway 会自动建好 `aion_emu` 的 89 张表）→ **停服** → 运行 `migrate-to-aion-emu.sql` → 再启动游戏。

- 脚本**只搬数据**（逐表清空 → 整表搬运 → 行数校验），不建库、不建表，可反复重跑；库名不同就把脚本里的三个库名全局替换。
- **跑之前先备份**：它会清空目标库的 89 张业务表；旧库不受影响。
- 坑：账本 `flyway_schema_history` 记录 V1 成功后永不重放——业务表被删时，清掉账本再重启（会重放 V1），或对空库手动补跑 `V1__init.sql`。

### 全新部署（没有旧库）

启动游戏即可，Flyway 自动建表。`src/main/resources/db/migration/V1__init.sql` 必须留在 classpath，不要搬进本目录。

---

## English

This directory holds **manually executed** database SQL. Runtime schema creation does not happen here — Flyway runs it at server startup.

### Migrating from the legacy databases (`al_server_gs` / `al_server_ls`)

**Start the game server once** (Flyway builds the 89 `aion_emu` tables) → **stop it** → run `migrate-to-aion-emu.sql` → start the game again.

- Data only (empty each table → copy → row-count check): no database or tables created, re-runnable; replace all three database names in the script if yours differ.
- **Back up first**: it empties the 89 target tables; the legacy databases are not modified.
- Trap: once `flyway_schema_history` records V1 it is never replayed — if the business tables get dropped, clear the ledger and restart (V1 replays), or run `V1__init.sql` manually against an empty database.

### Fresh install (no legacy data)

Just start the game — Flyway builds the schema. `src/main/resources/db/migration/V1__init.sql` must stay on the classpath; don't move it here.
