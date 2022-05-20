package net.shrimpworks.unreal.packages.entities;

public class NameNumber {

	public final int name;
	public final int number;

	public NameNumber(int name) {
		this(name, -1);
	}

	public NameNumber(int name, int number) {
		this.name = name;
		this.number = number;
	}
}
