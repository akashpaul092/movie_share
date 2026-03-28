package com.movieshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieshare.sync.SyncMessage;
import com.movieshare.sync.SyncType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class StompSyncIntegrationTest {

	@LocalServerPort
	private int port;

	@Test
	void stompBroadcastsSyncMessage() throws Exception {
		RestClient http = RestClient.builder()
				.baseUrl("http://127.0.0.1:" + port)
				.build();

		String created = http.post()
				.uri("/api/rooms")
				.contentType(MediaType.APPLICATION_JSON)
				.body("{}")
				.retrieve()
				.body(String.class);

		assertThat(created).isNotNull();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(created);
		String code = root.path("roomCode").asText();
		String adminToken = root.path("adminToken").asText();
		assertThat(code).isNotBlank();
		assertThat(adminToken).isNotBlank();

		List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
		SockJsClient sockJsClient = new SockJsClient(transports);
		WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setInboundMessageSizeLimit(64 * 1024);
		stompClient.setMessageConverter(new StringMessageConverter());

		BlockingQueue<SyncMessage> inbound = new LinkedBlockingQueue<>();

		SyncMessage out = new SyncMessage(SyncType.PLAY, "dQw4w9WgXcQ", 1.0, System.currentTimeMillis(), "test-sender", adminToken);

		stompClient.connectAsync("http://127.0.0.1:" + port + "/ws", new StompSessionHandlerAdapter() {
			@Override
			public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
				session.subscribe("/topic/room." + code, new StompFrameHandler() {
					@Override
					public Type getPayloadType(StompHeaders headers) {
						return String.class;
					}

					@Override
					public void handleFrame(StompHeaders headers, Object payload) {
						try {
							inbound.offer(mapper.readValue((String) payload, SyncMessage.class));
						}
						catch (Exception ignored) {
						}
					}
				});
				try {
					session.send("/app/room/" + code + "/sync", mapper.writeValueAsString(out));
				}
				catch (Exception ignored) {
				}
			}
		}).get(15, TimeUnit.SECONDS);

		SyncMessage received = inbound.poll(15, TimeUnit.SECONDS);
		assertThat(received).isNotNull();
		assertThat(received.type()).isEqualTo(SyncType.PLAY);
		assertThat(received.videoId()).isEqualTo("dQw4w9WgXcQ");
		assertThat(received.adminToken()).isNull();
	}

}
