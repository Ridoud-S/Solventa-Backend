package com.solventa.solventa_backend.users.repository;

import com.solventa.solventa_backend.users.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findById(UUID id);   

    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findByInvitationToken(String token);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<User> findByResetToken(String token);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);


    List<User> findAllByTenantId(UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}