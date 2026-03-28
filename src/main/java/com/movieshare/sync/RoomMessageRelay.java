package com.movieshare.sync;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieshare.service.RoomStateCache;

@Component
public class RoomMessageRelay implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(RoomMessageRelay.class);
	private static final String CHANNEL_PREFIX = "room:";

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;
	private final RoomStateCache roomStateCache;

	public RoomMessageRelay(
			SimpMessagingTemplate messagingTemplate,
			ObjectMapper objectMapper,
			RoomStateCache roomStateCache
	) {
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
		this.roomStateCache = roomStateCache;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		if (!channel.startsWith(CHANNEL_PREFIX)) {
			return;
		}
		String roomCode = channel.substring(CHANNEL_PREFIX.length());
		if (roomCode.isEmpty() || roomCode.indexOf(':') >= 0) {
			return;
		}
		try {
			String body = new String(message.getBody(), StandardCharsets.UTF_8);
			SyncMessage sync = objectMapper.readValue(body, SyncMessage.class);
			roomStateCache.updateFromMessage(roomCode, sync);
			messagingTemplate.convertAndSend("/topic/room." + roomCode, body);
		}
		catch (Exception e) {
			log.debug("Failed to relay room message: {}", e.getMessage());
		}
	}

}
