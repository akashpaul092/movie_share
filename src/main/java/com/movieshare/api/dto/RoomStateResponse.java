package com.movieshare.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomStateResponse(
		String videoId,
		Double positionSec,
		Boolean playing,
		Long clientTimestamp
) {
}
