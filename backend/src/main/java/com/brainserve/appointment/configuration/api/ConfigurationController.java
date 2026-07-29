package com.brainserve.appointment.configuration.api;

import com.brainserve.appointment.shared.audit.AuditService;
import com.brainserve.appointment.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1")
public class ConfigurationController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public ConfigurationController(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            CurrentUser currentUser,
            AuditService audit) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping("/configuration/public")
    Map<String, Object> publicConfiguration() {
        return read("where public_value = true");
    }

    @GetMapping("/admin/configuration")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURE')")
    Map<String, Object> allConfiguration() {
        return read("");
    }

    @PutMapping("/admin/configuration/{key}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIGURE')")
    @Transactional
    Map<String, Object> update(@PathVariable String key, @Valid @RequestBody ConfigurationRequest request) {
        if (!key.matches("[a-zA-Z0-9._-]{3,120}")) {
            throw new IllegalArgumentException("Invalid configuration key");
        }
        JsonNode value = validateType(request.type(), request.value());
        UUID actor = currentUser.id();
        jdbc.update("""
                insert into system_configuration(config_key, value_json, value_type, public_value, version, updated_by, updated_at)
                values (?, cast(? as jsonb), ?, ?, 0, ?, now())
                on conflict (config_key) do update
                set value_json = excluded.value_json,
                    value_type = excluded.value_type,
                    public_value = excluded.public_value,
                    version = system_configuration.version + 1,
                    updated_by = excluded.updated_by,
                    updated_at = now()
                """, key, value.toString(), request.type(), request.publicValue(), actor);
        audit.record(actor, "UPDATE", "SYSTEM_CONFIGURATION", key, null, "INTERNAL");
        return Map.of("key", key, "value", value, "type", request.type(), "publicValue", request.publicValue());
    }

    private Map<String, Object> read(String clause) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select config_key, value_json::text, value_type, public_value, version from system_configuration " + clause + " order by config_key");
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            try {
                values.put((String) row.get("config_key"), objectMapper.readTree((String) row.get("value_json")));
            } catch (JacksonException exception) {
                throw new IllegalStateException("Stored configuration is invalid", exception);
            }
        }
        return values;
    }

    private JsonNode validateType(String type, JsonNode value) {
        boolean valid = switch (type) {
            case "STRING" -> value.isTextual();
            case "INTEGER" -> value.isIntegralNumber();
            case "BOOLEAN" -> value.isBoolean();
            case "JSON" -> value.isObject() || value.isArray();
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("Configuration value does not match its declared type");
        return value;
    }

    record ConfigurationRequest(
            @NotNull JsonNode value,
            @NotBlank @Pattern(regexp = "STRING|INTEGER|BOOLEAN|JSON") String type,
            boolean publicValue) {
    }
}
