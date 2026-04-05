package com.movieshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RoomApiIntegrationTest {

	@LocalServerPort
	private int port;

	@Test
	void createAndGetRoom() {
		RestClient client = RestClient.builder()
				.baseUrl("http://127.0.0.1:" + port)
				.build();

		ResponseEntity<String> post = client.post()
				.uri("/api/rooms")
				.contentType(MediaType.APPLICATION_JSON)
				.body("{}")
				.retrieve()
				.toEntity(String.class);

		assertThat(post.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(post.getBody()).contains("roomCode");
		assertThat(post.getBody()).contains("adminToken");
		assertThat(post.getBody()).contains("WATCH_PARTY");

		String code = post.getBody().split("\"roomCode\":\"")[1].split("\"")[0];

		ResponseEntity<String> get = client.get()
				.uri("/api/rooms/{code}", code)
				.retrieve()
				.toEntity(String.class);

		assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(get.getBody()).contains(code);
		assertThat(get.getBody()).contains("WATCH_PARTY");
	}

	@Test
	void createMeetRoom() {
		RestClient client = RestClient.builder()
				.baseUrl("http://127.0.0.1:" + port)
				.build();

		ResponseEntity<String> post = client.post()
				.uri("/api/rooms")
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"name\":\"standup\",\"kind\":\"MEET\"}")
				.retrieve()
				.toEntity(String.class);

		assertThat(post.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(post.getBody()).contains("MEET");
	}

}
