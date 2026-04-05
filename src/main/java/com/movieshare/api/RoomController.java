package com.movieshare.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.movieshare.api.dto.RoomCreateResponse;
import com.movieshare.api.dto.RoomResponse;
import com.movieshare.api.dto.RoomStateResponse;
import com.movieshare.domain.RoomKind;
import com.movieshare.service.RoomCreationRateLimiter;
import com.movieshare.service.RoomPlaybackEstimator;
import com.movieshare.service.RoomService;
import com.movieshare.service.RoomStateCache;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

	private final RoomService roomService;
	private final RoomStateCache roomStateCache;
	private final RoomCreationRateLimiter rateLimiter;

	public RoomController(RoomService roomService, RoomStateCache roomStateCache, RoomCreationRateLimiter rateLimiter) {
		this.roomService = roomService;
		this.roomStateCache = roomStateCache;
		this.rateLimiter = rateLimiter;
	}

	@PostMapping
	public ResponseEntity<?> createRoom(@RequestBody(required = false) CreateRoomBody body, HttpServletRequest request) {
		String clientKey = clientKey(request);
		if (!rateLimiter.allow(clientKey)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many rooms created. Try again later.");
		}
		RoomKind kind;
		try {
			kind = parseKind(body != null ? body.kind() : null);
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("kind must be WATCH_PARTY or MEET");
		}
		String name = body != null ? body.name() : null;
		RoomCreateResponse created = roomService.createRoom(name, kind);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/{code}")
	public ResponseEntity<RoomWithStateResponse> getRoom(@PathVariable("code") String code) {
		try {
			String normalized = RoomService.normalizeCode(code);
			return roomService.getByCode(normalized)
					.map(r -> new RoomWithStateResponse(
							r,
							roomStateCache.getState(normalized)
									.map(s -> RoomPlaybackEstimator.withEstimatedPlayback(s, Instant.now()))
									.orElse(null)
					))
					.map(ResponseEntity::ok)
					.orElse(ResponseEntity.notFound().build());
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	private static RoomKind parseKind(String raw) {
		if (raw == null || raw.isBlank()) {
			return RoomKind.WATCH_PARTY;
		}
		return RoomKind.valueOf(raw.trim().toUpperCase());
	}

	private static String clientKey(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
	}

	public record CreateRoomBody(String name, String kind) {
	}

	public record RoomWithStateResponse(RoomResponse room, RoomStateResponse state) {
	}

}
