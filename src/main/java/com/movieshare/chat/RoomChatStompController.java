package com.movieshare.chat;

import java.nio.charset.StandardCharsets;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieshare.domain.RoomRepository;
import com.movieshare.service.RoomService;

import org.springframework.data.redis.core.StringRedisTemplate;

@Controller
public class RoomChatStompController {

	static final String REDIS_CHANNEL_PREFIX = "roomchat:";
	static final int MAX_PAYLOAD_CHARS = 4096;
	private static final int MAX_TEXT_LENGTH = 500;

	private final RoomRepository roomRepository;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public RoomChatStompController(
			RoomRepository roomRepository,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper
	) {
		this.roomRepository = roomRepository;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@MessageMapping("/room/{code}/chat")
	public void chat(@DestinationVariable String code, @Payload byte[] rawPayload) throws Exception {
		if (rawPayload == null || rawPayload.length > MAX_PAYLOAD_CHARS) {
			return;
		}
		String body = new String(rawPayload, StandardCharsets.UTF_8);
		ChatMessage message = objectMapper.readValue(body, ChatMessage.class);
		if (message.text() == null) {
			return;
		}
		String text = message.text().trim();
		if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
			return;
		}
		String displayName = message.displayName() == null ? null : message.displayName().trim();
		if (displayName != null && displayName.length() > 40) {
			displayName = displayName.substring(0, 40);
		}
		ChatMessage normalized = new ChatMessage(
				text,
				message.senderId(),
				message.clientTimestamp(),
				displayName == null || displayName.isEmpty() ? null : displayName
		);
		String normalizedBody = objectMapper.writeValueAsString(normalized);
		String normalizedCode = RoomService.normalizeCode(code);
		if (!roomRepository.existsByCode(normalizedCode)) {
			return;
		}
		stringRedisTemplate.convertAndSend(REDIS_CHANNEL_PREFIX + normalizedCode, normalizedBody);
	}

}
