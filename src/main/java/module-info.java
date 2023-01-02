module shrimpworks.unreal.packages {
	requires java.base;
	requires java.desktop;

	requires lzo.core;

	exports net.shrimpworks.unreal.packages;
	exports net.shrimpworks.unreal.packages.entities;
	exports net.shrimpworks.unreal.packages.entities.objects;
	exports net.shrimpworks.unreal.packages.entities.objects.geometry;
	exports net.shrimpworks.unreal.packages.entities.properties;
}