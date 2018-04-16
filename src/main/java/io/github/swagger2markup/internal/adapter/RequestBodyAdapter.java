package io.github.swagger2markup.internal.adapter;

import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.type.Type;
import io.github.swagger2markup.internal.utils.InlineSchemaUtils;
import io.github.swagger2markup.internal.utils.ModelUtils;
import io.github.swagger2markup.internal.utils.RefUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.boldText;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.literalText;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;

public class RequestBodyAdapter {
    private final RequestBody body;
    private final Schema schema;
    private final List<ObjectType> inlineDefinitions = new ArrayList<>();
    private final Swagger2MarkupConverter.Context context;
    private final Swagger2MarkupConfig config;
    private Type type;

    public RequestBodyAdapter(Swagger2MarkupConverter.Context context,
                              PathOperation operation,
                              RequestBody body,
                              DocumentResolver definitionDocumentResolver) {

        Validate.notNull(body, "parameter must not be null");
        this.body = body;
        this.context = context;
        this.config = context.getConfig();

        this.schema = body.getContent()
                          .values()
                          .stream()
                          .findFirst()
                          .map(MediaType::getSchema)
                          .orElseThrow(IllegalArgumentException::new);
        this.schema.setTitle("body");
        this.type = getType(definitionDocumentResolver);
        if (config.isInlineSchemaEnabled()) {
            type = InlineSchemaUtils.createInlineType(type, "body", "body", inlineDefinitions);
        }
    }

    public String getName() {
        return "body";
    }

    public boolean getRequired() {
        return Optional.ofNullable(body.getRequired()).orElse(false);
    }

    public String displayType(final MarkupDocBuilder markupDocBuilder) {
        return boldText(markupDocBuilder, "Body");
    }

    public String displayDescription(final MarkupDocBuilder markupDocBuilder) {
        return markupDescription(config.getSwaggerMarkupLanguage(), markupDocBuilder, body.getDescription());
    }

    public String displaySchema(final MarkupDocBuilder markupDocBuilder) {
        return type.displaySchema(markupDocBuilder);
    }

    public String displayDefaultValue(final MarkupDocBuilder markupDocBuilder) {
        return getDefaultValue().map(value -> literalText(markupDocBuilder, Json.pretty(value))).orElse("");
    }

    public List<ObjectType> getInlineDefinitions() {
        return inlineDefinitions;
    }

    public Type getType(DocumentResolver definitionDocumentResolver) {
        if (schema.get$ref() != null) {
            String refName = RefUtils.computeSimpleRef(schema.get$ref());
            return type = new RefType(definitionDocumentResolver.apply(refName), new ObjectType(refName, null));
        } else {
            return ModelUtils.getType(schema, context.getSwagger().getComponents().getSchemas(),
                                      definitionDocumentResolver);
        }

    }

    private Optional<Object> getDefaultValue() {
        return Optional.empty();
    }
}
