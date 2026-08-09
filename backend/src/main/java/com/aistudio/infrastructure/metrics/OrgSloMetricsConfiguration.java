package com.aistudio.infrastructure.metrics;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

@Configuration
public class OrgSloMetricsConfiguration {

    @Bean
    ServerRequestObservationConvention orgSloServerRequestObservationConvention() {
        return new DefaultServerRequestObservationConvention() {
            @Override
            public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
                KeyValues keyValues = super.getLowCardinalityKeyValues(context);
                String organizationId = OrgSloContext.organizationId();
                if (organizationId != null) {
                    return keyValues.and(KeyValue.of("organization_id", organizationId));
                }
                return keyValues;
            }
        };
    }
}
