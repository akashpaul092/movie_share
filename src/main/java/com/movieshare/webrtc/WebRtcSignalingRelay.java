package com.movieshare.webrtc;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebRtcSignalingRelay implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(WebRtcSignalingRelay.class);
	private static final String PREFIX = RoomWebRtcStompController.REDIS_CHANNEL_PREFIX;

	private final SimpMessagingTemplate messagingTemplate;

	public WebRtcSignalingRelay(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		if (!channel.startsWith(PREFIX)) {
			return;
		}
		String roomCode = channel.substring(PREFIX.length());
		if (roomCode.isEmpty() || roomCode.indexOf(':') >= 0) {
			return;
		}
		try {
			String body = new String(message.getBody(), StandardCharsets.UTF_8);
			messagingTemplate.convertAndSend("/topic/room." + roomCode + ".webrtc", body);
		}
		catch (Exception e) {
			log.debug("Failed to relay WebRTC signal: {}", e.getMessage());
		}
	}
}
