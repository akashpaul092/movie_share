package com.movieshare;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads a project root {@code .env} into default Spring properties. Spring Boot does not read
 * {@code .env} by itself; without this, {@code ./mvnw spring-boot:run} would not see those values
 * unless you export them in the shell (or use systemd {@code EnvironmentFile}, etc.).
 */
final class DotEnvLoader {

	private DotEnvLoader() {
	}

	static Map<String, Object> loadAsDefaultProperties() {
		Dotenv dotenv = Dotenv.configure().directory(".").ignoreIfMissing().load();
		Map<String, Object> map = new HashMap<>();
		dotenv.entries().forEach(e -> {
			String key = e.getKey();
			String value = e.getValue();
			if (key == null || key.isBlank()) {
				return;
			}
			map.put(key, value);
			// SPRING_DATASOURCE_URL -> spring.datasource.url (Spring relaxed binding)
			if (key.indexOf('_') >= 0) {
				map.put(key.toLowerCase(Locale.ROOT).replace('_', '.'), value);
			}
		});
		return map;
	}
}
