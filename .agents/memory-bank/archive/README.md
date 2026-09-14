# Memory Bank Archive (历史归档区)

本目录用于存放已经关闭、被替代或不再参与日常实时排查的历史记录。归档文件保留出处和结论，但默认不作为当前规则直接执行。

## 归档准则 (When to Archive)

1. **已闭环的一次性专项**：结果、验证边界和剩余风险已经记录，且未来不会作为日常操作规则。
2. **被新架构替代的旧方案**：必须注明替代模式、替代提交或新的权威文档。
3. **过期但仍有诊断价值的记录**：保留原始日期和验证环境，不能把旧环境结论伪装成当前事实。
4. **保持日常检索轻量**：日常排查优先读取 `systemPatterns.md` 和 `patterns/`，不要默认扫描整个归档区。

## 每个归档条目的最低字段 (Archive Metadata)

```text
status: ARCHIVED | SUPERSEDED | HISTORICAL
scope: 适用 checkout、分支、版本或运行环境
source: 原始 summary、commit、日志或文档路径
last_verified: YYYY-MM-DD 或 unknown
replacement: Pattern ID、文档路径或 none
read_by_default: no
```

归档不是删除的替代借口：若记录仍包含当前可复用的不变量，应先提炼到 `patterns/`，再把原始战役记录放入此处。
