package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.Device;
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
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            last_active_at = COALESCE(:lastActiveAt, last_active_at),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'doorState', to_jsonb(:doorState),
                                'tamperAlert', to_jsonb(:tamperAlert),
                                'lastOpened', to_jsonb(CAST(:lastOpened AS TIMESTAMP)),
                                'batteryLevel', to_jsonb(:batteryLevel)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateDoorSensorTelemetry(@NonNull @Param("id") UUID id,
                                  @Param("status") String status,
                                  @Param("lastActiveAt") OffsetDateTime lastActiveAt,
                                  @Param("firmwareVersion") String firmwareVersion,
                                  @Param("batteryLevel") Integer batteryLevel,
                                  @Param("updatedAt") OffsetDateTime updatedAt,
                                  @Param("doorState") String doorState,
                                  @Param("tamperAlert") Boolean tamperAlert,
                                  @Param("lastOpened") OffsetDateTime lastOpened);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'voltage', to_jsonb(:voltage),
                                'current', to_jsonb(:current),
                                'power', to_jsonb(:power),
                                'energyConsumed', to_jsonb(:energyConsumed)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateEnergyMeterTelemetry(@NonNull @Param("id") UUID id,
                                   @Param("status") String status,
                                   @Param("firmwareVersion") String firmwareVersion,
                                   @Param("updatedAt") OffsetDateTime updatedAt,
                                   @Param("voltage") Float voltage,
                                   @Param("current") Float current,
                                   @Param("power") Float power,
                                   @Param("energyConsumed") Float energyConsumed);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'isOn', to_jsonb(:isOn),
                                'brightness', to_jsonb(:brightness),
                                'colour', to_jsonb(:colour),
                                'mode', to_jsonb(:mode),
                                'powerConsumption', to_jsonb(:powerConsumption)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateSmartLightTelemetry(@NonNull @Param("id") UUID id,
                                  @Param("status") String status,
                                  @Param("firmwareVersion") String firmwareVersion,
                                  @Param("updatedAt") OffsetDateTime updatedAt,
                                  @Param("isOn") Boolean isOn,
                                  @Param("brightness") Integer brightness,
                                  @Param("colour") String colour,
                                  @Param("mode") String mode,
                                  @Param("powerConsumption") Float powerConsumption);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'isOn', to_jsonb(:isOn),
                                'voltage', to_jsonb(:voltage),
                                'current', to_jsonb(:current),
                                'powerUsage', to_jsonb(:powerUsage)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateSmartPlugTelemetry(@NonNull @Param("id") UUID id,
                                 @Param("status") String status,
                                 @Param("firmwareVersion") String firmwareVersion,
                                 @Param("updatedAt") OffsetDateTime updatedAt,
                                 @Param("isOn") Boolean isOn,
                                 @Param("voltage") Float voltage,
                                 @Param("current") Float current,
                                 @Param("powerUsage") Float powerUsage);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'moisturePercentage', to_jsonb(:moisturePercentage),
                                'soilTemperature', to_jsonb(:soilTemperature),
                                'batteryLevel', to_jsonb(:batteryLevel)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateSoilMoistureSensorTelemetry(@NonNull @Param("id") UUID id,
                                          @Param("status") String status,
                                          @Param("firmwareVersion") String firmwareVersion,
                                          @Param("updatedAt") OffsetDateTime updatedAt,
                                          @Param("moisturePercentage") Float moisturePercentage,
                                          @Param("soilTemperature") Float soilTemperature,
                                          @Param("batteryLevel") Integer batteryLevel);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            last_active_at = COALESCE(:lastActiveAt, last_active_at),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'temperature', to_jsonb(:temperature),
                                'humidity', to_jsonb(:humidity),
                                'pressure', to_jsonb(:pressure),
                                'unit', to_jsonb(:unit)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateTemperatureSensorTelemetry(@NonNull @Param("id") UUID id,
                                         @Param("status") String status,
                                         @Param("lastActiveAt") OffsetDateTime lastActiveAt,
                                         @Param("firmwareVersion") String firmwareVersion,
                                         @Param("updatedAt") OffsetDateTime updatedAt,
                                         @Param("temperature") Float temperature,
                                         @Param("humidity") Float humidity,
                                         @Param("pressure") Float pressure,
                                         @Param("unit") String unit);

    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Query(value = """
            UPDATE devices SET
            status = COALESCE(CASE WHEN :status IS NULL THEN NULL ELSE CAST(:status AS VARCHAR) END::device_statuses, status),
            firmware_version = COALESCE(:firmwareVersion, firmware_version),
            updated_at = COALESCE(:updatedAt, updated_at),
            telemetry = (telemetry ||
                        jsonb_strip_nulls(
                            jsonb_build_object(
                                'currentTemperature', to_jsonb(:currentTemperature),
                                'targetTemperature', to_jsonb(:targetTemperature),
                                'humidity', to_jsonb(:humidity),
                                'mode', to_jsonb(:mode)
                            )
                        )
                    )
            WHERE id = :id
            """, nativeQuery = true)
    int updateThermostatTelemetry(@NonNull @Param("id") UUID id,
                                  @Param("status") String status,
                                  @Param("firmwareVersion") String firmwareVersion,
                                  @Param("updatedAt") OffsetDateTime updatedAt,
                                  @Param("currentTemperature") Float currentTemperature,
                                  @Param("targetTemperature") Float targetTemperature,
                                  @Param("humidity") Float humidity,
                                  @Param("mode") String mode);

}
