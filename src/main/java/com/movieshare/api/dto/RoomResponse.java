package com.movieshare.api.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomResponse(String code, Instant createdAt, String name, String kind) {
}
