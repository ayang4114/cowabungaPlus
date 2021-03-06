package cyr7.semantics.types;

import cyr7.util.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ExpandedType {

    final public static ExpandedType boolType =
        new ExpandedType(PrimitiveType.boolDefault);
    final public static ExpandedType intType =
        new ExpandedType(PrimitiveType.intDefault);
    final public static ExpandedType floatType =
            new ExpandedType(PrimitiveType.floatDefault);
    final public static ExpandedType unitExpandedType =
        new ExpandedType();
    final public static ExpandedType unitOrdinaryType =
        new ExpandedType(UnitType.unitValue);
    final public static ExpandedType voidOrdinaryType =
        new ExpandedType(VoidType.voidValue);
    final public static ExpandedType genericAddType =
            new ExpandedType(GenericAddType.value);
    final public static ExpandedType voidArrayType =
        new ExpandedType(ArrayType.voidArrayDefault);

    /**
     * Returns the supertype of two types, if such a relation exists.
     *
     * @return Empty if there is typing relation between {@code left}
     * and {@code right}.
     */
    public static Optional<ExpandedType> supertypeOf(ExpandedType left,
                                                      ExpandedType right) {
        if (left.isASubtypeOf(right)) {
            return Optional.of(right);
        } else if (right.isASubtypeOf(left)) {
            return Optional.of(left);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ExpandedType)) {
            return false;
        }
        ExpandedType other = (ExpandedType) obj;
        return Objects.equals(types, other.types);
    }

    @Override
    public String toString() {
        if (this.isUnit()) {
            return "()";
        } else if (this.isOrdinary()) {
            if (this.equals(voidArrayType)) {
                return "void[]";
            } else {
                return this.getOrdinaryType().toString();
            }
        } else {
            return "(" +
                    this.types.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")) +
                    ")";
        }
    }


    final private List<OrdinaryType> types;

    /**
     * Creates a Unit ExpandedType.
     */
    private ExpandedType() {
        this.types = Util.immutableCopy(List.of());
    }

    /**
     * Creates an OrdinaryType ExpandedType.
     */
    public ExpandedType(OrdinaryType type) {
        this.types = Util.immutableCopy(List.of(type));
    }


    /**
     * Creates a Tuple ExpandedType.
     */
    public ExpandedType(List<OrdinaryType> types) {
        this.types = Util.immutableCopy(types);
    }

    private enum Type {
        ORDINARY, TUPLE, UNIT
    }

    public List<OrdinaryType> getTypes() {
        return this.types;
    }


    /**
     * Returns {@code true} if {@code this} is a subtype of
     * {@code supertypeSet}. The following rules determine if ExpandedType x
     * is a subtype of ExpandedType y:
     *
     * <ul>
     * <li> If x and y are both Unit, then x is a subtype of y because
     * there are no ordinary types.</li>
     * <li> If x and y are both Ordinary, then x is a subtype of y if and
     * only if the OrdinaryType of x is a subtype of the ordinary type of y
     * .</li>
     * <li> If x and y are both Tuples, then x is a subtype of y if and only if
     * the size of x == size of y, and for each OrdinaryType x_i of x and each
     * OrdinaryType y_i in y, x_i is a subtype of y_i. </li>
     * <li> If size of x != size of y, then x is not a subtype of y.</li>
     * </ul>.
     *
     * @param supertypeSet
     * @return
     */
    public boolean isASubtypeOf(ExpandedType supertypeSet) {
        if (this.types.size() != supertypeSet.types.size()) {
            return false;
        }

        Iterator<OrdinaryType> expectedSubtype = this.types.iterator();
        Iterator<OrdinaryType> expectedSupertype = supertypeSet.types.iterator();

        boolean isSubtype = true;
        while (expectedSubtype.hasNext() && expectedSupertype.hasNext()) {
            OrdinaryType subtype = expectedSubtype.next();
            OrdinaryType supertype = expectedSupertype.next();
            isSubtype &= subtype.isSubtypeOf(supertype);
        }
        return isSubtype;
    }

    public Type getType() {
        int size = this.types.size();
        switch (size) {
            case 0:
                return Type.UNIT;
            case 1:
                return Type.ORDINARY;
            default:
                return Type.TUPLE;
        }
    }

    public boolean isOrdinary() {
        return this.types.size() == 1;
    }

    public boolean isArray() {
        return this.isOrdinary() && this.getOrdinaryType().isArray();
    }


    public OrdinaryType getOrdinaryType() {
        assert this.isOrdinary();
        return this.types.get(0);
    }

    public OrdinaryType getInnerArrayType() {
        if (this.isVoid()) {
            return VoidType.voidValue;
        } if (this.isGenericAdd()) {
            return VoidType.voidValue;
        } else {
            assert this.isArray();
            return ((ArrayType) this.getOrdinaryType()).child;
        }
    }


    public boolean isTuple() {
        return this.types.size() >= 2;
    }

    public boolean isUnit() {
        return this.types.isEmpty();
    }

    public boolean isSubtypeOfInt() { return this.isASubtypeOf(ExpandedType.intType); }

    public boolean isSubtypeOfFloat() { return this.isASubtypeOf(ExpandedType.floatType); }

    public boolean isVoid() {
        return this.isOrdinary() && this.getOrdinaryType().isVoid();
    }

    public boolean isGenericAdd() {
        return this.isOrdinary() && this.getOrdinaryType().isGenericAdd();
    }

    public boolean isSubtypeOfArray() {
        return this.isVoid() || this.isGenericAdd() || this.isArray();
    }

    public boolean isSubtypeOfBool() {
        return this.isASubtypeOf(ExpandedType.boolType);
    }


}
