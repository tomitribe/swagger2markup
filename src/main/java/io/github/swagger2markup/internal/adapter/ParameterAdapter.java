/*
 * Copyright 2017 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.swagger2markup.internal.adapter;

import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ArrayType;
import io.github.swagger2markup.internal.type.BasicType;
import io.github.swagger2markup.internal.type.EnumType;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.type.Type;
import io.github.swagger2markup.internal.utils.InlineSchemaUtils;
import io.github.swagger2markup.internal.utils.RefUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.boldText;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.literalText;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;
import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;

public class ParameterAdapter {

    private final Parameter parameter;
    private final List<ObjectType> inlineDefinitions = new ArrayList<>();
    private final Swagger2MarkupConfig config;
    private Type type;

    public ParameterAdapter(Swagger2MarkupConverter.Context context,
                            PathOperation operation,
                            Parameter parameter,
                            DocumentResolver definitionDocumentResolver) {
        Validate.notNull(parameter, "parameter must not be null");
        this.parameter = parameter;

        if (parameter.get$ref() != null) {
            Optional.ofNullable(context.getCache()
                                       .loadRef(parameter.get$ref(), computeRefFormat(parameter.get$ref()),
                                                Object.class))
                    .filter(o -> o instanceof RequestBody)
                    .map(o -> ((RequestBody) o))
                    .ifPresent(body -> {
                        parameter.setIn("Body");
                        parameter.setName("body");
                        parameter.setDescription(body.getDescription());
                    });
        }

        type = getType(definitionDocumentResolver);
        config = context.getConfig();
        if (config.isInlineSchemaEnabled()) {
            if (config.isFlatBodyEnabled()) {
                if (!(type instanceof ObjectType)) {
                    type = InlineSchemaUtils.createInlineType(type, parameter.getName(), operation.getId() + " " + parameter.getName(), inlineDefinitions);
                }
            } else {
                type = InlineSchemaUtils.createInlineType(type, parameter.getName(), operation.getId() + " " + parameter.getName(), inlineDefinitions);
            }
        }
    }

    /**
     * Generate a default example value for parameter.
     *
     * @param parameter parameter
     * @return a generated example for the parameter
     */
    public static Object generateExample(AbstractSerializableParameter parameter) {
        switch (parameter.getType()) {
            case "integer":
                return 0;
            case "number":
                return 0.0;
            case "boolean":
                return true;
            case "string":
                return "string";
            default:
                return parameter.getType();
        }
    }

    public String getName() {
        return parameter.getName();
    }

    public String getUniqueName() {
        return type.getUniqueName();
    }

    public String displaySchema(MarkupDocBuilder docBuilder) {
        return type.displaySchema(docBuilder);
    }

    public String displayDefaultValue(MarkupDocBuilder docBuilder) {
        return getDefaultValue().map(value -> literalText(docBuilder, Json.pretty(value))).orElse("");
    }

    public String displayDescription(MarkupDocBuilder markupDocBuilder) {
        return markupDescription(config.getSwaggerMarkupLanguage(), markupDocBuilder, getDescription());
    }

    public String displayType(MarkupDocBuilder markupDocBuilder) {
        return boldText(markupDocBuilder, getIn());
    }

    public String getDescription() {
        return parameter.getDescription();
    }

    public boolean getRequired() {
        return Optional.ofNullable(parameter.getRequired()).orElse(false);
    }

    public Map<String, Object> getVendorExtensions() {
        return parameter.getExtensions();
    }

    public String getIn() {
        return WordUtils.capitalize(parameter.getIn());
    }

    public Type getType() {
        return type;
    }

    public List<ObjectType> getInlineDefinitions() {
        return inlineDefinitions;
    }

    /**
     * Retrieves the type of a parameter, or otherwise null
     *
     * @param definitionDocumentResolver the definition document resolver
     * @return the type of the parameter, or otherwise null
     */
    private Type getType(DocumentResolver definitionDocumentResolver) {
        Validate.notNull(parameter, "parameter must not be null!");
        Type type;

        if (parameter.get$ref() != null) {
            String refName = RefUtils.computeSimpleRef(parameter.get$ref());

            type = new RefType(definitionDocumentResolver.apply(refName), new ObjectType(refName, null));
        } else {
            Schema serializableParameter = parameter.getSchema();
            @SuppressWarnings("unchecked")
            List<String> enums = serializableParameter.getEnum();

            if (CollectionUtils.isNotEmpty(enums)) {
                type = new EnumType(parameter.getName(), enums);
            } else {
                type = new BasicType(serializableParameter.getType(), parameter.getName(), serializableParameter.getFormat());
            }
            if (serializableParameter.getType().equals("array")) {
                String collectionFormat = serializableParameter.getFormat();

                type = new ArrayType(parameter.getName(), new PropertyAdapter(serializableParameter).getType(definitionDocumentResolver), collectionFormat);
            }
        }

        return type;
    }

    /**
     * Retrieves the default value of a parameter
     *
     * @return the default value of the parameter
     */
    public Optional<Object> getDefaultValue() {
        Validate.notNull(parameter, "parameter must not be null!");
        if (parameter.get$ref() == null) {
            return Optional.ofNullable(parameter.getSchema()).map(Schema::getDefault);
        }
        return Optional.empty();
    }
}
