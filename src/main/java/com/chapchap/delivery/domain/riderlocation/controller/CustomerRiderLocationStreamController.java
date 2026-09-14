package com.chapchap.delivery.domain.riderlocation.controller;

import com.chapchap.delivery.domain.riderlocation.service.CustomerRiderLocationStreamService;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/customer/deliveries")
public class CustomerRiderLocationStreamController {
    private final CustomerRiderLocationStreamService streamService;

    @GetMapping(value = "/{deliveryId}/rider-location/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String deliveryId)
        throws IOException {
        return streamService.open(user.userId(), user.role(), deliveryId);
    }
}
