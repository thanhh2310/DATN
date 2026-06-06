package com.example.demo.repository;

import com.example.demo.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(@NotBlank(message = "Email cannot be blank") @Email(message = "Invalid email format") String email);

    long countByIsActiveTrue(); // Lấy tổng user đang hoạt động

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN u.userRoles ur
            JOIN ur.role r
            WHERE r.name = :roleName
            """)
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    @Query("""
            SELECT DISTINCT u
            FROM User u
            WHERE LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(u.phoneNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(TRIM(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, '')))) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
