package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/SCR-CityStreet.unr"));
		Package pkg = new Package(Paths.get("/home/shrimp/tmp/MonsterHunt.u"));
//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/DM-Gael.ut2"));
//		Package pkg = new Package(Paths.get("/home/shrimp/tmp/XGame.u"));
		System.out.printf("Package version: %d%n", pkg.version());

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

//		System.out.println(pkg.object(pkg.exports()[234]));
//		for (int i = 0; i < pkg.exports().length; i++) {
//			System.out.println(" >>> " + i);
//			pkg.object(pkg.exports()[i]);
//			System.out.println(pkg.object(pkg.exports()[i]));
//		}

//		for (Package.Export ex : pkg.exports()) {
//			System.out.printf("%s (%s extends %s)%n", ex.name.name, ex.objClass.get(), ex.objSuper.get());
//		}

//		Properties.Property screenshot = pkg.object(pkg.exports()[0]).property("Screenshot");
//		System.out.println(((Package.Export)((Properties.ObjectProperty)screenshot).value.get()).objClass.get());

		// try to find all textures in exports
		for (Package.Export export : pkg.exports()) {
			System.out.println(export.objClass.get().name());
		}
	}
}
