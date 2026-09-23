package com.autodesk.backend.repository;

import com.autodesk.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Boolean existsByName(String name);
}
