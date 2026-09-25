package com.autodesk.backend.service;

import com.autodesk.backend.dto.request.CommentRequest;
import com.autodesk.backend.dto.response.CommentResponse;
import com.autodesk.backend.entity.Comment;
import com.autodesk.backend.entity.Ticket;
import com.autodesk.backend.entity.User;
import com.autodesk.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request, User currentUser) {
        Ticket ticket = ticketService.getTicketEntity(ticketId, currentUser);

        Comment comment = Comment.builder()
                .content(request.getContent())
                .ticket(ticket)
                .user(currentUser)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByTicketId(Long ticketId, User currentUser) {
        // Validates ticket exists and currentUser has access permission
        ticketService.getTicketEntity(ticketId, currentUser);

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return comments.stream().map(this::mapToResponse).toList();
    }

    public CommentResponse mapToResponse(Comment comment) {
        if (comment == null) return null;

        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket() != null ? comment.getTicket().getId() : null)
                .content(comment.getContent())
                .user(ticketService.mapUserToSummary(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
