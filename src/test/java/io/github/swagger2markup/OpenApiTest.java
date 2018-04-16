package io.github.swagger2markup;

import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class OpenApiTest {
    @Test
    public void openApi() throws Exception {
        final URL resource = OpenApiTest.class.getClassLoader().getResource("yaml/openApi.yaml");
        assertNotNull(resource);

        final Swagger2MarkupConfig config =
                new Swagger2MarkupConfigBuilder()
                        .withMarkupLanguage(MarkupLanguage.ASCIIDOC)
                        .withSwaggerMarkupLanguage(MarkupLanguage.ASCIIDOC)
                        .withPathsGroupedBy(GroupBy.TAGS)
                        .withGeneratedExamples()
                        .withBasePathPrefix()
                        .withAnchorPrefix("api_")
                        .build();

        final Swagger2MarkupConverter converter =
                Swagger2MarkupConverter.from(resource)
                                       .withConfig(config)
                                       .build();

        System.out.println(converter.toString());
    }
}
