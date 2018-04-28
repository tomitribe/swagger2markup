package io.github.swagger2markup.internal.type;

import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ComposedType extends Type {
    private List<Type> allOf;
    private List<Type> anyOf;
    private List<Type> oneOf;

    private Map<String, Schema> allOfSchemas;
    private Map<String, Schema> anyOfSchemas;
    private Map<String, Schema> oneOfSchemas;

    public ComposedType(final String name) {
        super(name);
        allOf = new ArrayList<>();
        anyOf = new ArrayList<>();
        oneOf = new ArrayList<>();
        allOfSchemas = new HashMap<>();
        anyOfSchemas = new HashMap<>();
        oneOfSchemas = new HashMap<>();
    }

    public ComposedType(final String name,
                        final List<Type> allOf,
                        final List<Type> anyOf,
                        final List<Type> oneOf,
                        final Map<String, Schema> allOfSchemas,
                        final Map<String, Schema> anyOfSchemas,
                        final Map<String, Schema> oneOfSchemas) {
        super(name);
        this.allOf = allOf;
        this.anyOf = anyOf;
        this.oneOf = oneOf;
        this.allOfSchemas = allOfSchemas;
        this.anyOfSchemas = anyOfSchemas;
        this.oneOfSchemas = oneOfSchemas;
    }

    public List<Type> getAllOf() {
        return allOf;
    }

    public List<Type> getAnyOf() {
        return anyOf;
    }

    public List<Type> getOneOf() {
        return oneOf;
    }

    public Map<String, Schema> getAllOfSchemas() {
        return allOfSchemas;
    }

    public Map<String, Schema> getAnyOfSchemas() {
        return anyOfSchemas;
    }

    public Map<String, Schema> getOneOfSchemas() {
        return oneOfSchemas;
    }

    @Override
    public String displaySchema(final MarkupDocBuilder docBuilder) {
        final MarkupDocBuilder copy = docBuilder.copy(false);
        for (Iterator<Type> iterator = anyOf.iterator(); iterator.hasNext(); ) {
            copy.text(iterator.next().displaySchema(docBuilder));
            if (iterator.hasNext()) {
                copy.text(", ");
            }
        }
        return copy.toString();
    }
}
