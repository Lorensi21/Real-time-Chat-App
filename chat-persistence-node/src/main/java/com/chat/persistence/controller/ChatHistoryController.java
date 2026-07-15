package com.chat.persistence.controller;

import com.chat.common.models.ChatMessage;
import com.chat.persistence.service.ChatHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/history")
public class ChatHistoryController {

    private final ChatHistoryService historyService;

    public ChatHistoryController(ChatHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Flux<ChatMessage>> getRoomHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {

        // Perimeter validation: reject malformed requests before engaging database I/O.
        if (roomId == null || roomId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Flux<ChatMessage> historyStream = historyService.getHistoryByRoomId(roomId, limit);
        return ResponseEntity.ok(historyStream);
    }
}