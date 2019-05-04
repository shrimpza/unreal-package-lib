package net.shrimpworks.unreal.packages.entities.properties;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.ObjectReference;

public abstract class StructProperty extends Property {

	public enum StructType {
		Color,
		Vector,
		Rotator,
		Scale,
		PointRegion,
		Sphere,
		Plane,
		Unknown;

		public static StructType get(Name name) {
			for (StructType s : StructType.values()) {
				if (s.name().equalsIgnoreCase(name.name)) return s;
			}
			return Unknown;
		}
	}

	public StructProperty(Package pkg, Name name) {
		super(pkg, name);
	}

	public static class PointRegionProperty extends StructProperty {

		public final ObjectReference zone;
		public final int ileaf;
		public final byte zoneNumber;

		public PointRegionProperty(Package pkg, Name name, ObjectReference zone, int ileaf, byte zoneNumber) {
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

		public VectorProperty(Package pkg, Name name, float x, float y, float z) {
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

		public RotatorProperty(Package pkg, Name name, int pitch, int yaw, int roll) {
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

		public final float sheerRate;
		public final byte sheerAxis;

		public ScaleProperty(Package pkg, Name name, float x, float y, float z, float sheerRate, byte sheerAxis) {
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
		public final byte a;

		public ColorProperty(Package pkg, Name name, byte r, byte g, byte b, byte a) {
			super(pkg, name);
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}

		@Override
		public String toString() {
			return String.format("ColorProperty [name=%s, r=%s, g=%s, b=%s, a=%s]", name, r, g, b, a);
		}
	}

	public static abstract class ShapeProperty extends VectorProperty {

		public final float w;

		public ShapeProperty(Package pkg, Name name, float x, float y, float z, float w) {
			super(pkg, name, x, y, z);
			this.w = w;
		}

		@Override
		public String toString() {
			return String.format("%s [name=%s, x=%s, y=%s, z=%s, w=%s]", getClass().getSimpleName(), name, x, y, z, w);
		}
	}

	public static class SphereProperty extends ShapeProperty {

		public SphereProperty(Package pkg, Name name, float x, float y, float z, float w) {
			super(pkg, name, x, y, z, w);
		}
	}

	public static class PlaneProperty extends ShapeProperty {

		public PlaneProperty(Package pkg, Name name, float x, float y, float z, float w) {
			super(pkg, name, x, y, z, w);
		}
	}

	public static class UnknownStructProperty extends StructProperty {

		public UnknownStructProperty(Package pkg, Name name) {
			super(pkg, name);
		}
	}

}
