package com.movieshare.sync;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieshare.domain.Room;
import com.movieshare.domain.RoomKind;
import com.movieshare.domain.RoomRepository;
import com.movieshare.service.RoomService;

import org.springframework.data.redis.core.StringRedisTemplate;

@Controller
public class RoomSyncStompController {

	private static final int MAX_PAYLOAD_CHARS = 8192;

	private final RoomRepository roomRepository;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public RoomSyncStompController(
			RoomRepository roomRepository,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper
	) {
		this.roomRepository = roomRepository;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@MessageMapping("/room/{code}/sync")
	public void sync(@DestinationVariable String code, @Payload byte[] rawPayload) throws Exception {
		if (rawPayload == null || rawPayload.length > MAX_PAYLOAD_CHARS) {
			return;
		}
		String body = new String(rawPayload, StandardCharsets.UTF_8);
		SyncMessage message = objectMapper.readValue(body, SyncMessage.class);
		if (message.type() == null) {
			return;
		}
		String normalized = RoomService.normalizeCode(code);
		Optional<Room> room = roomRepository.findByCode(normalized);
		if (room.isEmpty()) {
			return;
		}
		if (room.get().getKind() != RoomKind.WATCH_PARTY) {
			return;
		}
		if (!adminTokensMatch(room.get().getAdminSecret(), message.adminToken())) {
			return;
		}
		SyncMessage publicMessage = new SyncMessage(
				message.type(),
				message.videoId(),
				message.positionSec(),
				message.clientTimestamp(),
				message.senderId(),
				null
		);
		String publicBody = objectMapper.writeValueAsString(publicMessage);
		stringRedisTemplate.convertAndSend("room:" + normalized, publicBody);
	}

	private static boolean adminTokensMatch(String expected, String provided) {
		if (expected == null || provided == null) {
			return false;
		}
		byte[] a = expected.getBytes(StandardCharsets.UTF_8);
		byte[] b = provided.getBytes(StandardCharsets.UTF_8);
		if (a.length != b.length) {
			return false;
		}
		return MessageDigest.isEqual(a, b);
	}

}
