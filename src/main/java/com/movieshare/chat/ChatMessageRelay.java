package com.movieshare.chat;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageRelay implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(ChatMessageRelay.class);
	private static final String CHANNEL_PREFIX = RoomChatStompController.REDIS_CHANNEL_PREFIX;

	private final SimpMessagingTemplate messagingTemplate;

	public ChatMessageRelay(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
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
			messagingTemplate.convertAndSend("/topic/room." + roomCode + ".chat", body);
		}
		catch (Exception e) {
			log.debug("Failed to relay chat message: {}", e.getMessage());
		}
	}

}
