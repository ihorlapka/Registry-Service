package com.iot.devices.management.registry_service.persistence.model;

import com.iot.devices.management.registry_service.persistence.model.enums.DeviceManufacturer;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceStatus;
import com.iot.devices.management.registry_service.persistence.model.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static jakarta.persistence.GenerationType.AUTO;

@Getter
@Setter
@Entity
@Table(name = "devices")
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "serial_number", unique = true, nullable = false, length = 100)
    private String serialNumber;

    @Column(name = "manufacturer", columnDefinition = "device_manufacturer", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeviceManufacturer deviceManufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "device_type", columnDefinition = "device_types", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeviceType deviceType;

    @Column(name = "location_description")
    private String location;

    @Column(name = "latitude", length = 15)
    private BigDecimal latitude;

    @Column(name = "longitude", length = 15)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "status", columnDefinition = "device_statuses", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeviceStatus status;

    @Column(name = "last_active_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastActiveAt;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @Column(columnDefinition = "jsonb")
    private String telemetry;
}
