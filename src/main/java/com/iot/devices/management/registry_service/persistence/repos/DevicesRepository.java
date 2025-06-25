package com.iot.devices.management.registry_service.persistence.repos;

import com.iot.devices.management.registry_service.persistence.model.Device;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicesRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findBySerialNumber(@NonNull @NotBlank(message = "serial number is required") String serialNumber);
}
