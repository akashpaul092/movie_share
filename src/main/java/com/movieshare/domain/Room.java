package com.movieshare.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 8)
	private String code;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(length = 255)
	private String name;

	@Column(name = "admin_secret", nullable = false, length = 64)
	private String adminSecret;

	protected Room() {
	}

	public Room(UUID id, String code, Instant createdAt, String name, String adminSecret) {
		this.id = id;
		this.code = code;
		this.createdAt = createdAt;
		this.name = name;
		this.adminSecret = adminSecret;
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getName() {
		return name;
	}

	public String getAdminSecret() {
		return adminSecret;
	}

}
