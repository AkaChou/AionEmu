# AGENTS.md

本文件为 AI coding agent 提供仓库级入口说明。

> **详细规则拆分在 `.agent/rules/` 下。** 执行任务前根据下方索引读取适用规则。

## 全局规则

1. 默认使用中文沟通和交付说明。
2. 保留工作区已有改动，只修改当前任务需要的文件。
3. 所有 shell 命令使用 `rtk` 前缀，Maven/Javac 串行执行。
4. 未经用户明确要求，不提交、不推送、不创建 PR。

## 项目概览

AionEmu 是 Aion 5.8 社区服务端。项目使用 JDK 25 和单 Maven 工程，由 Spring Boot 启动 login、game 和 chat 服务。

## 核心目录

- `src/main/java/`：服务端 Java 代码。
- `src/main/resources/aion/`：静态数据、任务 XML 和运行配置。
- `src/test/java/`：单元测试、生产 catalog 和任务回归测试。
- `docs/quest/`：任务编写、客户端证据和修复 playbook。
- `scripts/`：数据生成、审计和验证工具。
- `aion/`：运行目录，不作为源码或长期构建输入。

## Rules Index

所有详细规则位于 `.agent/rules/`：

| 文件 | 适用范围 | 说明 |
|---|---|---|
| [general.md](.agent/rules/general.md) | 全仓库 | 命令、搜索、编辑、验证、Git 和运行边界 |
| [quest-repair.md](.agent/rules/quest-repair.md) | 任务 XML、QuestEngine、任务 AI、任务测试及 `docs/quest/` | 任务证据、修复、验收和 playbook 更新规则 |
