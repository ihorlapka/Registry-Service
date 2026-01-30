package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.UserProjection;
import jakarta.validation.constraints.Email;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(@NonNull @Email String email);

    Optional<User> findByUsername(@NonNull String username);

    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    int removeById(@NonNull @Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :id")
    int updateLastLoginTime(@Param("id") UUID id, @Param("loginTime") OffsetDateTime loginTime);

    @Query("""
            SELECT new com.iot.devices.management.registry_service.persistence.model.UserProjection(u.id, u.username, u.userRole)
            FROM User u
            JOIN Device d ON u.id = d.owner.id
            WHERE d.id = :deviceId
            """)
    Optional<UserProjection> findUserProjectionByDeviceId(@Param("deviceId") UUID deviceId);
}
