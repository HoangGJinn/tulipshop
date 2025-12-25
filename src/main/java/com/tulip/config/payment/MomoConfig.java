package com.tulip.config.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class MomoConfig {
    @Value("${momo.partner.code}")
    private String partnerCode;

    @Value("${momo.return.url}")
    private String returnUrl;

    @Value("${momo.endpoint.url}")
    private String endpointUrl;

    @Value("${momo.ipn.url}")
    private String ipnUrl;

    @Value("${momo.access.key}")
    private String accessKey;

    @Value("${momo.secret.key}")
    private String secretKey;

    @Value("${momo.request.type}")
    private String requestType;
}

