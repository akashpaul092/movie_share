package com.movieshare.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(String text, String senderId, Long clientTimestamp, String displayName) {
}
