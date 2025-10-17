package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlertRulesRepository extends JpaRepository<AlertRule, UUID> {

    @Query("SELECT ar FROM AlertRule ar WHERE ar.username = :username")
    List<AlertRule> findAlertRulesByUsername(@Param("username") String username);
}
