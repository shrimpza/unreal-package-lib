package net.shrimpworks.unreal.packages.entities.objects;

import java.util.Arrays;
import java.util.Collection;

import net.shrimpworks.unreal.packages.Package;
import net.shrimpworks.unreal.packages.PackageReader;
import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.properties.Property;

public class Sound extends Object {

	public Sound(Package pkg, PackageReader reader, Export export, ObjectHeader header, Collection<Property> properties, int dataStart) {
		super(pkg, reader, export, header, properties, dataStart);
	}

	public SoundData readSound() {
		reader.moveTo(dataStart);

		final String format = pkg.names[reader.readIndex()].name;
		final int size = reader.readIndex();

		byte[] data = new byte[size];
		int pos = 0;

		while (pos < size) {
			pos += reader.readBytes(data, pos, size - pos);
			reader.fillBuffer();
		}

		return new SoundData(export.name.name, format, data);
	}

	public static class SoundData {

		public final String name;
		public final String format;
		public final byte[] data;

		public SoundData(String name, String format, byte[] data) {
			this.name = name;
			this.format = format;
			this.data = data;
		}

		@Override
		public String toString() {
			return String.format("SoundData [name=%s, format=%s, data=%s]", name, format, Arrays.toString(data));
		}
	}

}
