package com.chapchap.delivery.domain.delivery.response;

import java.time.OffsetDateTime;

public record DeliveryPhotoAccessResponse(String accessUrl, OffsetDateTime expiresAt) { }
