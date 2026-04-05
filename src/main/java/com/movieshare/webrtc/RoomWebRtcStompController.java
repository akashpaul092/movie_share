package com.movieshare.webrtc;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieshare.domain.RoomRepository;
import com.movieshare.service.RoomService;

import org.springframework.data.redis.core.StringRedisTemplate;

@Controller
public class RoomWebRtcStompController {

	static final String REDIS_CHANNEL_PREFIX = "roomwebrtc:";
	static final int MAX_PAYLOAD_CHARS = 32000;

	private static final Set<String> ALLOWED_TYPES = Set.of(
			"joined-video",
			"left-video",
			"offer",
			"answer",
			"ice"
	);

	private final RoomRepository roomRepository;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public RoomWebRtcStompController(
			RoomRepository roomRepository,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper
	) {
		this.roomRepository = roomRepository;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@MessageMapping("/room/{code}/webrtc")
	public void relay(@DestinationVariable String code, @Payload byte[] rawPayload) throws Exception {
		if (rawPayload == null || rawPayload.length > MAX_PAYLOAD_CHARS) {
			return;
		}
		String body = new String(rawPayload, StandardCharsets.UTF_8);
		WebRtcSignal signal = objectMapper.readValue(body, WebRtcSignal.class);
		if (signal.type() == null || signal.from() == null) {
			return;
		}
		if (!ALLOWED_TYPES.contains(signal.type())) {
			return;
		}
		String from = signal.from().trim();
		if (from.isEmpty() || from.length() > 128) {
			return;
		}
		String normalizedCode = RoomService.normalizeCode(code);
		if (!roomRepository.existsByCode(normalizedCode)) {
			return;
		}
		stringRedisTemplate.convertAndSend(REDIS_CHANNEL_PREFIX + normalizedCode, body);
	}
}
