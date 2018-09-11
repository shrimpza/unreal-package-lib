package net.shrimpworks.unreal.packages.entities.properties;

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
