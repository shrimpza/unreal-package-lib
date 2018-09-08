package net.shrimpworks.unreal.archive;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
//		Package pgk = new Package(Paths.get("/home/shrimp/tmp/SCR-CityStreet.unr"));
		Package pgk = new Package(Paths.get("/home/shrimp/tmp/DM-Gael.ut2"));
//		Package pgk = new Package(Paths.get("/home/shrimp/tmp/XGame.u"));
		System.out.printf("Package version: %d%n", pgk.version());

//		System.out.println(pgk.exports()[0]);
//		System.out.println(pgk.object(pgk.exports()[0]));
//
//		System.out.println(pgk.exports()[0]);
//
//		System.out.println(pgk.exports()[0].flags());
//
//		System.out.println(pgk.exports()[0].objClass().get());
//
//		if (pgk.exports()[0].objClass().get() instanceof Package.Import) {
//			System.out.println(((Package.Import)pgk.exports()[0].objClass().get()).packageName().get());
//		}

//		System.out.println(pgk.object(pgk.exports()[234]));
		for (int i = 0; i < pgk.exports().length; i++) {
			System.out.println(" >>> " + i);
//			pgk.object(pgk.exports()[i]);
			System.out.println(pgk.object(pgk.exports()[i]));
		}
	}
}
