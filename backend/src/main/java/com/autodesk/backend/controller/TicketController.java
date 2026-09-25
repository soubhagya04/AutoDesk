package com.autodesk.backend.controller;

import com.autodesk.backend.dto.request.AssignTicketRequest;
import com.autodesk.backend.dto.request.CommentRequest;
import com.autodesk.backend.dto.request.StatusUpdateRequest;
import com.autodesk.backend.dto.request.TicketRequest;
import com.autodesk.backend.dto.response.CommentResponse;
import com.autodesk.backend.dto.response.TicketResponse;
import com.autodesk.backend.entity.User;
import com.autodesk.backend.security.UserDetailsImpl;
import com.autodesk.backend.service.CommentService;
import com.autodesk.backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;

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

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {
        TicketResponse response = ticketService.assignTicket(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = ticketService.getUserFromDetails(userDetails);
        CommentResponse response = commentService.addComment(id, request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User currentUser = ticketService.getUserFromDetails(userDetails);
        List<CommentResponse> comments = commentService.getCommentsByTicketId(id, currentUser);
        return ResponseEntity.ok(comments);
    }
}
