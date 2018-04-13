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

import com.google.common.collect.ImmutableMap;
import io.github.swagger2markup.internal.adapter.PropertyAdapter;
import io.github.swagger2markup.internal.resolver.DocumentResolver;
import io.github.swagger2markup.internal.type.ArrayType;
import io.github.swagger2markup.internal.type.BasicType;
import io.github.swagger2markup.internal.type.EnumType;
import io.github.swagger2markup.internal.type.MapType;
import io.github.swagger2markup.internal.type.ObjectType;
import io.github.swagger2markup.internal.type.ObjectTypePolymorphism;
import io.github.swagger2markup.internal.type.RefType;
import io.github.swagger2markup.internal.type.Type;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.Validate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class ModelUtils {

    /**
     * Recursively resolve referenced type if {@code type} is of type RefType
     *
     * @param type type to resolve
     * @return referenced type
     */
    public static Type resolveRefType(Type type) {
        if (type == null)
            return null;

        if (type instanceof RefType)
            return resolveRefType(((RefType) type).getRefType());
        else
            return type;
    }

    /**
     * Retrieves the type of a model, or otherwise null
     *
     * @param model                      the model
     * @param definitionDocumentResolver the definition document resolver
     * @return the type of the model, or otherwise null
     */
    public static Type getType(Schema model, Map<String, Schema> definitions, DocumentResolver definitionDocumentResolver) {
        Validate.notNull(model, "model must not be null!");
        if (model instanceof ComposedSchema) {
            ComposedSchema composedModel = (ComposedSchema) model;
            Map<String, Schema> allProperties = new LinkedHashMap<>();
            ObjectTypePolymorphism polymorphism = new ObjectTypePolymorphism(ObjectTypePolymorphism.Nature.NONE, null);
            String name = model.getTitle();

            if (composedModel.getAllOf() != null) {
                polymorphism.setNature(ObjectTypePolymorphism.Nature.COMPOSITION);

                for (Schema innerModel : composedModel.getAllOf()) {
                    Type innerModelType = resolveRefType(getType(innerModel, definitions, definitionDocumentResolver));
                    name = innerModelType.getName();

                    if (innerModelType instanceof ObjectType) {

                        String innerModelDiscriminator = ((ObjectType) innerModelType).getPolymorphism().getDiscriminator();
                        if (innerModelDiscriminator != null) {
                            polymorphism.setNature(ObjectTypePolymorphism.Nature.INHERITANCE);
                            polymorphism.setDiscriminator(innerModelDiscriminator);
                        }

                        Map<String, Schema> innerModelProperties = ((ObjectType) innerModelType).getProperties();
                        if (innerModelProperties != null)
                            allProperties.putAll(ImmutableMap.copyOf(innerModelProperties));
                    }
                }
            }

            return new ObjectType(name, polymorphism, allProperties);
        } else if (model.get$ref() != null) {
            String refName = model.get$ref();

            Type refType = new ObjectType(refName, null);
            if (definitions.containsKey(refName)) {
                refType = getType(definitions.get(refName), definitions, definitionDocumentResolver);
                refType.setName(refName);
                refType.setUniqueName(refName);
            }

            return new RefType(definitionDocumentResolver.apply(refName), refType);
        } else if (model instanceof ArraySchema) {
            ArraySchema arrayModel = ((ArraySchema) model);

            return new ArrayType(null, new PropertyAdapter(arrayModel.getItems()).getType(definitionDocumentResolver));
        } else {
            Schema modelImpl = model;

            if (modelImpl.getAdditionalProperties() != null)
                return new MapType(modelImpl.getTitle(), new PropertyAdapter((Schema) modelImpl.getAdditionalProperties()).getType(definitionDocumentResolver));
            else if (modelImpl.getEnum() != null)
                return new EnumType(modelImpl.getTitle(), modelImpl.getEnum());
            else if (modelImpl.getProperties() != null) {
                ObjectType objectType = new ObjectType(modelImpl.getTitle(), model.getProperties());

                objectType.getPolymorphism().setDiscriminator(modelImpl.getDiscriminator().getPropertyName());

                return objectType;
            } else if (isNotBlank(modelImpl.getFormat()))
                return new BasicType(modelImpl.getType(), modelImpl.getTitle(), modelImpl.getFormat());
            else
                return new BasicType(modelImpl.getType(), modelImpl.getTitle());
        }
    }
}
