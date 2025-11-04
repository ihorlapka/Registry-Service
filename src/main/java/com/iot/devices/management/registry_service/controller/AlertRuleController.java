package com.iot.devices.management.registry_service.controller;

import com.iot.devices.management.registry_service.controller.dto.AlertRuleDto;
import com.iot.devices.management.registry_service.controller.util.CreateAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.PatchAlertRuleRequest;
import com.iot.devices.management.registry_service.controller.util.Utils;
import com.iot.devices.management.registry_service.open.api.custom.annotations.alert_rules.*;
import com.iot.devices.management.registry_service.persistence.model.AlertRule;
import com.iot.devices.management.registry_service.persistence.model.User;
import com.iot.devices.management.registry_service.persistence.services.AlertRuleService;
import com.iot.devices.management.registry_service.persistence.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.iot.devices.management.registry_service.controller.util.Utils.*;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/v1/alertRules")
@RequiredArgsConstructor
@Tag(name = "AlertRules", description = "CRUD operations for AlertRules")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final UserService userService;


    @GetMapping("/userRules/{id}")
    @GetAlertRuleByIdOpenApi
    public ResponseEntity<AlertRuleDto> getAlertRuleById(@PathVariable("id") UUID id, Authentication auth) {
        final Optional<User> owner = loadUser(auth.getName());
        if (!hasPermission(auth, owner)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        final AlertRule alertRule = alertRuleService.findAlertRuleById(id);
        return ResponseEntity.ok(mapAlertRuleToDto(alertRule));
    }

    @GetMapping("/myRules")
    @GetUserAlertRulesOpenApi
    public ResponseEntity<List<AlertRuleDto>> getMyAlertRules(Authentication auth) {
        return getUserAlertRules(auth.getName());
    }

    @GetMapping("/userRules/{username}")
    @GetUserAlertRulesOpenApi
    public ResponseEntity<List<AlertRuleDto>> getUserAlertRules(@PathVariable("username") String username) {
        final List<AlertRule> myRules = alertRuleService.findRulesByUsername(username);
        final List<AlertRuleDto> mappedRules = myRules.stream()
                .map(Utils::mapAlertRuleToDto)
                .toList();
        return ResponseEntity.ok(mappedRules);
    }

    @PostMapping
    @CreateAlertRuleOpenApi
    public ResponseEntity<AlertRuleDto> createRule(@RequestBody @Valid CreateAlertRuleRequest request, Authentication auth) {
        final Optional<User> owner = loadUser(request.username());
        if (!hasPermission(auth, owner)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        final AlertRule saved = alertRuleService.saveAndSendMessage(request, owner.orElse(null));
        return ResponseEntity.created(getLocation(saved.getRuleId()))
                .body(mapAlertRuleToDto(saved));
    }

    @PatchMapping
    @UpdateAlertRuleOpenApi
    public ResponseEntity<AlertRuleDto> patchRule(@RequestBody @Valid PatchAlertRuleRequest request, Authentication auth) {
        final Optional<User> owner = loadUser(request.username());
        if (!hasPermission(auth, owner)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        final AlertRule saved = alertRuleService.patchAndSendMessage(request, owner.orElse(null));
        return ResponseEntity.created(getLocation(saved.getRuleId()))
                .body(mapAlertRuleToDto(saved));
    }

    @DeleteMapping("/{ruleId}")
    @RemoveAlertRuleByIdOpenApi
    public ResponseEntity<Void> removeRule(@PathVariable("ruleId") UUID ruleId, Authentication auth) {
        final Optional<User> owner = loadUser(auth.getName());
        if (!hasPermission(auth, owner)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        alertRuleService.removeAndSendTombstone(ruleId);
        return ResponseEntity.noContent().build();
    }

    private Optional<User> loadUser(String username) {
        return ofNullable(username).flatMap(userService::findByUsername);
    }
}
