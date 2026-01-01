package com.techforge.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI techForgeOpenAPI() {
        final String securitySchemeName = "X-Requester-ID";

        // Define global header parameter as a security scheme (apiKey in header)
        SecurityScheme scheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(securitySchemeName)
                .description("User identifier forwarded from Desktop client (dev-only). Use this to test RBAC in Swagger UI");

        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, scheme)
                .addParameters("X-Requester-ID-Param", new Parameter()
                        .name("X-Requester-ID")
                        .in("header")
                        .description("Requester userId header for RBAC testing")
                        .required(false)
                );

        OpenAPI openAPI = new OpenAPI()
                .components(components)
                .info(new Info().title("TechForge ERP API").version("1.0").description("API documentation for TechForge System"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));

        return openAPI;
    }

    // Add a global header parameter X-Requester-ID to all operations so Swagger UI can send it
    @Bean
    public OpenApiCustomizer globalHeaderOpenApiCustomiser() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            // Reusable parameter definition
            Parameter requesterParam = new Parameter()
                    .in("header")
                    .name("X-Requester-ID")
                    .description("Requester User ID - set this header to simulate authenticated user (dev/test)")
                    .required(false)
                    .schema(new StringSchema());

            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem.readOperations() == null) return;
                pathItem.readOperations().forEach(operation -> {
                    // avoid duplicate
                    boolean exists = operation.getParameters() != null && operation.getParameters().stream()
                            .anyMatch(p -> "X-Requester-ID".equals(p.getName()));
                    if (!exists) {
                        operation.addParametersItem(requesterParam);
                    }
                });
            });
        };
    }
}
