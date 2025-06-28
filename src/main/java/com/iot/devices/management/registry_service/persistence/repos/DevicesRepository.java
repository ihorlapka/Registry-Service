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
    @Query("UPDATE Device d SET d.status = :newStatus WHERE d.id = :id AND d.status <> :newStatus")
    int updateDeviceStatus(@NonNull @Param("id") UUID id, @NonNull @Param("status") DeviceStatus newStatus);
}
