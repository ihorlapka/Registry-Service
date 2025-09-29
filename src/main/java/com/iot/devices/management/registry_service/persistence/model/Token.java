package com.iot.devices.management.registry_service.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import static jakarta.persistence.GenerationType.AUTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tokens")
@ToString(exclude = "user")
@Entity
public class Token {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true)
    public String token;

    public boolean revoked;

    public boolean expired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;
}
