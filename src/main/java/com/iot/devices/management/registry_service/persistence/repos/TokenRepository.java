package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    @Query(value = """
      SELECT t FROM Token t JOIN User u\s
      ON t.user.id = u.id\s
      WHERE u.id = :id AND (t.expired = false or t.revoked = false)\s
      """)
    List<Token> findAllValidTokenByUser(@Param("id") UUID userId);

    Optional<Token> findByToken(String token);
}
