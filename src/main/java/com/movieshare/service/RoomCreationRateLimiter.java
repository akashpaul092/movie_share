package com.movieshare.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomCreationRateLimiter {

	private final StringRedisTemplate redis;
	private final int maxCreatesPerWindow;
	private final Duration window;

	public RoomCreationRateLimiter(
			StringRedisTemplate redis,
			@Value("${movieshare.rate-limit.room-create-per-hour:20}") int maxCreatesPerWindow
	) {
		this.redis = redis;
		this.maxCreatesPerWindow = maxCreatesPerWindow;
		this.window = Duration.ofHours(1);
	}

	public boolean allow(String clientKey) {
		String key = "ratelimit:roomcreate:" + clientKey;
		Long n = redis.opsForValue().increment(key);
		if (n != null && n == 1L) {
			redis.expire(key, window);
		}
		return n != null && n <= maxCreatesPerWindow;
	}

}
