/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2008-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.type;

import mondrian.olap.*;
import mondrian.olap.fun.Resolver;
import mondrian.test.TestContext;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit test for mondrian type facility.
 *
 * @author jhyde
 * @since Jan 17, 2008
 */
public class TypeTest {

    @Test public void testConversions() {
        final Connection connection = getTestContext().getConnection();
        Cube salesCube =
            getCubeWithName("Sales", connection.getSchema().getCubes());
        assertThat(salesCube, notNullValue());
        Dimension customersDimension = null;
        for (Dimension dimension : salesCube.getDimensionList()) {
            if (dimension.getName().equals("Customer")) {
                customersDimension = dimension;
            }
        }

        assertThat(customersDimension, notNullValue());
        Hierarchy hierarchy = customersDimension.getHierarchy();
        Member member = hierarchy.getDefaultMember();
        Level level = member.getLevel();
        Type memberType = new MemberType(
            customersDimension, hierarchy, level, member);
        final LevelType levelType =
            new LevelType(customersDimension, hierarchy, level);
        final HierarchyType hierarchyType =
            new HierarchyType(customersDimension, hierarchy);
        final DimensionType dimensionType =
            new DimensionType(customersDimension);
        final StringType stringType = new StringType();
        final ScalarType scalarType = new ScalarType();
        final NumericType numericType = new NumericType();
        final DateTimeType dateTimeType = new DateTimeType();
        final DecimalType decimalType = new DecimalType(10, 2);
        final DecimalType integerType = new DecimalType(7, 0);
        final NullType nullType = new NullType();
        final MemberType unknownMemberType = MemberType.Unknown;
        final TupleType tupleType =
            new TupleType(
                new Type[] {memberType,  unknownMemberType});
        final SetType tupleSetType = new SetType(tupleType);
        final SetType setType = new SetType(memberType);
        final LevelType unknownLevelType = LevelType.Unknown;
        final HierarchyType unknownHierarchyType = HierarchyType.Unknown;
        final DimensionType unknownDimensionType = DimensionType.Unknown;
        final BooleanType booleanType = new BooleanType();
        Type[] types = {
            memberType,
            levelType,
            hierarchyType,
            dimensionType,
            numericType,
            dateTimeType,
            decimalType,
            integerType,
            scalarType,
            nullType,
            stringType,
            booleanType,
            tupleType,
            tupleSetType,
            setType,
            unknownDimensionType,
            unknownHierarchyType,
            unknownLevelType,
            unknownMemberType
        };

        for (Type type : types) {
            // Check that each type is assignable to itself.
            final String desc = type.toString() + ":" + type.getClass();
            assertThat(desc, type.computeCommonType(type, null), is(type));

            int[] conversionCount = {0};
            assertThat(desc, type.computeCommonType(type, conversionCount),
                is(type));
            assertThat(conversionCount[0], is(0));

            // Check that each scalar type is assignable to nullable with zero
            // conversions.
            if (type instanceof ScalarType) {
                assertThat(type.computeCommonType(nullType, null), is(type));
                assertThat(type.computeCommonType(nullType, conversionCount), is(
                    type));
                assertThat(conversionCount[0], is(0));
            }
        }

        for (Type fromType : types) {
            for (Type toType : types) {
                Type type = fromType.computeCommonType(toType, null);
                Type type2 = toType.computeCommonType(fromType, null);
                final String desc =
                    "symmetric, from " + fromType + ", to " + toType;
                assertThat(desc, type2, is(type));

                int[] conversionCount = {0};
                int[] conversionCount2 = {0};
                type = fromType.computeCommonType(toType, conversionCount);
                type2 = toType.computeCommonType(fromType, conversionCount2);
                if (conversionCount[0] == 0
                    && conversionCount2[0] == 0)
                {
                    assertThat(desc, type2, is(type));
                }

                final int toCategory = TypeUtil.typeToCategory(toType);
                final List<Resolver.Conversion> conversions =
                    new ArrayList<Resolver.Conversion>();
                final boolean canConvert =
                    TypeUtil.canConvert(
                        0,
                        fromType,
                        toCategory,
                        conversions);
                if (canConvert && conversions.size() == 0 && type == null) {
                    if (!(fromType == memberType && toType == tupleType
                        || fromType == tupleSetType && toType == setType
                        || fromType == setType && toType == tupleSetType))
                    {
                        fail("can convert from " + fromType + " to " + toType
                             + ", but their most general type is null");
                    }
                }
                if (!canConvert && type != null && type.equals(toType)) {
                    fail("cannot convert from " + fromType + " to " + toType
                         + ", but they have a most general type " + type);
                }
            }
        }
    }

    protected TestContext getTestContext() {
        return TestContext.instance();
    }

    @Test public void testCommonTypeWhenSetTypeHavingMemberTypeAndTupleType() {
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure());

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild());

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild());

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        SetType setTypeWithMember = new SetType(measureMemberType);
        SetType setTypeWithTuple = new SetType(tupleType);

        Type type1 =
            setTypeWithMember.computeCommonType(setTypeWithTuple, null);
        assertThat(type1, notNullValue());
        assertThat(((SetType) type1).getElementType() instanceof TupleType, is(true));

        Type type2 =
            setTypeWithTuple.computeCommonType(setTypeWithMember, null);
        assertThat(type2, notNullValue());
        assertThat(((SetType) type2).getElementType() instanceof TupleType, is(true));
        assertThat(type2, is(type1));
    }

    @Test public void testCommonTypeOfMemberandTupleTypeIsTupleType() {
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure());

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild());

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild());

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        Type type1 = measureMemberType.computeCommonType(tupleType, null);
        assertThat(type1, notNullValue());
        assertThat(type1 instanceof TupleType, is(true));

        Type type2 = tupleType.computeCommonType(measureMemberType, null);
        assertThat(type2, notNullValue());
        assertThat(type2 instanceof TupleType, is(true));
        assertThat(type2, is(type1));
    }

    @Test public void testCommonTypeBetweenTuplesOfDifferentSizesIsATupleType() {
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure());

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild());

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild());

        TupleType tupleTypeLarger = new TupleType(
            new Type[] {storeMemberType, genderMemberType, measureMemberType});

        TupleType tupleTypeSmaller = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        Type type1 = tupleTypeSmaller.computeCommonType(tupleTypeLarger, null);
        assertThat(type1, notNullValue());
        assertThat(type1 instanceof TupleType, is(true));
        assertThat(((TupleType) type1).elementTypes[0] instanceof MemberType, is(true));
        assertThat(((TupleType) type1).elementTypes[1] instanceof MemberType, is(true));
        assertThat(((TupleType) type1).elementTypes[2] instanceof ScalarType, is(true));

        Type type2 = tupleTypeLarger.computeCommonType(tupleTypeSmaller, null);
        assertThat(type2, notNullValue());
        assertThat(type2 instanceof TupleType, is(true));
        assertThat(((TupleType) type2).elementTypes[0] instanceof MemberType, is(true));
        assertThat(((TupleType) type2).elementTypes[1] instanceof MemberType, is(true));
        assertThat(((TupleType) type2).elementTypes[2] instanceof ScalarType, is(true));
        assertThat(type2, is(type1));
    }

    private MemberType getStoreMemberType(Member storeChild) {
        return new MemberType(
            storeChild.getDimension(),
            storeChild.getDimension().getHierarchy(),
            storeChild.getLevel(),
            storeChild);
    }

    private Member getStoreChild() {
        List<Id.Segment> storeParts = Arrays.<Id.Segment>asList(
            new Id.NameSegment("Store", Id.Quoting.UNQUOTED),
            new Id.NameSegment("Stores", Id.Quoting.UNQUOTED),
            new Id.NameSegment("All Stores", Id.Quoting.UNQUOTED),
            new Id.NameSegment("USA", Id.Quoting.UNQUOTED),
            new Id.NameSegment("CA", Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader().getMemberByUniqueName(
            storeParts, false);
    }

    private MemberType getMemberTypeHavingMaleChild(Member maleChild) {
        return new MemberType(
            maleChild.getDimension(),
            maleChild.getHierarchy(),
            maleChild.getLevel(),
            maleChild);
    }

    private MemberType getMemberTypeHavingMeasureInIt(Member unitSalesMeasure) {
        return new MemberType(
            unitSalesMeasure.getDimension(),
            unitSalesMeasure.getHierarchy(),
            unitSalesMeasure.getHierarchy().getLevelList().get(0),
            unitSalesMeasure);
    }

    private Member getMaleChild() {
        List<Id.Segment> genderParts = Arrays.<Id.Segment>asList(
            new Id.NameSegment("Customer", Id.Quoting.UNQUOTED),
            new Id.NameSegment("Gender", Id.Quoting.UNQUOTED),
            new Id.NameSegment("M", Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader().getMemberByUniqueName(
            genderParts, false);
    }

    private Member getUnitSalesMeasure() {
        List<Id.Segment> measureParts = Arrays.<Id.Segment>asList(
            new Id.NameSegment("Measures", Id.Quoting.UNQUOTED),
            new Id.NameSegment("Unit Sales", Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader().getMemberByUniqueName(
            measureParts, false);
    }

    private SchemaReader getSalesCubeSchemaReader() {
        final Cube salesCube = getCubeWithName(
            "Sales",
            getSchemaReader().getCubes());
        return salesCube.getSchemaReader(
            getTestContext().getConnection().getRole()).withLocus();
    }

    private SchemaReader getSchemaReader() {
        return getTestContext().getConnection().getSchemaReader().withLocus();
    }

    private Cube getCubeWithName(String cubeName, Cube[] cubes) {
        Cube resultCube = null;
        for (Cube cube : cubes) {
            if (cubeName.equals(cube.getName())) {
                resultCube = cube;
                break;
            }
        }
        return resultCube;
    }
}

// End TypeTest.java
