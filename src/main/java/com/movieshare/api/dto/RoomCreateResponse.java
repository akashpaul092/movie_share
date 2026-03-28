package com.movieshare.api.dto;

public record RoomCreateResponse(String roomCode, String adminToken, String wsPath, String stompDestinationPrefix) {

	public static RoomCreateResponse of(String roomCode, String adminToken) {
		return new RoomCreateResponse(roomCode, adminToken, "/ws", "/app");
	}

}
