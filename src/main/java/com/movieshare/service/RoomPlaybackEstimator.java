package com.movieshare.service;

import java.time.Instant;

import com.movieshare.api.dto.RoomStateResponse;

public final class RoomPlaybackEstimator {

	private RoomPlaybackEstimator() {
	}

	/**
	 * Adjusts {@code positionSec} using elapsed time since {@code clientTimestamp} when {@code playing} is true,
	 * so late joiners catch up to live playback.
	 */
	public static RoomStateResponse withEstimatedPlayback(RoomStateResponse raw, Instant now) {
		if (raw == null || raw.videoId() == null || raw.positionSec() == null || raw.clientTimestamp() == null) {
			return raw;
		}
		boolean isPlaying = Boolean.TRUE.equals(raw.playing());
		double pos = raw.positionSec();
		if (isPlaying) {
			long elapsedMs = now.toEpochMilli() - raw.clientTimestamp();
			if (elapsedMs > 0) {
				pos = raw.positionSec() + elapsedMs / 1000.0;
			}
		}
		return new RoomStateResponse(raw.videoId(), pos, isPlaying, raw.clientTimestamp());
	}

}
