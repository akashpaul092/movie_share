package com.movieshare.api.dto;

public record RoomCreateResponse(String roomCode, String adminToken, String wsPath, String stompDestinationPrefix, String kind) {

	public static RoomCreateResponse of(String roomCode, String adminToken, String kind) {
		return new RoomCreateResponse(roomCode, adminToken, "/ws", "/app", kind);
	}

}
