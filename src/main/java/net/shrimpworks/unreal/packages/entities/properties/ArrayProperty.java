package net.shrimpworks.unreal.packages.entities.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;

public class ArrayProperty extends Property {

	public final List<Property> values;

	public ArrayProperty(Property initialValue) {
		this(initialValue.pkg, initialValue.name, List.of(initialValue));
	}

	public ArrayProperty(Package pkg, Name name, List<Property> values) {
		super(pkg, name);
		this.values = Collections.unmodifiableList(values);
	}

	public ArrayProperty add(ArrayItem value) {
		List<Property> nextValues = new ArrayList<>(values);
		nextValues.add(Math.min(value.index, nextValues.size()), value.property);
		return new ArrayProperty(pkg, name, nextValues);
	}

	@Override
	public String toString() {
		return String.format("ArrayProperty [name=%s, values=%s]", name, values);
	}

	/**
	 * A special magical transitional item which represents an element within
	 * an array.
	 * <p>
	 * The Package managing this property should unwrap the property within
	 * and add it to an ArrayProperty.
	 */
	public static class ArrayItem extends Property {

		public final Property property;
		public final int index;

		public ArrayItem(Property property, int index) {
			super(property.pkg, property.name);
			this.property = property;
			this.index = index;
		}

		@Override
		public String toString() {
			return String.format("ArrayItem [index=%s, property=%s]", index, property);
		}
	}
}
