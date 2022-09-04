package org.bf2.cos.connector.camel.salesforce;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.CamelContext;
import org.apache.camel.component.extension.ComponentVerifierExtension;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("verifier")
@Readiness
@ApplicationScoped
public class SalesforceHealthCheck implements HealthCheck {
    private static final String COMPONENT_SCHEME = "salesforce";
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceHealthCheck.class);

    @Inject
    CamelContext context;

    @ConfigProperty(name = "sf.client.id")
    String clientId;
    @ConfigProperty(name = "sf.client.secret")
    String clientSecret;
    @ConfigProperty(name = "sf.client.usr")
    String userName;
    @ConfigProperty(name = "sf.client.pwd")
    String password;
    @ConfigProperty(name = "sf.login.url", defaultValue = "https://login.salesforce.com")
    String loginUrl;

    @Override
    public HealthCheckResponse call() {
        return context.getComponent(COMPONENT_SCHEME, true, false)
                .getExtension(ComponentVerifierExtension.class)
                .map(this::compute)
                .orElseGet(this::unimplemented);
    }

    private HealthCheckResponse unimplemented() {
        return HealthCheckResponse.up("verifier");
    }

    private HealthCheckResponse compute(ComponentVerifierExtension extension) {
        ComponentVerifierExtension.Result result = extension.verify(ComponentVerifierExtension.Scope.CONNECTIVITY, Map.of(
                "clientId", clientId,
                "clientSecret", clientSecret,
                "userName", userName,
                "password", password,
                "loginUrl", loginUrl));

        LOGGER.info("{}", result);

        HealthCheckResponseBuilder builder = HealthCheckResponse.builder().name("verifier");

        if (result.getStatus() == ComponentVerifierExtension.Result.Status.OK) {
            builder.up();
        } else {
            List<ComponentVerifierExtension.VerificationError> errors = result.getErrors();

            for (int i = 0; i < errors.size(); i++) {
                builder.withData(
                        "error." + i + ".code",
                        errors.get(i).getCode().getName());
                builder.withData(
                        "error." + i + ".description",
                        errors.get(i).getDescription());

                for (var entry : errors.get(i).getDetails().entrySet()) {
                    builder.withData(
                            "error." + i + ".details." + entry.getKey().getName(),
                            Objects.toString(entry.getValue()));
                }
            }

            builder.down();
        }

        return builder.build();
    }
}
