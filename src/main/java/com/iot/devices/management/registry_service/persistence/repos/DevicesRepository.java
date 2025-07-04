package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.Device;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicesRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findBySerialNumber(@NonNull @NotBlank(message = "serial number is required") String serialNumber);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query("DELETE FROM Device d WHERE d.id = :id")
    int removeById(@NonNull @Param("id") UUID id);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(:status, status),
            last_active_at = COALESCE(:lastActiveAt, last_active_at),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            battery_level = COALESCE(:batteryLevel, battery_level)
            updated_at = COALESCE(:updatedAt, updated_at)
            telemetry =
              COALESCE(CASE WHEN :doorState IS NOT NULL THEN jsonb_set(telemetry, '{doorState}', to_jsonb(:doorState::text), true) ELSE telemetry END, telemetry)
              |>
              COALESCE(CASE WHEN :tamperAlert IS NOT NULL THEN jsonb_set(telemetry, '{tamperAlert}', to_jsonb(:tamperAlert), true) ELSE telemetry END, telemetry)
              |>
              COALESCE(CASE WHEN :lastOpened IS NOT NULL THEN jsonb_set(telemetry, '{lastOpened}', to_jsonb(EXTRACT(EPOCH FROM :lastOpened) * 1000)::to_jsonb, true) ELSE telemetry END, telemetry)
            WHERE device_id = :id
            """, nativeQuery = true)
    int updateDoorSensorTelemetry(@NonNull @Param("id") String id,
                                  @Param("status") String status,
                                  @Param("lastActiveAt") OffsetDateTime lastActiveAt,
                                  @Param("firmwareVersion") String firmwareVersion,
                                  @Param("batteryLevel") Integer batteryLevel,
                                  @Param("updatedAt") OffsetDateTime updatedAt,
                                  @Param("doorState") String doorState,
                                  @Param("tamperAlert") Boolean tamperAlert,
                                  @Param("lastOpened") OffsetDateTime lastOpened);

//    @Modifying
//    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
//    @Query(value = """
//            UPDATE devices SET
//            status = COALESCE(:status, status),
//            last_active_at = COALESCE(:lastActiveAt, last_active_at),
//            firmware_version = COALESCE(:firmwareVersion, firmware_version),
//            updated_at = COALESCE(:updatedAt, updated_at)
//            telemetry =
//              COALESCE(CASE WHEN :voltage IS NOT NULL THEN jsonb_set(telemetry, '{voltage}', to_jsonb(:doorState::text), true) ELSE telemetry END, telemetry)
//              |>
//              COALESCE(CASE WHEN :tamperAlert IS NOT NULL THEN jsonb_set(telemetry, '{tamperAlert}', to_jsonb(:tamperAlert), true) ELSE telemetry END, telemetry)
//              |>
//              COALESCE(CASE WHEN :lastOpened IS NOT NULL THEN jsonb_set(telemetry, '{lastOpened}', to_jsonb(EXTRACT(EPOCH FROM :lastOpened) * 1000)::to_jsonb, true) ELSE telemetry END, telemetry)
//            WHERE device_id = :id
//            """, nativeQuery = true)
//    int updateEnergyMeterTelemetry(@NonNull @Param("id") String id,
//                                  @Param("status") String status,
//                                  @Param("lastActiveAt") OffsetDateTime lastActiveAt,
//                                  @Param("firmwareVersion") String firmwareVersion,
//                                  @Param("batteryLevel") Integer batteryLevel,
//                                  @Param("updatedAt") OffsetDateTime updatedAt,
//                                  @Param("doorState") String doorState,
//                                  @Param("tamperAlert") Boolean tamperAlert,
//                                  @Param("lastOpened") OffsetDateTime lastOpened);

}
