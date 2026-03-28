package com.movieshare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.movieshare.chat.ChatMessageRelay;
import com.movieshare.sync.RoomMessageRelay;

@Configuration
public class RedisListenerConfig {

	@Bean
	RedisMessageListenerContainer redisMessageListenerContainer(
			RedisConnectionFactory connectionFactory,
			RoomMessageRelay roomMessageRelay,
			ChatMessageRelay chatMessageRelay
	) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(roomMessageRelay, new PatternTopic("room:*"));
		container.addMessageListener(chatMessageRelay, new PatternTopic("roomchat:*"));
		return container;
	}

}
