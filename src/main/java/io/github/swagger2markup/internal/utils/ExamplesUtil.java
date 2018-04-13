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

package io.github.swagger2markup.internal.utils;

import io.github.swagger2markup.internal.adapter.ParameterAdapter;
import io.github.swagger2markup.internal.adapter.PropertyAdapter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.model.PathOperation;
import io.swagger.models.ArrayModel;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExamplesUtil {

    private static final Integer MAX_RECURSION_TO_DISPLAY = 2;

    /**
     * Generates a Map of response examples
     *
     * @param generateMissingExamples specifies the missing examples should be generated
     * @param operation               the Swagger Operation
     * @param definitions             the map of definitions
     * @param markupDocBuilder        the markup builder
     * @return map containing response examples.
     */
    public static Map<String, Object> generateResponseExampleMap(boolean generateMissingExamples, PathOperation operation, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder) {
        Map<String, Object> examples = new LinkedHashMap<>();
        Map<String, ApiResponse> responses = operation.getOperation().getResponses();
        if (responses != null)
            for (Map.Entry<String, ApiResponse> responseEntry : responses.entrySet()) {
                ApiResponse response = responseEntry.getValue();
                final Optional<MediaType> mediaTypeOptional =
                        Optional.ofNullable(response.getContent())
                                .flatMap(content -> content.values().stream().findFirst());

                Object example = null;
                if (mediaTypeOptional.isPresent()) {
                    final MediaType mediaType = mediaTypeOptional.get();
                    example = mediaType.getExample();

                        /*
                        if (example == null && schema instanceof RefProperty) {
                            String simpleRef = ((RefProperty) schema).getSimpleRef();
                            example = generateExampleForRefModel(generateMissingExamples, simpleRef, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                        }
                        if (example == null && schema instanceof ArrayProperty && generateMissingExamples) {
                            example = generateExampleForArrayProperty((ArrayProperty) schema, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                        }
                        if (example == null && generateMissingExamples) {
                            example = PropertyAdapter.generateExample(schema, markupDocBuilder);
                        }
                        */
                }

                if (example != null)
                    examples.put(responseEntry.getKey(), example);

            }

        return examples;
    }

    /**
     * Generates examples for request
     *
     * @param generateMissingExamples specifies the missing examples should be generated
     * @param pathOperation           the Swagger Operation
     * @param definitions             the map of definitions
     * @param markupDocBuilder        the markup builder
     * @return an Optional with the example content
     */
    public static Map<String, Object> generateRequestExampleMap(boolean generateMissingExamples, PathOperation pathOperation, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder) {
        Operation operation = pathOperation.getOperation();
        List<Parameter> parameters = operation.getParameters();
        Map<String, Object> examples = new LinkedHashMap<>();

        // Path example should always be included (if generateMissingExamples):
        if (generateMissingExamples)
            examples.put("path", pathOperation.getPath());
        for (Parameter parameter : parameters) {
            Object example = null;
            if (parameter.get$ref() == null) {
                if (generateMissingExamples) {
                    Object abstractSerializableParameterExample;
                    abstractSerializableParameterExample = parameter.getExample();
                    if (abstractSerializableParameterExample == null) {
                        abstractSerializableParameterExample = parameter.getExtensions().get("x-example");
                    }
                    if (abstractSerializableParameterExample == null) {
                        final Schema item = parameter.getSchema();
                        if (item != null) {
                            abstractSerializableParameterExample = item.getExample();
                            if (abstractSerializableParameterExample == null) {
                                abstractSerializableParameterExample = PropertyAdapter.generateExample(item, markupDocBuilder);
                            }
                        }
                        /*
                        if (abstractSerializableParameterExample == null) {
                            abstractSerializableParameterExample = ParameterAdapter.generateExample(parameter);
                        }
                        */
                    }
                    if (parameter instanceof PathParameter) {
                        String pathExample = (String) examples.get("path");
                        pathExample = pathExample.replace('{' + parameter.getName() + '}', String.valueOf(abstractSerializableParameterExample));
                        example = pathExample;
                    } else {
                        example = abstractSerializableParameterExample;
                    }
                    if (parameter instanceof QueryParameter) {
                        //noinspection unchecked
                        @SuppressWarnings("unchecked")
                        Map<String, Object> queryExampleMap = (Map<String, Object>) examples.get("query");
                        if (queryExampleMap == null) {
                            queryExampleMap = new LinkedHashMap<>();
                        }
                        queryExampleMap.put(parameter.getName(), abstractSerializableParameterExample);
                        example = queryExampleMap;
                    }
                }
            } else if (parameter.get$ref() != null) {
                String simpleRef = parameter.get$ref();
                example = generateExampleForRefModel(generateMissingExamples, simpleRef, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
            }

            final RequestBody requestBody = pathOperation.getOperation().getRequestBody();
            final Optional<MediaType> mediaTypeOptional =
                    Optional.ofNullable(requestBody.getContent())
                            .flatMap(content -> content.values().stream().findFirst());

            if (mediaTypeOptional.isPresent()) {
                final Schema schema = mediaTypeOptional.get().getSchema();
                if (schema.get$ref() != null) {
                    String simpleRef = schema.get$ref();
                    example = generateExampleForRefModel(generateMissingExamples, simpleRef, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                } else if (generateMissingExamples) {
                    if (schema instanceof ComposedSchema) {
                        //FIXME: getProperties() may throw NullPointerException
                        example = exampleMapForProperties(((ObjectType) ModelUtils.getType(schema, definitions, definitionDocumentResolver)).getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                    } else if (schema instanceof ArraySchema) {
                        example = generateExampleForArrayModel((ArraySchema) schema, definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                    } else {
                        example = schema.getExample();
                        if (example == null) {
                            example = exampleMapForProperties(schema.getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                        }
                    }
                }
            }

            if (example != null)
                examples.put(parameter.getIn(), example);
        }

        return examples;
    }

    /**
     * Generates an example object from a simple reference
     *
     * @param generateMissingExamples specifies the missing examples should be generated
     * @param simpleRef               the simple reference string
     * @param definitions             the map of definitions
     * @param markupDocBuilder        the markup builder
     * @param refStack                map to detect cyclic references
     * @return returns an Object or Map of examples
     */
    private static Object generateExampleForRefModel(boolean generateMissingExamples, String simpleRef, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        Schema model = definitions.get(simpleRef);
        Object example = null;
        if (model != null) {
            example = model.getExample();
            if (example == null && generateMissingExamples) {
                if (!refStack.containsKey(simpleRef)) {
                    refStack.put(simpleRef, 1);
                } else {
                    refStack.put(simpleRef, refStack.get(simpleRef) + 1);
                }
                if (refStack.get(simpleRef) <= MAX_RECURSION_TO_DISPLAY) {
                    if (model instanceof ComposedSchema) {
                        //FIXME: getProperties() may throw NullPointerException
                        example = exampleMapForProperties(((ObjectType) ModelUtils.getType(model, definitions, definitionDocumentResolver)).getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, new HashMap<>());
                    } else {
                        example = exampleMapForProperties(model.getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, refStack);
                    }
                } else {
                    return "...";
                }
                refStack.put(simpleRef, refStack.get(simpleRef) - 1);
            }
        }
        return example;
    }

    /**
     * Generates a map of examples from a map of properties. If defined examples are found, those are used. Otherwise,
     * examples are generated from the type.
     *
     * @param properties       the map of properties
     * @param definitions      the map of definitions
     * @param markupDocBuilder the markup builder
     * @param refStack         map to detect cyclic references
     * @return a Map of examples
     */
    private static Map<String, Object> exampleMapForProperties(Map<String, Schema> properties, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        Map<String, Object> exampleMap = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, Schema> property : properties.entrySet()) {
                Object exampleObject = property.getValue().getExample();
                if (exampleObject == null) {
                    if (property.getValue().get$ref() != null) {
                        exampleObject = generateExampleForRefModel(true, property.getValue().get$ref(), definitions, definitionDocumentResolver, markupDocBuilder, refStack);
                    } else if (property.getValue() instanceof ArraySchema) {
                        exampleObject = generateExampleForArrayProperty((ArraySchema) property.getValue(), definitions, definitionDocumentResolver, markupDocBuilder, refStack);
                    } else if (property.getValue() instanceof MapSchema) {
                        exampleObject = generateExampleForMapProperty((MapSchema) property.getValue(), markupDocBuilder);
                    }
                    if (exampleObject == null) {
                        Schema valueProperty = property.getValue();
                        exampleObject = PropertyAdapter.generateExample(valueProperty, markupDocBuilder);
                    }
                }
                exampleMap.put(property.getKey(), exampleObject);
            }
        }
        return exampleMap;
    }

    private static Object generateExampleForMapProperty(MapSchema property, MarkupDocBuilder markupDocBuilder) {
        if (property.getExample() != null) {
            return property.getExample();
        }
        Map<String, Object> exampleMap = new LinkedHashMap<>();
        Schema valueProperty = (Schema) property.getAdditionalProperties();
        if (valueProperty.getExample() != null) {
            return valueProperty.getExample();
        }
        exampleMap.put("string", PropertyAdapter.generateExample(valueProperty, markupDocBuilder));
        return exampleMap;
    }

    private static Object generateExampleForArrayModel(ArraySchema model, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        if (model.getExample() != null) {
            return model.getExample();
        } else if (model.getProperties() != null) {
            return new Object[]{exampleMapForProperties(model.getProperties(), definitions, definitionDocumentResolver, markupDocBuilder, refStack)};
        } else {
            Schema itemProperty = model.getItems();
            return getExample(itemProperty, definitions, definitionDocumentResolver, markupDocBuilder, refStack);
        }
    }

    /**
     * Generates examples from an ArrayProperty
     *
     * @param value            ArrayProperty
     * @param definitions      map of definitions
     * @param markupDocBuilder the markup builder
     * @return array of Object
     */
    private static Object[] generateExampleForArrayProperty(ArraySchema value, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver, MarkupDocBuilder markupDocBuilder, Map<String, Integer> refStack) {
        final Schema property = value.getItems();
        return getExample(property, definitions, definitionDocumentResolver, markupDocBuilder, refStack);
    }

    /**
     * Get example from a property
     *
     * @param property                   Property
     * @param definitions                map of definitions
     * @param definitionDocumentResolver DocumentResolver
     * @param markupDocBuilder           the markup builder
     * @param refStack                   reference stack
     * @return array of Object
     */
    private static Object[] getExample(
            Schema property,
            Map<String, Schema> definitions,
            DocumentResolver definitionDocumentResolver,
            MarkupDocBuilder markupDocBuilder,
            Map<String, Integer> refStack) {
        if (property.getExample() != null) {
            return new Object[]{property.getExample()};
        } else if (property instanceof ArraySchema) {
            return new Object[]{generateExampleForArrayProperty((ArraySchema) property, definitions, definitionDocumentResolver, markupDocBuilder, refStack)};
        } else if (property.get$ref() != null) {
            return new Object[]{generateExampleForRefModel(true, property.get$ref(), definitions, definitionDocumentResolver, markupDocBuilder, refStack)};
        } else {
            return new Object[]{PropertyAdapter.generateExample(property, markupDocBuilder)};
        }
    }

    //TODO: Unused method, make sure this is never used and then remove it.
    //FIXME: getProperties() may throw NullPointerException

}
