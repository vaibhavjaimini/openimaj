package org.openimaj.image.contour;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimaj.citation.annotation.Reference;
import org.openimaj.citation.annotation.ReferenceType;
import org.openimaj.image.FImage;
import org.openimaj.image.analyser.ImageAnalyser;
import org.openimaj.image.pixel.Pixel;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.IndependentPair;

/**
 * Given a binary image (1-connected and 0-connected regions) detect contours
 * and provide both the contours and a hierarchy of contour membership.
 * 
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * 
 */
@Reference(
		type = ReferenceType.Article,
		author = { "Suzuki, S.", "Abe, K." },
		title = "Topological Structural Analysis of Digitized Binary Image by Border Following",
		year = "1985",
		journal = "Computer Vision, Graphics and Image Processing",
		pages = { "32", "46" },
		month = "January",
		number = "1",
		volume = "30")
public class SuzukiContourProcessor implements ImageAnalyser<FImage> {
	/**
	 * The border type
	 * 
	 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
	 * 
	 */
	public static enum BorderType {
		HOLE,
		OUTER
	}

	/**
	 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
	 * 
	 */
	public static class Border extends Polygon {
		/**
		 * 
		 */
		public BorderType type;
		/**
		 * sub borders
		 */
		public List<Border> children = new ArrayList<Border>();
		/**
		 * The parent border, might be null
		 */
		public Border parent;

		/**
		 * where the border starts
		 */
		public Pixel start;

		/**
		 * @param type
		 */
		private Border(BorderType type) {
			this.type = type;
			this.start = new Pixel(0, 0);
		}

		private Border(BorderType type, int x, int y) {
			this.type = type;
			this.start = new Pixel(x, y);
		}

		private Border(BorderType type, Pixel p) {
			this.type = type;
			this.start = p;
		}

		private Border(int x, int y) {
			this.type = null;
			this.start = new Pixel(x, y);
		}

		private void setParent(Border bp) {
			this.parent = bp;
			bp.children.add(this);

		}

		private void sanityCheck() {
//			System.out.println("THIS: " + this);
//			System.out.println("PARENT: " + parent);
			if(this.points.size() == 0){
				throw new RuntimeException("NO POINTS! this cannot happen");
			}
			Rectangle childBB = this.calculateRegularBoundingBox();
			Rectangle rootBB = parent.calculateRegularBoundingBox();
//			System.out.println("Root bb: " + rootBB);
//			System.out.println("child bb: " + childBB);
			if(!rootBB.isInside(childBB)){
				throw new RuntimeException("THE CHILD WAS NOT INSIDE THE ROOT!");
			}
		}

		@Override
		public String toString() {
			final StringWriter border = new StringWriter();
			final PrintWriter pw = new PrintWriter(border);
			pw.println(String.format("[%s] start: %s %s", this.type, this.start,this.points));
			for (final Border child : this.children) {
				pw.print(child);
			}
			pw.flush();
			return border.toString();
		}

	}

	/**
	 * the root border detected
	 */
	public Border root;

	@Override
	public void analyseImage(final FImage image) {
		this.root = findContours(image.clone());
	}

	/**
	 * 
	 * @param image
	 * @return Detect borders hierarcically in this binary image. Note the image
	 *         is changed while borders are found
	 */
	public static Border findContours(final FImage image) {
		final float nbd[] = new float[] { 1 };
		final float lnbd[] = new float[] { 1 };
		// Prepare the special outer frame
		final Border root = new Border(BorderType.HOLE);
		Rectangle bb = image.getBounds();
		root.points.addAll(bb.asPolygon().getVertices());
		
		final Map<Float, Border> borderMap = new HashMap<Float, Border>();
		borderMap.put(lnbd[0], root);
		final SuzukiNeighborStrategy borderFollow = new SuzukiNeighborStrategy();
		
		for (int i = 0; i < image.height; i++) {
			lnbd[0] = 1; // Beggining of appendix 1, this is the beggining of a scan
			for (int j = 0; j < image.width; j++) {
				float fji = image.getPixel(j,i);
				final boolean isOuter = isOuterBorderStart(image, i, j); // check 1(a)
				final boolean isHole = isHoleBorderStart(image, i, j); // check 1(b)
				if (isOuter || isHole){ // check 1(c)
					final Border border = new Border(j, i);
					Border borderPrime = null;
					final Pixel from = new Pixel(j, i);
					if (isOuter) {
						nbd[0] += 1; // in 1(a)  we increment NBD
						from.x -= 1;
						border.type = BorderType.OUTER;
						borderPrime = borderMap.get(lnbd[0]);
						// the check of table 1
						switch (borderPrime.type) {
						case OUTER:
							border.setParent(borderPrime.parent);
							break;
						case HOLE:
							border.setParent(borderPrime);
							break;
						}
					}
					else {
						nbd[0] += 1; // in 1(b)  we increment NBD
						// according to 1(b) we set lnbd to the pixel value if it is greater than 1 
						if(fji > 1) lnbd[0]= fji;
						borderPrime = borderMap.get(lnbd[0]);
						from.x += 1;
						border.type = BorderType.HOLE;
						// the check of table 1
						switch (borderPrime.type) {
						case OUTER:
							border.setParent(borderPrime);
							break;
						case HOLE:
							border.setParent(borderPrime.parent);
							break;
						}
					}
					Pixel ij = new Pixel(j, i);
					borderFollow.directedBorder(image, ij, from,
						new Operation<IndependentPair<Pixel, Set<Pixel>>>() {
	
							@Override
							public void perform(IndependentPair<Pixel, Set<Pixel>> object) {
								final Pixel p = object.firstObject();
								final Set<Pixel> d = object.secondObject();
								border.points.add(p);
								if (crossesEastBorder(image, d, p)) {
									image.setPixel(p.x, p.y, -nbd[0]);
								} else if (image.getPixel(p) == 1f) {
									// only set if the pixel has not been
									// visited before 3.4(b)!
									image.setPixel(p.x, p.y, nbd[0]);
								} // otherwise leave it alone!
							}
	
						});
					// this is 3.1, if no borders were given, this means this is a pixel on its own, so we set it to -nbd
					if(border.points.size() == 0){
						border.points.add(ij);
						image.setPixel(j, i, -nbd[0]);
					} 
					
					border.sanityCheck();
					borderMap.put(nbd[0], border);
				}
				// This is step (4)
				if(fji != 0 && fji != 1) lnbd[0] = Math.abs(fji);
				

			}
		}
		return root;
	}

	private static boolean crossesEastBorder(final FImage image, Set<Pixel> d, final Pixel p) {
		Pixel nextP = new Pixel(p.x + 1, p.y);
		return image.getPixel(p) != 0 && (p.x == image.width - 1 || (d.contains(nextP)));// this is 3.4(a) with an edge case check
	}

	private static boolean isOuterBorderStart(FImage image, int i, int j) {
		return (image.pixels[i][j] == 1 && (j == 0 || image.pixels[i][j - 1] == 0));
	}

	private static boolean isHoleBorderStart(FImage image, int i, int j) {
		return (image.pixels[i][j] >= 1 && (j == image.width - 1 || image.pixels[i][j + 1] == 0));
	}

}