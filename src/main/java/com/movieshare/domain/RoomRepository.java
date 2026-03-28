package com.movieshare.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

	Optional<Room> findByCode(String code);

	boolean existsByCode(String code);

}
