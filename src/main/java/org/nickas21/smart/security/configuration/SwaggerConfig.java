package org.nickas21.smart.security.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.nickas21.smart.util.StringUtils;
import org.springdoc.core.customizers.RouterOperationCustomizer;
import org.springdoc.core.discoverer.SpringDocParameterNameDiscoverer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Configuration
public class SwaggerConfig {

    @Value("${swagger.api_path:/api/**}")
    private String apiPath;
    @Value("${swagger.title}")
    private String title;

    @Value("${app.version:unknown}")
    String version;

    @Bean
    public GroupedOpenApi publicApi(SpringDocParameterNameDiscoverer localSpringDocParameterNameDiscoverer) {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch(apiPath)
                .addRouterOperationCustomizer(routerOperationCustomizer(localSpringDocParameterNameDiscoverer))
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title(title)
                .version(version);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    private RouterOperationCustomizer routerOperationCustomizer(SpringDocParameterNameDiscoverer localSpringDocParameterNameDiscoverer) {
        return (routerOperation, handlerMethod) -> {
            String[] pNames = localSpringDocParameterNameDiscoverer.getParameterNames(handlerMethod.getMethod());
            String[] reflectionParametersNames = Arrays.stream(handlerMethod.getMethod().getParameters()).map(java.lang.reflect.Parameter::getName).toArray(String[]::new);
            if (pNames == null || Arrays.stream(pNames).anyMatch(Objects::isNull))
                pNames = reflectionParametersNames;
            MethodParameter[] parameters = handlerMethod.getMethodParameters();
            List<String> requestParams = new ArrayList<>();
            for (var i = 0; i < parameters.length; i++) {
                var methodParameter = parameters[i];
                RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
                if (requestParam != null) {
                    String pName = StringUtils.isNotBlank(requestParam.value()) ? requestParam.value() :
                            pNames[i];
                    if (StringUtils.isNotBlank(pName)) {
                        requestParams.add(pName);
                    }
                }
            }
            if (!requestParams.isEmpty()) {
                var path = routerOperation.getPath() + "{?" + String.join(",", requestParams) + "}";
                routerOperation.setPath(path);
            }
            return routerOperation;
        };
    }
}

