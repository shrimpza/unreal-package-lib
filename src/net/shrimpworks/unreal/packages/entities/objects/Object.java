package net.shrimpworks.unreal.packages.entities.objects;

import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;

/**
 * Parent type for packaged objects.
 * <p>
 * Implementations are expected to be able to read, decode and present their
 * contents in usable formats.
 * <p>
 * Implementations should not change the constructor signature, and should be
 * represented by an additional value in the {@link ObjectFactory} enum.
 */
public class Object {

	final Package pkg;
	final PackageReader reader;

	public final Export export;
	public final ObjectHeader header;
	public final Collection<Property> properties;
	final int dataStart;

	public Object(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		this.pkg = pkg;
		this.reader = reader;

		this.export = export;
		this.header = header;
		this.properties = properties;

		this.dataStart = dataStart;
	}

	/**
	 * Convenience to obtain a property by name.
	 *
	 * @param propertyName name of property
	 * @return the property, or null if not found
	 */
	public Property property(String propertyName) {
		for (Property p : properties) {
			if (p.name.name.equals(propertyName)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Get the class of the object.
	 *
	 * @return object class
	 */
	public String className() {
		return export.objClass.get().name().name;
	}

	@Override
	public String toString() {
		return String.format("%s [class=%s, export=%s, header=%s, properties=%s]",
							 getClass().getSimpleName(), className(), export, header, properties);
	}
}
