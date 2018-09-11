package net.shrimpworks.unreal.packages.entities;

public interface Named {

	public static Named NULL = () -> Name.NONE;

	public Name name();
}
