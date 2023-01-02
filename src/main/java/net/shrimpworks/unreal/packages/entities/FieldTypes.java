package net.shrimpworks.unreal.packages.entities;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum FieldTypes {
	Const,
	Enum,
	Struct,
	Function,
	State,
	TextBuffer,
	Property,
	ByteProperty,
	ObjectProperty,
	FixedArrayProperty,
	ArrayProperty,
	MapProperty,
	ClassProperty,
	StructProperty,
	IntProperty,
	BoolProperty,
	FloatProperty,
	NameProperty,
	StrProperty,
	StringProperty;

	private static final Set<String> STRINGS = Arrays.stream(values()).map(java.lang.Enum::name).collect(Collectors.toSet());

	public static boolean isField(ObjectReference classRef) {
		Named named = classRef.get();
		if (named == null) return true;
		if (classRef.index == 0) return true;
		return STRINGS.contains(named.name().name);
	}
}
