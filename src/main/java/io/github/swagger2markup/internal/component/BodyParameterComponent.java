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
package io.github.swagger2markup.internal.component;


import io.github.swagger2markup.GroupBy;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.utils.RefUtils;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.PathOperation;
import io.github.swagger2markup.spi.MarkupComponent;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.ResolverCache;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Optional;

import static io.github.swagger2markup.Labels.BODY_PARAMETER;
import static io.github.swagger2markup.Labels.FLAGS_COLUMN;
import static io.github.swagger2markup.Labels.FLAGS_OPTIONAL;
import static io.github.swagger2markup.Labels.FLAGS_REQUIRED;
import static io.github.swagger2markup.Labels.NAME_COLUMN;
import static io.github.swagger2markup.Labels.TYPE_COLUMN;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.copyMarkupDocBuilder;
import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.markupDescription;
import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class BodyParameterComponent extends MarkupComponent<BodyParameterComponent.Parameters> {

    private final DocumentResolver definitionDocumentResolver;
    private final PropertiesTableComponent propertiesTableComponent;

    public BodyParameterComponent(Swagger2MarkupConverter.Context context,
                                  DocumentResolver definitionDocumentResolver) {
        super(context);
        this.definitionDocumentResolver = Validate.notNull(definitionDocumentResolver, "DocumentResolver must not be null");
        this.propertiesTableComponent = new PropertiesTableComponent(context, definitionDocumentResolver);
    }

    public static BodyParameterComponent.Parameters parameters(PathOperation operation,
                                                               List<ObjectType> inlineDefinitions) {
        return new BodyParameterComponent.Parameters(operation, inlineDefinitions);
    }

    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, Parameters params) {
        PathOperation operation = params.operation;
        List<ObjectType> inlineDefinitions = params.inlineDefinitions;
        if (config.isFlatBodyEnabled()) {
            List<Parameter> parameters = operation.getOperation().getParameters();
            if (CollectionUtils.isNotEmpty(parameters)) {
                for (Parameter parameter : parameters) {

                    final ResolverCache resolverCache = new ResolverCache(context.getSwagger(), null, null);
                    final Optional<RequestBody> requestBodyOptional =
                            Optional.ofNullable(resolverCache.loadRef(parameter.get$ref(), computeRefFormat(parameter.get$ref()), RequestBody.class));

                    requestBodyOptional.ifPresent(requestBody -> {
                        buildSectionTitle(markupDocBuilder, labels.getLabel(BODY_PARAMETER));
                        String description = requestBody.getDescription();

                        if (isNotBlank(description)) {
                            markupDocBuilder.paragraph(markupDescription(config.getSwaggerMarkupLanguage(), markupDocBuilder, description));
                        }

                        MarkupDocBuilder typeInfos = copyMarkupDocBuilder(markupDocBuilder);
                        typeInfos.italicText(labels.getLabel(NAME_COLUMN)).textLine(COLON + "body");
                        typeInfos.italicText(labels.getLabel(FLAGS_COLUMN)).textLine(COLON + (BooleanUtils.isTrue(
                                requestBody.getRequired()) ? labels.getLabel(FLAGS_REQUIRED).toLowerCase() : labels.getLabel(FLAGS_OPTIONAL).toLowerCase()));

                        // TODO - radcortez - We need to support multiple MediaTypes
                        final Optional<MediaType> mediaTypeOptional =
                                Optional.ofNullable(requestBody.getContent())
                                        .flatMap(content -> content.values().stream().findFirst());

                        mediaTypeOptional.ifPresent(mediaType -> {
                            final String ref = RefUtils.computeSimpleRef(mediaType.getSchema().get$ref());
                            RefType type = new RefType(definitionDocumentResolver.apply(ref), new ObjectType(ref, null));
                            typeInfos.italicText(labels.getLabel(TYPE_COLUMN)).textLine(COLON + type.displaySchema(markupDocBuilder));
                        });

                        markupDocBuilder.paragraph(typeInfos.toString(), true);

                        // TODO - radcortez - What to do here?
                        /*
                        if (type instanceof ObjectType) {
                            List<ObjectType> localDefinitions = new ArrayList<>();

                            propertiesTableComponent.apply(markupDocBuilder, PropertiesTableComponent.parameters(
                                    ((ObjectType) type).getProperties(),
                                    operation.getId(),
                                    localDefinitions
                                                                                                                ));

                            inlineDefinitions.addAll(localDefinitions);
                        }
                        */
                    });
                }
            }
        }
        return markupDocBuilder;
    }

    private void buildSectionTitle(MarkupDocBuilder markupDocBuilder, String title) {
        if (config.getPathsGroupedBy() == GroupBy.AS_IS) {
            markupDocBuilder.sectionTitleLevel3(title);
        } else {
            markupDocBuilder.sectionTitleLevel4(title);
        }
    }

    public static class Parameters {
        private final List<ObjectType> inlineDefinitions;
        private PathOperation operation;

        public Parameters(PathOperation operation,
                          List<ObjectType> inlineDefinitions) {
            Validate.notNull(operation, "Operation must not be null");
            this.operation = operation;
            this.inlineDefinitions = inlineDefinitions;
        }
    }
}
