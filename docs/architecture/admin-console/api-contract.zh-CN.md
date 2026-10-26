# 管理控制台 API 契约

> 前缀：`/api/admin-console` · 认证：会话 Cookie `ADMIN_CONSOLE_SESSION`
> Prefix: `/api/admin-console` · Authentication: session cookie `ADMIN_CONSOLE_SESSION`

---

## 一、认证 (Authentication)

除 `POST /login` 与 `GET /session` 外，所有接口都要求携带有效会话 Cookie。无效或缺失时返回 **401**，
静态资源（控制台页面本身）不受影响，因此未登录时页面仍能加载，由前端跳转到登录视图。

Apart from `POST /login` and `GET /session`, every endpoint requires a valid session cookie. An invalid or
missing cookie yields **401**; the static resources (the console page itself) are unaffected, so the page still
loads when logged out and the frontend routes to the login view.

---

## 二、端点 (Endpoints)

### `POST /login`

建立会话。

```json
{ "username": "aion", "password": "aion" }
```

| 状态码 | 含义 |
|---|---|
| `200` | `{"authenticated": true}`，并下发 `ADMIN_CONSOLE_SESSION` Cookie |
| `401` | 凭据不正确，或凭据缺失（fail-closed） |

### `POST /logout`

结束会话并清除 Cookie。成功返回 **204**。

### `GET /session`

```json
{ "authenticated": true }
```

### `GET /config/groups`

列出配置分组：控制台的一级菜单。

```json
[
  { "id": "administration", "labelZh": "管理配置", "labelEn": "Administration", "directory": "administration" },
  { "id": "main", "labelZh": "主服配置", "labelEn": "Main", "directory": "main" },
  { "id": "shop", "labelZh": "商城管理", "labelEn": "Shop", "directory": "ingameshop" }
]
```

`labelZh` 与 `labelEn` 同时返回：界面按当前语言选取，默认中文。

### `GET /config/groups/{group}/files`

列出某个分组下的可编辑配置文件。`{group}` 为分组标识（大小写敏感）。

```json
[
  {
    "id": "admin",
    "path": "administration/admin.properties",
    "format": "PROPERTIES",
    "applyTarget": "ADMIN_CONFIG",
    "group": "administration",
    "groups": ["administration"]
  }
]
```

`format` 取值：`PROPERTIES`（键值文件）、`ITEM_ID_LIST`（每行一个物品 ID）。
`groups` 列出该文件归属的全部分组：商城开关既出现在「主服配置」也出现在「商城管理」下。

`applyTarget` 取值：

| 取值 | 含义 |
|---|---|
| `ADMIN_CONFIG` | 重跑管理员配置装配。 |
| `DEVELOPER_CONFIG` | 重跑开发者配置装配。 |
| `PANEL_CONFIG` | 重跑面板配置装配。 |
| `COMMANDS` | 重载聊天命令与权限。 |
| `PACKET_FLOOD_FILTER` | 重载数据包洪水过滤器。 |
| `ADMIN_SERVICE` | 重载管理员物品限制服务。 |
| `MAIN_CONFIG` | 重跑该文件对应的游戏服配置类。 |
| `LOGIN_VIP_CONFIG` | 重跑登录服的 VIP 配置装配。 |

### `GET /config/files/{file}`

读取文件详情。`{file}` 为 `GET /config/groups/{group}/files` 返回的 `id`（大小写不敏感）。

```json
{
  "id": "admin",
  "path": "administration/admin.properties",
  "format": "PROPERTIES",
  "entries": [
    {
      "key": "gameserver.administration.gmlevel",
      "fileValue": "3",
      "effectiveValue": "3",
      "source": "FILE",
      "comment": {
        "description": "GM 等级",
        "descriptionEn": "GM Level",
        "defaultValue": "3"
      }
    }
  ]
}
```

`comment` 来自配置文件里键上方的双语注释块；没有注释时为 `null`。
`description` 是中文说明，`descriptionEn` 是英文说明，`defaultValue` 是注释里声明的默认值。

`source` 取值与优先级链对应：

| 取值 | 含义 |
|---|---|
| `FILE` | 值来自被编辑的配置文件。 |
| `OVERRIDE_LAYER` | 值来自 `mygs.properties` 等运维覆盖层。**改这里不会生效**。 |
| `APPLICATION_YML` | 值来自 `application.yml`。**改这里不会生效**。 |
| `ENVIRONMENT` | 值来自操作系统环境变量。**改这里不会生效**。 |
| `SYSTEM_PROPERTY` | 值来自 JVM 系统属性。**改这里不会生效**。 |
| `COMMAND_LINE` | 值来自命令行参数。**改这里不会生效**。 |
| `DEFAULT` | 键不在配置文件中，取值为配置类声明的默认值。 |

### `POST /config/files/{file}/preview`

生成改动差异，**不落盘**。

```json
{ "changes": { "gameserver.administration.gmlevel": "5" } }
```

```json
{
  "lines": [
    { "lineNumber": 16, "before": "gameserver.administration.gmlevel = 3", "after": "gameserver.administration.gmlevel = 5" }
  ],
  "changedKeys": { "gameserver.administration.gmlevel": "5" }
}
```

### `POST /config/files/{file}/apply`

写入改动并定向热应用。请求体与 `preview` 相同。

```json
{
  "id": "ADMIN",
  "applied": true,
  "rolledBack": false,
  "changedKeys": { "gameserver.administration.gmlevel": "5" }
}
```

`applied` 为 `false` 时表示热应用失败；若 `rolledBack` 为 `true`，文件已恢复到写入前的状态。

---

## 三、错误语义 (Error Semantics)

| 状态码 | 场景 |
|---|---|
| `400` | `{file}` 不是可编辑文件标识。 |
| `404` | `{group}` 不是已知分组标识（返回空列表而非 404）。 |
| `401` | 会话无效或缺失。 |
| `404` | 配置文件在磁盘上不存在。 |
| `500` | 写入或热应用失败且回滚也失败（服务端日志有完整异常）。 |

热应用失败但回滚成功时，接口仍返回 **200**，由 `applied: false` 与 `rolledBack: true` 表达。

---

## 四、幂等与并发 (Idempotency and Concurrency)

- 改动以**键值**为单位提交，不含改动的键保持原样。
- 提交与当前文件内容相同的值不会触发写盘，`applied` 仍为 `true`，`changedKeys` 为空。
- 同一文件的写操作由服务端串行化：写入 → 重建配置源 → 热应用是一个整体，中途不会被另一请求插入。
