package com.semih.ajanofisi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html, with the hook token as an "Authorize" option. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String HOOK_TOKEN_SCHEME = "hookToken";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ajan Ofisi API")
                        .description("Claude Code hook events in, live office events out")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(HOOK_TOKEN_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Hook-Token")));
    }
}
