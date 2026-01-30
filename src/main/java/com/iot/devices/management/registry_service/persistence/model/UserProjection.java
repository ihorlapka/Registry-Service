package com.iot.devices.management.registry_service.persistence.model;

import com.iot.devices.management.registry_service.persistence.model.enums.UserRole;
import lombok.*;

import java.util.UUID;


@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class UserProjection implements UserBase {
    private final UUID id;
    private final String username;
    private final UserRole userRole;
}
