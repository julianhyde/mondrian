/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2016 Pentaho
// All Rights Reserved.
*/
package mondrian.olap4j;

import mondrian.olap.*;
import mondrian.rolap.RolapCubeDimension;

import org.olap4j.OlapException;
import org.olap4j.impl.*;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.*;

/**
 * Implementation of {@link org.olap4j.metadata.Dimension}
 * for the Mondrian OLAP engine.
 *
 * @author jhyde
 * @since May 24, 2007
 */
class MondrianOlap4jDimension
    extends MondrianOlap4jMetadataElement
    implements Dimension, Named
{
    private final MondrianOlap4jSchema olap4jSchema;
    private final mondrian.olap.Dimension dimension;

    MondrianOlap4jDimension(
        MondrianOlap4jSchema olap4jSchema,
        mondrian.olap.Dimension dimension)
    {
        this.olap4jSchema = olap4jSchema;
        this.dimension = dimension;
    }

    public boolean equals(Object obj) {
        return obj instanceof MondrianOlap4jDimension
            && dimension.equals(((MondrianOlap4jDimension) obj).dimension);
    }

    public int hashCode() {
        return dimension.hashCode();
    }

    public NamedList<Hierarchy> getHierarchies() {
        final NamedList<MondrianOlap4jHierarchy> list =
            new NamedListImpl<MondrianOlap4jHierarchy>();
        final MondrianOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;
        final mondrian.olap.SchemaReader schemaReader =
            olap4jConnection.getMondrianConnection2().getSchemaReader()
            .withLocus();
        for (mondrian.olap.Hierarchy hierarchy
            : schemaReader.getDimensionHierarchies(dimension))
        {
            list.add(olap4jConnection.toOlap4j(hierarchy));
        }
        return Olap4jUtil.cast(list);
    }

    public Hierarchy getDefaultHierarchy() {
        return getHierarchies().get(0);
    }

    public Type getDimensionType() throws OlapException {
        return dimension.getDimensionType();
    }

    public String getName() {
        return dimension.getName();
    }

    public String getUniqueName() {
        return dimension.getUniqueName();
    }

    public String getCaption() {
        return dimension.getLocalized(
            LocalizedProperty.CAPTION,
            olap4jSchema.getLocale());
    }

    public String getDescription() {
        return dimension.getLocalized(
            LocalizedProperty.DESCRIPTION,
            olap4jSchema.getLocale());
    }

    public boolean isVisible() {
        return dimension.isVisible();
    }

    protected OlapElement getOlapElement() {
        return dimension;
    }

    public Cube getCube() {
        if (dimension instanceof RolapCubeDimension) {
            final MondrianOlap4jConnection olap4jConnection =
                olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData
                    .olap4jConnection;
            return olap4jConnection.toOlap4j(
                ((RolapCubeDimension) dimension).getCube());
        } else {
            return null;
        }
    }

    public int getOrdinal() {
        if (dimension instanceof RolapCubeDimension) {
            return ((RolapCubeDimension) dimension).getOrdinal();
        } else {
            return -1;
        }
    }

    public int getCardinality() {
        return -1;
    }

    public KeyUniqueness getUniqueSettings() {
        return KeyUniqueness.MEMBER_KEY_UNIQUE;
    }
}

// End MondrianOlap4jDimension.java
