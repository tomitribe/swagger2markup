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

import io.github.swagger2markup.Labels;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.spi.MarkupComponent;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.Validate;

import java.util.List;

import static io.github.swagger2markup.internal.utils.MarkupDocBuilderUtils.copyMarkupDocBuilder;
import static org.apache.commons.lang3.StringUtils.join;

public class UriSchemeComponent extends MarkupComponent<UriSchemeComponent.Parameters> {


    public UriSchemeComponent(Swagger2MarkupConverter.Context context) {
        super(context);
    }

    public static UriSchemeComponent.Parameters parameters(OpenAPI swagger, int titleLevel) {
        return new UriSchemeComponent.Parameters(swagger, titleLevel);
    }

    @Override
    public MarkupDocBuilder apply(MarkupDocBuilder markupDocBuilder, Parameters params) {
        OpenAPI swagger = params.swagger;
        List<Server> servers = swagger.getServers();

        markupDocBuilder.sectionTitleLevel(params.titleLevel, labels.getLabel(Labels.URI_SCHEME));
        MarkupDocBuilder paragraphBuilder = copyMarkupDocBuilder(markupDocBuilder);
        for (Server server : servers) {
            paragraphBuilder.italicText(labels.getLabel(Labels.SERVER)).textLine(COLON + server.getUrl());
        }

        markupDocBuilder.paragraph(paragraphBuilder.toString(), true);

        return markupDocBuilder;
    }

    public static class Parameters {

        private final int titleLevel;
        private final OpenAPI swagger;

        public Parameters(OpenAPI swagger, int titleLevel) {

            this.swagger = Validate.notNull(swagger);
            this.titleLevel = titleLevel;
        }
    }


}
