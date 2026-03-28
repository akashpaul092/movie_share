package com.movieshare.service;

import java.time.Duration;
import java.util.Optional;

import com.movieshare.sync.SyncType;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieshare.api.dto.RoomStateResponse;
import com.movieshare.sync.SyncMessage;

@Service
public class RoomStateCache {

	static final String KEY_PREFIX_STATE = "room:";

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public RoomStateCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	private static String stateKey(String roomCode) {
		return KEY_PREFIX_STATE + roomCode + ":state";
	}

	public void updateFromMessage(String roomCode, SyncMessage message) {
		RoomStateResponse state = deriveState(message);
		if (state == null) {
			return;
		}
		RoomStateResponse toStore = state;
		if (message.type() == SyncType.SEEK) {
			Optional<RoomStateResponse> prev = getState(roomCode);
			Boolean keepPlaying = prev.map(RoomStateResponse::playing).orElse(false);
			String vid = message.videoId() != null ? message.videoId() : prev.map(RoomStateResponse::videoId).orElse(null);
			toStore = new RoomStateResponse(vid, state.positionSec(), keepPlaying, state.clientTimestamp());
		}
		try {
			String json = objectMapper.writeValueAsString(toStore);
			redis.opsForValue().set(stateKey(roomCode), json, Duration.ofHours(24));
		}
		catch (JsonProcessingException ignored) {
		}
	}

	public Optional<RoomStateResponse> getState(String roomCode) {
		String raw = redis.opsForValue().get(stateKey(roomCode));
		if (raw == null || raw.isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(raw, RoomStateResponse.class));
		}
		catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

	private static RoomStateResponse deriveState(SyncMessage message) {
		if (message.type() == null) {
			return null;
		}
		return switch (message.type()) {
			case VIDEO_CHANGE -> new RoomStateResponse(message.videoId(), message.positionSec(), false, message.clientTimestamp());
			case PLAY -> new RoomStateResponse(message.videoId(), message.positionSec(), true, message.clientTimestamp());
			case PAUSE -> new RoomStateResponse(message.videoId(), message.positionSec(), false, message.clientTimestamp());
			case SEEK -> new RoomStateResponse(message.videoId(), message.positionSec(), null, message.clientTimestamp());
		};
	}

}
