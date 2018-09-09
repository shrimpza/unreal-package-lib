package net.shrimpworks.unreal.archive;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import javax.imageio.ImageIO;

public class Main {

	public static void main(String[] args) throws IOException {
		Package pkg = new Package(Paths.get("/home/shrimp/tmp/SCR-CityStreet.unr"));
//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/MonsterHunt.u"));
//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/DM-Gael.ut2"));
//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/XGame.u"));
		System.out.printf("Package version: %d%n", pkg.version);

//		System.out.println(pkg.exports()[0]);
//		System.out.println(pkg.object(pkg.exports()[0]));
//
//		System.out.println(pkg.exports()[0]);
//
//		System.out.println(pkg.exports()[0].flags());
//
//		System.out.println(pkg.exports()[0].objClass().get());
//
//		if (pkg.exports()[0].objClass().get() instanceof Package.Import) {
//			System.out.println(((Package.Import)pkg.exports()[0].objClass().get()).packageName().get());
//		}

		// read all objects' properties
//		System.out.println(pkg.object(pkg.exports()[234]));
//		for (int i = 0; i < pkg.objects.length; i++) {
//			System.out.println(" >>> " + i + ": " + pkg.objects[i].objClass.get().name());
//			try {
//				System.out.println(pkg.objects[i].object());
//			} catch (Exception e) {
//				System.out.println("ERR: " + e.getMessage());
//			}
//			if (pkg.exports[i] instanceof Package.ExportedObject) {
//				System.out.println(((Package.ExportedObject)pkg.exports[i]).object());
//			}
//		}

//		for (Package.Export ex : pkg.exports) {
//			System.out.printf("%s (%s extends %s)%n", ex.name.name, ex.objClass.get(), ex.objSuper.get());
//		}

//		Properties.Property screenshot = pkg.object(pkg.exports()[0]).property("Screenshot");
//		System.out.println(((Package.Export)((Properties.ObjectProperty)screenshot).value.get()).objClass.get());

		// try to find all textures in exports
//		Collection<Entities.ExportedObject> textures = pkg.objectsByClassName("Texture");
//		for (Entities.ExportedObject tex : textures) {
//			Objects.Texture obj = (Objects.Texture)tex.object();
//			Objects.Texture.MipMap[] mipMaps = obj.mipMaps();
//			System.out.println(Arrays.toString(mipMaps));
//
//			BufferedImage bufferedImage = mipMaps[0].get();
//			ImageIO.write(bufferedImage, "jpg", new File("/tmp/tmp.jpg"));
//		}

		// read level info (also in LevelSummary, but missing Screenshot)
		Entities.ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
		System.out.println(levelInfo.object().property("Author"));
		System.out.println(levelInfo.object().property("Title"));
		System.out.println(levelInfo.object().property("Screenshot"));

		System.out.println(((Properties.ObjectProperty)levelInfo.object().property("Screenshot")).value.get());
//
//		for (Properties.Property property : levelInfo.object().properties) {
//			System.out.println(property);
//		}
	}
}
