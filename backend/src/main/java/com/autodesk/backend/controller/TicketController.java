package com.autodesk.backend.controller;

import com.autodesk.backend.dto.request.StatusUpdateRequest;
import com.autodesk.backend.dto.request.TicketRequest;
import com.autodesk.backend.dto.response.TicketResponse;
import com.autodesk.backend.entity.User;
import com.autodesk.backend.security.UserDetailsImpl;
import com.autodesk.backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = ticketService.getUserFromDetails(userDetails);
        TicketResponse response = ticketService.createTicket(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = ticketService.getUserFromDetails(userDetails);
        List<TicketResponse> tickets = ticketService.getAllTickets(currentUser);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = ticketService.getUserFromDetails(userDetails);
        TicketResponse response = ticketService.getTicketById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = ticketService.getUserFromDetails(userDetails);
        TicketResponse response = ticketService.updateTicketStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
