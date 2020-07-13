package net.shrimpworks.unreal.packages;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;

import net.shrimpworks.unreal.packages.entities.Export;
import net.shrimpworks.unreal.packages.entities.ExportedObject;
import net.shrimpworks.unreal.packages.entities.Import;
import net.shrimpworks.unreal.packages.entities.Name;
import net.shrimpworks.unreal.packages.entities.objects.Model;
import net.shrimpworks.unreal.packages.entities.objects.Object;
import net.shrimpworks.unreal.packages.entities.objects.Polys;
import net.shrimpworks.unreal.packages.entities.objects.Texture;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Polygon;
import net.shrimpworks.unreal.packages.entities.objects.geometry.Vector;
import net.shrimpworks.unreal.packages.entities.properties.ByteProperty;
import net.shrimpworks.unreal.packages.entities.properties.ObjectProperty;
import net.shrimpworks.unreal.packages.entities.properties.Property;
import net.shrimpworks.unreal.packages.entities.properties.StringProperty;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static net.shrimpworks.unreal.packages.entities.properties.StructProperty.*;
import static org.junit.jupiter.api.Assertions.*;

public class PackageTest {

	private static Path unrMap;
	private static Path ut2Map;

	@BeforeAll
	public static void setup() throws IOException {
		// unpack our test map to a temporary location
		unrMap = Files.createTempFile("test-map-", ".unr");
		try (InputStream is = PackageTest.class.getResourceAsStream("SCR-CityStreet.unr.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, unrMap, StandardCopyOption.REPLACE_EXISTING);
		}

		ut2Map = Files.createTempFile("test-map-", ".ut2");
		try (InputStream is = PackageTest.class.getResourceAsStream("UWRM-Torpedo.ut2.gz");
			 GZIPInputStream gis = new GZIPInputStream(is)) {
			Files.copy(gis, ut2Map, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@AfterAll
	public static void teardown() throws IOException {
		Files.deleteIfExists(unrMap);
		Files.deleteIfExists(ut2Map);
		unrMap = null;
		ut2Map = null;
	}

	@Test
	public void readGeometryTypes() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			pkg.objectsByClassName("Model").forEach(o -> {
				assertTrue(o.object() instanceof Model);
			});

			pkg.objectsByClassName("Polys").forEach(o -> {
				assertTrue(o.object() instanceof Polys);
			});
		}
	}

	@Test
	public void readLevelInfo() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			assertNotNull(pkg.sha1Hash());

			// read level info (also in LevelSummary, but missing Screenshot)
			ExportedObject levelInfo = pkg.objectsByClassName("LevelInfo").iterator().next();
			Object level = levelInfo.object();

			// read some basic level info
			assertEquals("Kenneth \"Shrimp\" Watson", ((StringProperty)level.property("Author")).value);
			assertEquals("City Street", ((StringProperty)level.property("Title")).value);

			// get the screenshot, and then save it to file
			Property shotProp = level.property("Screenshot");
			assertTrue(shotProp instanceof ObjectProperty);

			ExportedObject screenshot = pkg.objectByRef(((ObjectProperty)shotProp).value);
			Object object = screenshot.object();

			assertTrue(object instanceof Texture);

			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

			try {
				Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
			} finally {
				Files.deleteIfExists(tmpScreenshot);
			}

		}
	}

	@Test
	public void readUE2Level() throws IOException {
		try (Package pkg = new Package(ut2Map)) {
			assertNotNull(pkg.sha1Hash());
			ExportedObject screenshot = pkg.objectByName(new Name("Shot00052"));

			Object object = screenshot.object();

			assertTrue(object instanceof Texture);

			Path tmpScreenshot = Files.createTempFile("test-ss-", ".png");

			try {
				Texture.MipMap[] mipMaps = ((Texture)object).mipMaps();
				BufferedImage bufferedImage = mipMaps[0].get();
				ImageIO.write(bufferedImage, "png", tmpScreenshot.toFile());

				// attempt to load the saved screenshot, should work if it was written correctly
				assertNotNull(ImageIO.read(tmpScreenshot.toFile()));
			} finally {
				Files.deleteIfExists(tmpScreenshot);
			}
		}
	}

	@Test
	public void readImports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<Import> imports = pkg.packageImports();
			assertFalse(imports.isEmpty());
			Import engine = imports.stream()
								   .filter(i -> i.name.name.equals("Engine"))
								   .findFirst().get();
			assertNotNull(engine);
			Import levelSummary = engine.children().stream()
										.filter(o -> o.name.name.equals("LevelSummary") && o.className.name.equals("Class"))
										.findFirst().get();
			assertNotNull(levelSummary);
		}
	}

	@Test
	public void readExports() throws IOException {
		try (Package pkg = new Package(unrMap)) {
			Collection<Export> local = pkg.rootExports();
			assertFalse(local.isEmpty());
			Export levelInfo = local.stream()
									.filter(e -> e.name.name.startsWith("LevelInfo"))
									.findFirst().get();
			assertNotNull(levelInfo);
			assertNotNull(pkg.objectByExport(levelInfo));
		}
	}

	@Test
	public void readPolys() throws IOException {
		class BrushThing {

			final Object brush;
			final Model model;
			final Polys polys;
			final ScaleProperty mainScale;
			final ScaleProperty postScale;
			final VectorProperty location;
			final Vector pivot;
			final RotatorProperty rotation;

			public BrushThing(
					Object brush, Model model, Polys polys,
					ScaleProperty mainScale, ScaleProperty postScale,
					VectorProperty location,
					VectorProperty pivot,
					RotatorProperty rotation) {
				this.brush = brush;
				this.model = model;
				this.polys = polys;
				this.mainScale = mainScale;
				this.postScale = postScale;
				this.location = location;
				this.pivot = pivot == null
						? new Vector(0, 0, 0)
						: new Vector(pivot.x, pivot.y, pivot.z);
				this.rotation = rotation;
			}
		}

//		try (Package pkg = new Package(unrMap)) {
//		try (Package pkg = new Package(Paths.get("/home/shrimp/tmp/DM-Morbias][.unr"))) {
		try (Package pkg = new Package(Paths.get("/home/shrimp/tmp/monsterhunt/Maps/MH-Trials.unr"))) {
//		try (Package pkg = new Package(Paths.get("/home/shrimp/tmp/Rotations.unr"))) {
			int minX = 0, maxX = 0;
			int minY = 0, maxY = 0;

			final ScaleProperty defaultScale = new ScaleProperty(pkg, new Name("PostScale"), 1f, 1f, 1f, 0f,
																 (byte)5);
			final RotatorProperty defaultRot = new RotatorProperty(pkg, new Name("Rotation"), 0, 0, 0);
			final VectorProperty defaultLoc = new VectorProperty(pkg, new Name("Location"), 0f, 0f, 0f);

			final Set<Byte> csgOps = Set.of((byte)1, (byte)2);

			List<BrushThing> brushes = new ArrayList<>();
			for (ExportedObject brush : pkg.objectsByClassName("Brush")) {
				Object brushObj = brush.object();

				Property region = brushObj.property("Region");
				if (region instanceof PointRegionProperty && ((PointRegionProperty)region).zone.get(true).name().name.contains("SkyZone")) {
					continue;
				}
				if (brushObj.property("CsgOper") == null || !csgOps.contains(((ByteProperty)brushObj.property("CsgOper")).value)) continue;

				Property modelProp = brushObj.property("Brush");
				Property location = brushObj.property("Location");
				Property pivot = brushObj.property("PrePivot");
				Property rotation = brushObj.property("Rotation");
				Property postScale = brushObj.property("PostScale");
				Property mainScale = brushObj.property("MainScale");
				if (location == null) location = defaultLoc;
				if (postScale == null) postScale = defaultScale;
				if (mainScale == null) mainScale = defaultScale;
				if (rotation == null) rotation = defaultRot;
				if (modelProp instanceof ObjectProperty) {
					ExportedObject modelExp = pkg.objectByRef(((ObjectProperty)modelProp).value);
					if (modelExp != null && modelExp.object() instanceof Model) {
						Model model = (Model)modelExp.object();
						ExportedObject polysExp = pkg.objectByRef(model.polys);
						if (polysExp != null && polysExp.object() instanceof Polys) {
							if (((Polys)polysExp.object()).polys.size() == 1) continue;
							brushes.add(new BrushThing(brushObj, model, (Polys)polysExp.object(),
													   (ScaleProperty)mainScale,
													   (ScaleProperty)postScale,
													   (VectorProperty)location,
													   (VectorProperty)pivot,
													   (RotatorProperty)rotation));
						}
					}
				}
			}

			for (BrushThing brush : brushes) {
				minX = Math.min(minX, (int)brush.location.x - 300);
				maxX = Math.max(maxX, (int)brush.location.x + 300);
				minY = Math.min(minY, (int)brush.location.y - 300);
				maxY = Math.max(maxY, (int)brush.location.y + 300);
			}

			BufferedImage bigImage = new BufferedImage(maxX - minX, maxY - minY, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = bigImage.createGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, bigImage.getWidth(), bigImage.getHeight());

			graphics.setStroke(new BasicStroke(5));
			graphics.setColor(Color.BLACK);

			// TODO rotate brushes
			// https://www.gamefromscratch.com/post/2012/11/24/GameDev-math-recipes-Rotating-one-point-around-another-point.aspx
			// https://stackoverflow.com/questions/2259476/rotating-a-point-about-another-point-2d

			// 360 == 32786

			for (BrushThing brush : brushes) {
				if (brush.brush.export.name.name.equalsIgnoreCase("brush234")) {
					System.out.println("hi");
				}

				for (Polygon p : brush.polys.polys) {
					int[] pointsX = new int[p.vertices.size() + 1];
					int[] pointsY = new int[p.vertices.size() + 1];
					for (int i = 0; i < p.vertices.size(); i++) {
						Vector vertex = p.vertices.get(i);

						Vector v = translate2(vertex, brush.mainScale, brush.postScale, brush.pivot, brush.rotation);

						pointsX[i] = (int)(v.x - minX + brush.location.x - brush.pivot.x);
						pointsY[i] = (int)(v.y - minY + brush.location.y - brush.pivot.y);
					}
//					pointsX[pointsX.length - 1] = (int)(thing.location.x + p.vertices.get(0).x - minX);
//					pointsY[pointsY.length - 1] = (int)(thing.location.y + p.vertices.get(0).y - minY);
					graphics.drawPolygon(pointsX, pointsY, p.vertices.size());
				}
//				graphics.drawString(thing.brush.export.name.name, (int)thing.location.x - minX, (int)thing.location.y - minY);
//				graphics.drawString(thing.model.export.name.name, (int)thing.location.x - minX, (int)thing.location.y - minY + 15);
				if (brush.pivot != null && brush.brush.export.name.name.equalsIgnoreCase("brush234")) {
					graphics.setColor(Color.RED);
					graphics.drawOval((int)(brush.location.x - minX),
									  (int)(brush.location.y - minY),
									  8, 8);
					graphics.setColor(Color.BLACK);
				}
			}

			ImageIO.write(bigImage, "png", new File("map.png"));
		}
	}

	private Vector translate(Vector point, ScaleProperty postScale, Vector pivot, RotatorProperty rotation) {
		double roll = (((double)rotation.roll / 65536d) * 360d) * (Math.PI / 180);
		double pitch = (((double)rotation.pitch / 65536d) * 360d) * (Math.PI / 180);
		double yaw = (((double)rotation.yaw / (65536d)) * 360d) * (Math.PI / 180); // Convert to radians

		double x = point.x;
		double y = point.y;
		double z = point.z;

		// yaw
		double newX = (Math.cos(yaw) * (x)) - (Math.sin(yaw) * (y));
		y = (Math.sin(yaw) * (x)) + (Math.cos(yaw) * (y));
		x = newX;

		// pitch
		newX = ((x - pivot.x) * Math.cos(pitch)) + (Math.sin(pitch) * (z - pivot.z)) + pivot.x;
		z = (Math.sin(pitch) * (x - pivot.x)) + (Math.cos(pitch) * (z - pivot.z)) + pivot.z;
		x = newX;

//		// roll
		double newY = (Math.cos(roll) * (y - pivot.y)) - (Math.sin(roll) * (z - pivot.z)) + pivot.y;
		z = (Math.sin(roll) * (y - pivot.y)) + (Math.cos(roll) * (z - pivot.z)) + pivot.z;
		y = newY;

		return new Vector((float)x * postScale.x, (float)y * postScale.y, (float)z * postScale.z);
	}

	private Vector translate2(Vector point, ScaleProperty mainScale, ScaleProperty postScale, Vector pivot, RotatorProperty rotation) {
		double roll = (((double)rotation.roll / 65536d) * 360d) * (Math.PI / 180);
		double pitch = (((double)rotation.pitch / 65536d) * 360d) * (Math.PI / 180);
		double yaw = (((double)rotation.yaw / 65536d) * 360d) * (Math.PI / 180); // Convert to radians

		double x = point.x * mainScale.x;
		double y = point.y * mainScale.y;
		double z = point.z * mainScale.z;

		double[] v = { x, y, z };

		// pitch
		x = v[0]; z = v[2];
		v[0] = ((x - pivot.x) * Math.cos(pitch) + (z - pivot.z) * Math.sin(pitch)) + pivot.x;
		v[2] = ((z - pivot.z) * Math.cos(pitch) - (x - pivot.x) * Math.sin(pitch)) + pivot.z;
		// roll
		y = v[1]; z = v[2];
		v[1] = ((y - pivot.y) * Math.cos(roll) - (z - pivot.z) * Math.sin(roll)) + pivot.y;
		v[2] = ((z - pivot.z) * Math.cos(roll) + (y - pivot.y) * Math.sin(roll)) + pivot.z;
		// yaw
		x = v[0]; y = v[1];
		v[0] = ((x - pivot.x) * Math.cos(yaw) - (y - pivot.y) * Math.sin(yaw)) + pivot.x;
		v[1] = ((y - pivot.y) * Math.cos(yaw) + (x - pivot.x) * Math.sin(yaw)) + pivot.y;

		return new Vector((float)v[0] * postScale.x, (float)v[1] * postScale.y, (float)v[2] * postScale.z);
	}
}
