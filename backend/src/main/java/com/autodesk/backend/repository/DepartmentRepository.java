package com.autodesk.backend.repository;

import com.autodesk.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    Boolean existsByName(String name);
}
