package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.model.UserProjection;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.devices
            WHERE u.username = :username
            """)
    Optional<User> findByUsernameWithDevices(@NotBlank @Param("username") String username);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.devices
            WHERE u.email = :email
            """)
    Optional<User> findByEmailWithDevices(@NotBlank @Email(message = "Email must be valid") String email);

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

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.devices
            """)
    Page<User> findAllWithDevices(Pageable pageable);
}
