package com.movieshare.webrtc;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebRtcSignal(
		String type,
		String from,
		String to,
		String sdp,
		String sdpType,
		String candidate,
		String sdpMid,
		Integer sdpMLineIndex
) {
}
