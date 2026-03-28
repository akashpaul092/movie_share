package com.movieshare.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movieshare.api.dto.RoomCreateResponse;
import com.movieshare.api.dto.RoomResponse;
import com.movieshare.domain.Room;
import com.movieshare.domain.RoomRepository;

@Service
public class RoomService {

	private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final int CODE_LENGTH = 8;
	private static final int MAX_ATTEMPTS = 16;

	private final SecureRandom random = new SecureRandom();
	private final RoomRepository roomRepository;

	public RoomService(RoomRepository roomRepository) {
		this.roomRepository = roomRepository;
	}

	@Transactional
	public RoomCreateResponse createRoom(String name) {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			String code = generateCode();
			if (roomRepository.existsByCode(code)) {
				continue;
			}
			String adminSecret = UUID.randomUUID().toString();
			Room room = new Room(UUID.randomUUID(), code, Instant.now(), blankToNull(name), adminSecret);
			roomRepository.save(room);
			return RoomCreateResponse.of(code, adminSecret);
		}
		throw new IllegalStateException("Could not allocate a unique room code");
	}

	@Transactional(readOnly = true)
	public Optional<RoomResponse> getByCode(String code) {
		String normalized = normalizeCode(code);
		return roomRepository.findByCode(normalized).map(this::toResponse);
	}

	private RoomResponse toResponse(Room room) {
		return new RoomResponse(room.getCode(), room.getCreatedAt(), room.getName());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	public static String normalizeCode(String code) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Room code is required");
		}
		return code.trim().toUpperCase();
	}

	private String generateCode() {
		char[] buf = new char[CODE_LENGTH];
		for (int i = 0; i < CODE_LENGTH; i++) {
			buf[i] = CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)];
		}
		return new String(buf);
	}

}
