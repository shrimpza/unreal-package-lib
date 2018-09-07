package net.shrimpworks.unreal.archive;

public interface Properties {

	public enum PropertyType {
		ByteProperty((byte)1),
		IntegerProperty((byte)2),
		BooleanProperty((byte)3),
		FloatProperty((byte)4),
		ObjectProperty((byte)5),
		NameProperty((byte)6),
		StringProperty((byte)7),
		ClassProperty((byte)8),
		ArrayProperty((byte)9),
		StructProperty((byte)10),
		VectorProperty((byte)11),
		RotatorProperty((byte)12),
		StrProperty((byte)13),
		MapProperty((byte)14),
		FixedArrayProperty((byte)15);

		private final byte type;

		PropertyType(byte type) {
			this.type = type;
		}

		public static PropertyType get(byte type) {
			for (PropertyType p : values()) {
				if (p.type == type) return p;
			}
			return null;
		}
	}

	public enum StructType {
		Color,
		Vector,
		Rotator,
		Scale,
		PointRegion,
		Sphere,
		Plane;

		public static StructType get(Package.Name name) {
			for (StructType s : values()) {
				if (s.name().equalsIgnoreCase(name.name())) return s;
			}
			return null;
		}
	}

	public static abstract class Property {

		final Package pkg;
		public final Package.Name name;

		private Property(Package pkg, Package.Name name) {
			this.pkg = pkg;
			this.name = name;
		}

		@Override
		public String toString() {
			return String.format("Property [name=%s]", name);
		}
	}

	public static class BooleanProperty extends Property {

		public final boolean value;

		public BooleanProperty(Package pkg, Package.Name name, boolean value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("BooleanProperty [name=%s, value=%s]", name, value);
		}
	}

	public static class ByteProperty extends Property {

		public final byte value;

		public ByteProperty(Package pkg, Package.Name name, byte value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("ByteProperty [name=%s, value=%s]", name, value);
		}
	}

	public static class IntegerProperty extends Property {

		public final int value;

		public IntegerProperty(Package pkg, Package.Name name, int value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("IntegerProperty [name=%s, value=%s]", name, value);
		}
	}

	public static class FloatProperty extends Property {

		public final float value;

		public FloatProperty(Package pkg, Package.Name name, float value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("FloatProperty [name=%s, value=%s]", name, value);
		}
	}

	public static class StringProperty extends Property {

		public final String value;

		public StringProperty(Package pkg, Package.Name name, String value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("StringProperty [name=%s, value=%s]", name, value);
		}
	}

	public static class ObjectProperty extends Property {

		public final Package.ObjectReference value;

		public ObjectProperty(Package pkg, Package.Name name, Package.ObjectReference value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("ObjectProperty [name=%s, value=%s]", name, value);
		}
	}

	public static class NameProperty extends Property {

		public final Package.Name value;

		public NameProperty(Package pkg, Package.Name name, Package.Name value) {
			super(pkg, name);
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("NameProperty [name=%s, value=%s]", name, value);
		}
	}

	public static abstract class StructProperty extends Property {

		public StructProperty(Package pkg, Package.Name name) {
			super(pkg, name);
		}
	}

	public static class PointRegionProperty extends StructProperty {

		public final Package.ObjectReference zone;
		public final int ileaf;
		public final byte zoneNumber;

		public PointRegionProperty(Package pkg, Package.Name name, Package.ObjectReference zone, int ileaf, byte zoneNumber) {
			super(pkg, name);
			this.zone = zone;
			this.ileaf = ileaf;
			this.zoneNumber = zoneNumber;
		}

		@Override
		public String toString() {
			return String.format("PointRegionProperty [name=%s, zone=%s, ileaf=%s, zoneNumber=%s]", name, zone, ileaf, zoneNumber);
		}
	}

	public static class VectorProperty extends StructProperty {

		public final float x;
		public final float y;
		public final float z;

		public VectorProperty(Package pkg, Package.Name name, float x, float y, float z) {
			super(pkg, name);
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public String toString() {
			return String.format("VectorProperty [name=%s, x=%s, y=%s, z=%s]", name, x, y, z);
		}
	}

	public static class RotatorProperty extends StructProperty {

		public final int pitch;
		public final int yaw;
		public final int roll;

		public RotatorProperty(Package pkg, Package.Name name, int pitch, int yaw, int roll) {
			super(pkg, name);
			this.pitch = pitch;
			this.yaw = yaw;
			this.roll = roll;
		}

		@Override
		public String toString() {
			return String.format("RotatorProperty [name=%s, pitch=%s, yaw=%s, roll=%s]", name, pitch, yaw, roll);
		}
	}

	public static class ScaleProperty extends VectorProperty {

		public final int sheerRate;
		public final byte sheerAxis;

		public ScaleProperty(Package pkg, Package.Name name, float x, float y, float z, int sheerRate, byte sheerAxis) {
			super(pkg, name, x, y, z);
			this.sheerRate = sheerRate;
			this.sheerAxis = sheerAxis;
		}

		@Override
		public String toString() {
			return String.format("ScaleProperty [name=%s, x=%s, y=%s, z=%s, sheerRate=%s, sheerAxis=%s]",
								 name, x, y, z, sheerRate, sheerAxis);
		}
	}

	public static class ColorProperty extends StructProperty {

		public final byte r;
		public final byte g;
		public final byte b;

		public ColorProperty(Package pkg, Package.Name name, byte r, byte g, byte b) {
			super(pkg, name);
			this.r = r;
			this.g = g;
			this.b = b;
		}

		@Override
		public String toString() {
			return String.format("ColorProperty [name=%s, r=%s, g=%s, b=%s]", name, r, g, b);
		}
	}

	public static abstract class ShapeProperty extends VectorProperty {

		public final float w;

		public ShapeProperty(Package pkg, Package.Name name, float x, float y, float z, float w) {
			super(pkg, name, x, y, z);
			this.w = w;
		}

		@Override
		public String toString() {
			return String.format("ShapeProperty [name=%s, x=%s, y=%s, z=%s, w=%s]", name, x, y, z, w);
		}
	}

	public static class SphereProperty extends ShapeProperty {

		public SphereProperty(Package pkg, Package.Name name, float x, float y, float z, float w) {
			super(pkg, name, x, y, z, w);
		}
	}

	public static class PlaneProperty extends ShapeProperty {

		public PlaneProperty(Package pkg, Package.Name name, float x, float y, float z, float w) {
			super(pkg, name, x, y, z, w);
		}
	}

	public static class FixedArrayProperty extends Property {

		private final Package.ObjectReference arrayType;
		private final int count;

		public FixedArrayProperty(Package pkg, Package.Name name, Package.ObjectReference arrayType, int count) {
			super(pkg, name);
			this.arrayType = arrayType;
			this.count = count;
		}

		public Package.ObjectReference arrayType() {
			return arrayType;
		}

		public int count() {
			return count;
		}

		@Override
		public String toString() {
			return String.format("FixedArrayProperty [name=%s, arrayType=%s, count=%s]", name, arrayType, count);
		}
	}
}
