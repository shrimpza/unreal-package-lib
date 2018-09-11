package net.shrimpworks.unreal.packages.entities.objects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.Named;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * Factory for instantiating instances of objects of known types.
 * <p>
 * Each known object type should be added to the enum set with its associated
 * implementation class, and the value name should match the class name of the
 * Unreal object, as per {@link Object#className()}.
 */
public enum ObjectFactory {

	Texture(Texture.class),
	Palette(Palette.class);

	private final Class<? extends Object> clazz;

	ObjectFactory(Class<? extends Object> clazz) {
		this.clazz = clazz;
	}

	/**
	 * Magically create a new typed instance of an object for the provided
	 * export, if a specific type exists and is defined in the
	 * {@link ObjectFactory} enum.
	 *
	 * @param pkg        package
	 * @param reader     package reader, used by objects for decoding their content from the package
	 * @param export     exported entity
	 * @param header     object header
	 * @param properties properties read for object
	 * @param dataStart  position of object payload in package
	 * @return a new object
	 */
	public static Object newInstance(
			Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {

		Named type = export.objClass.get();

		try {
			ObjectFactory objectFactory = ObjectFactory.valueOf(type.name().name);
			Constructor<? extends Object> constructor = objectFactory.clazz.getConstructor(Package.class, PackageReader.class, Export.class,
																						   ObjectHeader.class, Collection.class,
																						   int.class);
			return constructor.newInstance(pkg, reader, export, header, properties, dataStart);
		} catch (IllegalArgumentException | NoSuchMethodException
				| IllegalAccessException | InstantiationException | InvocationTargetException e) {
			return new Object(pkg, reader, export, header, properties, dataStart);
		}
	}
}
