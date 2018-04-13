package io.github.swagger2markup;

import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Check {@link io.swagger.v3.parser.converter.SwaggerConverter}
 */
public class OpenAPI2SwaggerConverter {
    public static Swagger convert(final OpenAPI openAPI) {
        final Swagger swagger = new Swagger();
        swagger.setInfo(convertInfo(openAPI.getInfo()));
        return swagger;
    }

    private static Info convertInfo(final io.swagger.v3.oas.models.info.Info openApiInfo) {
        final Info info = new Info();
        info.setContact(null);
        info.setDescription(openApiInfo.getDescription());
        info.setLicense(null);
        info.setTermsOfService(openApiInfo.getTermsOfService());
        info.setTitle(openApiInfo.getTitle());
        info.setVersion(openApiInfo.getVersion());
        info.setVendorExtensions(null);
        return info;
    }
}
