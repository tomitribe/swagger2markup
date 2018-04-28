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

package io.github.swagger2markup.internal.type;

import io.github.swagger2markup.markup.builder.MarkupDocBuilder;

/**
 * Reference to a type defined elsewhere
 */
public class RefType extends Type {

    private String document;
    private Type refType;

    public RefType(String document, Type refType) {
        super(null);
        this.document = document;
        this.refType = refType;
    }

    public RefType(Type refType) {
        this(null, refType);
    }

    @Override
    public String displaySchema(MarkupDocBuilder docBuilder) {
        if (refType instanceof ComposedType) {
            return refType.displaySchema(docBuilder);
        }

        return docBuilder.copy(false).crossReference(getDocument(), refType.getUniqueName(), refType.getUniqueName()).toString();
    }

    @Override
    public String getName() {
        return refType.getName();
    }

    @Override
    public String getUniqueName() {
        return refType.getUniqueName();
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public Type getRefType() {
        return refType;
    }

    public void setRefType(Type refType) {
        this.refType = refType;
    }
}
