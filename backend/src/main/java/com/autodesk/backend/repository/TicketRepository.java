package com.autodesk.backend.repository;

import com.autodesk.backend.entity.Department;
import com.autodesk.backend.entity.Ticket;
import com.autodesk.backend.entity.TicketStatus;
import com.autodesk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    List<Ticket> findByAssignedToOrderByCreatedAtDesc(User assignedTo);

    List<Ticket> findByDepartmentOrderByCreatedAtDesc(Department department);

    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo = :user OR (t.assignedTo IS NULL AND t.department = :department) ORDER BY t.createdAt DESC")
    List<Ticket> findForEngineer(@Param("user") User user, @Param("department") Department department);
}
