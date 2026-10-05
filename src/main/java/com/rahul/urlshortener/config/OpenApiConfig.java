package com.rahul.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI metadata: the X-API-Key scheme ("Authorize" button) applied to every mutating operation. */
@Configuration
public class OpenApiConfig {

    static final String SCHEME = "ApiKeyAuth";

    @Bean
    OpenAPI shortenerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("URL Shortener API").version("1.0.0")
                        .description("Click Authorize and enter the API key (X-API-Key) to call mutating endpoints."))
                .components(new Components().addSecuritySchemes(SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-API-Key")));
    }

    @Bean
    OpenApiCustomizer requireApiKeyOnMutatingOperations() {
        return openApi -> openApi.getPaths().values().forEach(item -> {
            for (var op : new io.swagger.v3.oas.models.Operation[]{item.getPost(), item.getPut(), item.getPatch(), item.getDelete()}) {
                if (op != null) {
                    op.addSecurityItem(new SecurityRequirement().addList(SCHEME));
                }
            }
        });
    }
}
