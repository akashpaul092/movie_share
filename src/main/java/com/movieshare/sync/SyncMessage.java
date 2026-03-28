package com.movieshare.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SyncMessage(
		SyncType type,
		String videoId,
		Double positionSec,
		Long clientTimestamp,
		String senderId,
		String adminToken
) {
}
