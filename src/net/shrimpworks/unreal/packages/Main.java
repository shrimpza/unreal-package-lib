package net.shrimpworks.unreal.packages;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;

public class Main {

	public static void main(String[] args) throws IOException {
		Umod umod = new Umod(Paths.get("/home/shrimp/tmp/MonsterHunt.umod"));
		System.out.println(Arrays.toString(umod.files));

//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/SCR-CityStreet.unr"));
		Package pkg = new Package(Paths.get("/home/shrimp/tmp/DM-Gael.ut2"));
		System.out.printf("Package version: %d%n", pkg.version);

		// read level info (also in LevelSummary, but missing Screenshot)
		ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
		Object level = levelInfo.object();
		System.out.println(level.property("Author"));
		System.out.println(level.property("Title"));
		System.out.println(level.property("Screenshot"));

		ExportedObject screenshot = pkg.objectByRef(((ObjectProperty)level.property("Screenshot")).value);
		Object object = screenshot.object();
		if (object instanceof Texture) {
			Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
			BufferedImage bufferedImage = mipMaps[0].get();
			ImageIO.write(bufferedImage, "png", new File("/tmp/screenshot.png"));
		} else if (object.className().equals("MaterialSequence")) {
			// TODO might have to read arrays after all
//			System.out.println(object.property("SequenceItems"));
			System.out.println(object.property("FallbackMaterial"));
			ExportedObject fallback = pkg.objectByRef(((ObjectProperty)object.property("FallbackMaterial")).value);
			Object fallbackObj = fallback.object();
			if (fallbackObj instanceof Texture) {
				Texture.MipMap[] mipMaps = ((Texture)fallbackObj).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", new File("/tmp/screenshot.png"));
			}

		}
	}
}
