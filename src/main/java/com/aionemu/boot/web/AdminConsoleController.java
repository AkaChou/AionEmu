package com.aionemu.boot.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.boot.web.config.AdminConfigService;
import com.aionemu.boot.web.config.ConfigGroup;
import com.aionemu.boot.web.config.ConfigKeyComment;
import com.aionemu.boot.web.config.EditableFile;
import com.aionemu.boot.web.config.EffectiveValueResolver;
import com.aionemu.boot.web.config.ValueSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理控制台 REST API。
 * Admin console REST API.
 * <p>
 * 所有接口以 {@code /api/admin-console} 为前缀；除登录与会话查询外，其余接口都要求有效的会话 Cookie。
 * <p>
 * Every endpoint lives under {@code /api/admin-console}; apart from login and the session probe, all of them
 * require a valid session cookie.
 * <p>
 * <b>线程安全</b>：控制器本身无可变状态，委托给 {@link AdminConfigService} 与
 * {@link AdminConsoleSessionService}。
 * <b>Thread safety</b>: the controller holds no mutable state and delegates to {@link AdminConfigService} and
 * {@link AdminConsoleSessionService}.
 *
 * @author AionEmu
 */
@Slf4j
@RestController
@RequestMapping("/api/admin-console")
@RequiredArgsConstructor
public class AdminConsoleController {

	/**
	 * 会话 Cookie 名。
	 * Session cookie name.
	 */
	private static final String SESSION_COOKIE = "ADMIN_CONSOLE_SESSION";

	/**
	 * 配置服务。
	 * Configuration service.
	 */
	private final AdminConfigService configService;

	/**
	 * 会话服务。
	 * Session service.
	 */
	private final AdminConsoleSessionService sessionService;

	/**
	 * 生效值解析器。
	 * Effective value resolver.
	 */
	private final EffectiveValueResolver valueResolver;

	/**
	 * 登录并建立会话。
	 * Logs in and establishes a session.
	 * @param request 登录请求 / login request
	 * @param response 响应 / response
	 * @return 登录结果 / login result
	 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletResponse response) {
		String sessionId = sessionService.login(request.username(), request.password());
		if (sessionId == null) {
			log.warn(I18n.get("console.web.login_failed", request.username()));
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		response.addCookie(new jakarta.servlet.http.Cookie(SESSION_COOKIE, sessionId));
		return ResponseEntity.ok(Map.of("authenticated", true));
	}

	/**
	 * 退出登录。
	 * Logs out.
	 * @param request 请求 / request
	 * @param response 响应 / response
	 * @return 空响应 / empty response
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		sessionService.logout(sessionId(request));
		jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(SESSION_COOKIE, "");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 查询当前会话状态。
	 * Probes the current session state.
	 * @param request 请求 / request
	 * @return 会话状态 / session state
	 */
	@GetMapping("/session")
	public ResponseEntity<Map<String, Object>> session(HttpServletRequest request) {
		return ResponseEntity.ok(Map.of("authenticated", sessionService.isValid(sessionId(request))));
	}

	/**
	 * 列出配置分组：控制台的一级菜单。
	 * Lists the configuration groups: the top-level console menu.
	 * @return 分组列表 / group list
	 */
	@GetMapping("/config/groups")
	public ResponseEntity<List<Map<String, Object>>> groups() {
		List<Map<String, Object>> groups = new ArrayList<>();
		for (ConfigGroup group : ConfigGroup.all()) {
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("id", group.id());
			entry.put("labelZh", group.labelZh());
			entry.put("labelEn", group.labelEn());
			entry.put("directory", group.directory());
			groups.add(entry);
		}
		return ResponseEntity.ok(groups);
	}

	/**
	 * 列出某个分组下的可编辑配置文件。
	 * Lists the editable configuration files of a group.
	 * @param group 分组标识 / the group id
	 * @return 文件列表 / file list
	 */
	@GetMapping("/config/groups/{group}/files")
	public ResponseEntity<List<Map<String, Object>>> files(@PathVariable("group") String groupId) {
		List<Map<String, Object>> files = new ArrayList<>();
		for (EditableFile file : filesOfGroup(groupId)) {
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("id", file.name());
			entry.put("path", file.relativePath());
			entry.put("format", file.format().name());
			entry.put("applyTarget", file.applyTarget().name());
			entry.put("group", file.group().id());
			entry.put("groups", file.groupIds());
			files.add(entry);
		}
		return ResponseEntity.ok(files);
	}

	/**
	 * 读取可编辑文件的内容、键值、生效值与覆盖来源。
	 * Reads the content, key-value pairs, effective values and override sources of an editable file.
	 * @param file 文件标识 / file id
	 * @return 文件视图 / file view
	 */
	@GetMapping("/config/files/{file}")
	public ResponseEntity<Map<String, Object>> file(@PathVariable("file") String fileId) {
		EditableFile file = resolveFile(fileId);
		AdminConfigService.FileView view = configService.read(file);
		List<Map<String, Object>> entries = new ArrayList<>();
		for (Map.Entry<String, String> entry : view.values().entrySet()) {
			EffectiveValueResolver.Resolved resolved = valueResolver.resolve(entry.getKey(), entry.getValue());
			ConfigKeyComment comment = view.comments().get(entry.getKey());
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("key", entry.getKey());
			row.put("fileValue", entry.getValue());
			row.put("effectiveValue", resolved.value());
			row.put("source", resolved.source().name());
			row.put("comment", comment == null ? null : Map.of(
				"description", comment.description() == null ? "" : comment.description(),
				"descriptionEn", comment.descriptionEn() == null ? "" : comment.descriptionEn(),
				"defaultValue", comment.defaultValue() == null ? "" : comment.defaultValue()));
			entries.add(row);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", file.name());
		body.put("path", file.relativePath());
		body.put("format", file.format().name());
		body.put("entries", entries);
		return ResponseEntity.ok(body);
	}

	/**
	 * 预览改动差异，不落盘。
	 * Previews the diff of a change without touching the disk.
	 * @param file 文件标识 / file id
	 * @param request 改动请求 / change request
	 * @return 差异预览 / diff preview
	 */
	@PostMapping("/config/files/{file}/preview")
	public ResponseEntity<Map<String, Object>> preview(@PathVariable("file") String fileId,
			@RequestBody ChangeRequest request) {
		EditableFile file = resolveFile(fileId);
		AdminConfigService.DiffPreview preview = configService.preview(file, request.changes());
		List<Map<String, Object>> lines = new ArrayList<>();
		for (AdminConfigService.DiffLine line : preview.lines()) {
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("lineNumber", line.lineNumber());
			entry.put("before", line.before());
			entry.put("after", line.after());
			lines.add(entry);
		}
		return ResponseEntity.ok(Map.of("lines", lines, "changedKeys", preview.changedKeys()));
	}

	/**
	 * 应用改动：写盘并定向热应用。
	 * Applies a change: writes the file and hot-applies it in a targeted way.
	 * @param file 文件标识 / file id
	 * @param request 改动请求 / change request
	 * @return 应用结果 / apply result
	 */
	@PostMapping("/config/files/{file}/apply")
	public ResponseEntity<Map<String, Object>> apply(@PathVariable("file") String fileId,
			@RequestBody ChangeRequest request) {
		EditableFile file = resolveFile(fileId);
		AdminConfigService.ApplyResult result = configService.apply(file, request.changes());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", result.file().name());
		body.put("applied", result.applied());
		body.put("rolledBack", result.rolledBack());
		body.put("changedKeys", result.changedKeys());
		return ResponseEntity.ok(body);
	}

	/**
	 * 按标识解析可编辑文件。
	 * Resolves an editable file by its id.
	 * @param fileId 文件标识 / file id
	 * @return 可编辑文件 / editable file
	 */
	private EditableFile resolveFile(String fileId) {
		EditableFile file = EditableFile.byName(fileId);
		if (file != null) {
			return file;
		}
		throw new IllegalArgumentException("Unknown editable file: " + fileId);
	}

	/**
	 * 列出分组下的可编辑文件；分组不存在时返回空列表。
	 * Lists the editable files of a group; an unknown group yields an empty list.
	 * @param groupId 分组标识 / the group id
	 * @return 文件列表 / the file list
	 */
	private List<EditableFile> filesOfGroup(String groupId) {
		ConfigGroup group = ConfigGroup.byId().get(groupId);
		return group == null ? List.of() : group.files();
	}

	/**
	 * 从请求中取出会话标识。
	 * Extracts the session id from the request.
	 * @param request 请求 / request
	 * @return 会话标识；缺失时为 {@code null} / the session id, {@code null} when absent
	 */
	private String sessionId(HttpServletRequest request) {
		jakarta.servlet.http.Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (jakarta.servlet.http.Cookie cookie : cookies) {
			if (SESSION_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	/**
	 * 登录请求。
	 * Login request.
	 *
	 * @param username 用户名 / user name
	 * @param password 密码 / password
	 */
	public record LoginRequest(String username, String password) {
	}

	/**
	 * 改动请求。
	 * Change request.
	 *
	 * @param changes 键值改动 / key-value changes
	 */
	public record ChangeRequest(Map<String, String> changes) {
	}
}
