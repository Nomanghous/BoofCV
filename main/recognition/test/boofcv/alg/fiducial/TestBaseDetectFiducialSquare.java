/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.fiducial;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestBaseDetectFiducialSquare {

	/**
	 * Runs the entire processing loop with a very simple oriented fiducial.  Makes sure that the found projection
	 * from fiducial to camera is consistent by transforming one of the corners and seeing if it ends up
	 * in the correct location.
	 */
	@Test
	public void checkFoundOrientation() {
		List<Point2D_F64> expected = new ArrayList<Point2D_F64>();
		expected.add( new Point2D_F64(200,300+120));
		expected.add( new Point2D_F64(200,300));
		expected.add( new Point2D_F64(200+120,300));
		expected.add( new Point2D_F64(200+120,300+120));

		IntrinsicParameters intrinsic =new IntrinsicParameters(500,500,0,320,240,640,480);

		// create a pattern with a corner for orientation and put it into the image
		ImageUInt8 pattern = createPattern(6*20, true);

		ImageUInt8 image = new ImageUInt8(640,480);

		for (int i = 0; i < 4; i++) {

			ImageMiscOps.fill(image, 255);

			image.subimage(200, 300, 200 + pattern.width, 300 + pattern.height, null).setTo(pattern);

			DetectCorner detector = new DetectCorner();

			detector.configure(intrinsic, false);
			detector.process(image);

			assertEquals(1, detector.getFound().size());
			FoundFiducial ff = detector.getFound().get(0);

			// make sure the returned quadrilateral makes sense
			for (int j = 0; j < 4; j++) {
				int index = j-i;
				if( index < 0) index = 4 + index;
				Point2D_F64 f = ff.location.get(index);
				Point2D_F64 e = expected.get((j+1)%4);
				assertTrue(f.distance(e) <= 1e-8 );
			}

			// lower left hand corner in the fiducial.  side is of length 2
			Point3D_F64 lowerLeft = new Point3D_F64(-1, -1, 0);
			Point3D_F64 cameraPt = new Point3D_F64();
			SePointOps_F64.transform(ff.targetToSensor, lowerLeft, cameraPt);
			Point2D_F64 pixelPt = new Point2D_F64();
			PerspectiveOps.convertNormToPixel(intrinsic, cameraPt.x / cameraPt.z, cameraPt.y / cameraPt.z, pixelPt);

//			System.out.println("pixel = " + pixelPt);
			// see if that point projects into the correct location
			assertEquals(expected.get(i).x, pixelPt.x, 1e-4);
			assertEquals(expected.get(i).y, pixelPt.y, 1e-4);

			ImageMiscOps.rotateCW(pattern);
		}
	}

	/**
	 * Basic test where a distorted pattern is places in the image and the found coordinates
	 * are compared against ground truth
	 */
	@Test
	public void findPatternEasy() {
		checkFindKnown(new IntrinsicParameters(500,500,0,320,240,640,480),1.1);
	}

	private void checkFindKnown(IntrinsicParameters intrinsic, double tol ) {
		ImageUInt8 pattern = createPattern(6*20, false);
		Quadrilateral_F64 where = new Quadrilateral_F64(50,50,  130,60,  140,150,  40,140);
//		Quadrilateral_F64 where = new Quadrilateral_F64(50,50,  100,50,  100,150,  50,150);

		ImageUInt8 image = new ImageUInt8(640,480);
		ImageMiscOps.fill(image, 255);
		render(pattern, where, image);

		Dummy dummy = new Dummy();
		dummy.configure(intrinsic,false);
		dummy.process(image);

		assertEquals(1,dummy.detected.size());

		Quadrilateral_F64 found = dummy.getFound().get(0).location;
//		System.out.println("found "+found);
//		System.out.println("where "+where);
		checkMatch(where, found.a, tol);
		checkMatch(where, found.b, tol);
		checkMatch(where, found.c, tol);
		checkMatch(where, found.d, tol);

		// see if the undistorted image is as expected
		checkPattern( dummy.detected.get(0) );
	}

	private void checkMatch( Quadrilateral_F64 q , Point2D_F64 p , double tol ) {

		if( q.a.distance(p) <= tol)
			return;
		if( q.b.distance(p) <= tol)
			return;
		if( q.c.distance(p) <= tol)
			return;
		if( q.d.distance(p) <= tol)
			return;
		fail("no match "+p+"    "+q);
	}

	@Test
	public void computeTargetToWorld() {

		double lengthSide = 0.5;
		IntrinsicParameters intrinsic = new IntrinsicParameters(400,400,0,320,240,640,380);
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic,null);

		Dummy alg = new Dummy();
		alg.configure(intrinsic,false);

		Se3_F64 targetToWorld = new Se3_F64();
		targetToWorld.getT().set(0.1,-0.07,1.5);
		RotationMatrixGenerator.eulerXYZ(0.03,0.1,0,targetToWorld.getR());

		Quadrilateral_F64 quad = new Quadrilateral_F64();

		double r = lengthSide/2.0;
		quad.a = PerspectiveOps.renderPixel(targetToWorld,K,c(new Point2D_F64(-r, r)));
		quad.b = PerspectiveOps.renderPixel(targetToWorld,K,c(new Point2D_F64( r, r)));
		quad.c = PerspectiveOps.renderPixel(targetToWorld,K,c(new Point2D_F64( r,-r)));
		quad.d = PerspectiveOps.renderPixel(targetToWorld,K,c(new Point2D_F64(-r,-r)));

		Se3_F64 found = new Se3_F64();
		alg.computeTargetToWorld(quad, 0.5, found);

		assertTrue(MatrixFeatures.isIdentical(targetToWorld.getR(), found.getR(), 1e-6));
		assertEquals(0,targetToWorld.getT().distance(found.getT()),1e-6);
	}

	private static Point3D_F64 c( Point2D_F64 a ) {
		return new Point3D_F64(a.x,a.y,0);
	}

	/**
	 * Sees if perspective distortion is correctly removed from square regions in the image
	 * which are smaller than the internal square.
	 *
	 * Earlier versions used bilinear interpolation which introduced artifacts in this situation.
	 */
	@Test
	public void perspectiveRemoval_smallRegion() {
		fail("implement");
	}

	/**
	 * Sees if perspective distortion is correctly removed from square regions in the image
	 * which are larger than the internal square
	 */
	@Test
	public void perspectiveRemoval_largeRegion() {
		fail("implement");
	}

	/**
	 * Ensure that lens distortion is being removed from the fiducial square.
	 */
	@Test
	public void lensRemoval() {
		fail("implement");
	}

	/**
	 * Creates a square pattern image of the specified size
	 *
	 * @param squareLowLeft If true a square will be added to the image lower left
	 */
	public static ImageUInt8 createPattern(int length, boolean squareLowLeft) {
		ImageUInt8 pattern = new ImageUInt8( length , length );

		if( length%6 != 0 )
			throw new RuntimeException("Must be divisible by 6!");

		int b = length/6;

		for (int y = 0; y < length; y++) {
			for (int x = 0; x < length; x++) {
				int color = (x < b || y < b || x >= length-b || y >= length-b ) ? 0 : 255;
				pattern.set(x,y,color);
			}
		}

		if( squareLowLeft ) {
			for (int y = 0; y < 2*b; y++) {
				for (int x = 0; x < 2*b; x++) {
					pattern.set(x + b, y + 3*b, 0);
				}
			}
		}

		return pattern;
	}

	public static void checkPattern( ImageFloat32 image ) {

		int x0 = image.width/6;
		int y0 = image.height/6;
		int x1 = image.width-x0;
		int y1 = image.height-y0;

		double totalBorder = 0;
		int countBorder = 0;
		double totalInner = 0;
		int countInner = 0;

		// the border regions can be ambiguous so sum up around them
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if( x < (x0-1) || x >= (x1+1) || y < (y0-1) || y >= (y1+1) ) {
					totalBorder += image.get(x,y);
					countBorder++;
				} else if( x >= (x0+1) && x < (x1-1) && y >= (y0+1) && y < (y1-1) ) {
					totalInner += image.get(x,y);
					countInner++;
				}
			}
		}

		totalBorder /= countBorder;
		totalInner /= countInner;

		assertTrue( totalBorder < 15 );
		assertTrue( totalInner > 245 );
	}

	/**
	 * Draws a distorted pattern onto the output
	 */
	public static void render( ImageUInt8 pattern , Quadrilateral_F64 where , ImageUInt8 output ) {

		int w = pattern.width;
		int h = pattern.height;

		ArrayList<AssociatedPair> associatedPairs = new ArrayList<AssociatedPair>();
		associatedPairs.add(new AssociatedPair(where.a,new Point2D_F64(0,0)));
		associatedPairs.add(new AssociatedPair(where.b,new Point2D_F64(w,0)));
		associatedPairs.add(new AssociatedPair(where.c,new Point2D_F64(w,h)));
		associatedPairs.add(new AssociatedPair(where.d,new Point2D_F64(0,h)));

		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

		DenseMatrix64F H = new DenseMatrix64F(3,3);
		computeHomography.process(associatedPairs, H);

		// Create the transform for distorting the image
		PointTransformHomography_F32 homography = new PointTransformHomography_F32(H);
		PixelTransform_F32 pixelTransform = new PointToPixelTransform_F32(homography);

		// Apply distortion and show the results
		DistortImageOps.distortSingle(pattern, output, pixelTransform, null, TypeInterpolate.BILINEAR);
	}

	public static class Dummy extends BaseDetectFiducialSquare<ImageUInt8> {

		public List<ImageFloat32> detected = new ArrayList<ImageFloat32>();

		protected Dummy() {
			super(FactoryShapeDetector.polygon(FactoryThresholdBinary.globalFixed(50,true,ImageUInt8.class),
					new ConfigPolygonDetector(4,false),ImageUInt8.class),100, ImageUInt8.class);
		}

		@Override
		public boolean processSquare(ImageFloat32 square, Result result) {
			detected.add(square.clone());
			return true;
		}
	}

	/**
	 * Accepts the pattern when it's in the lower left corner
	 */
	public static class DetectCorner extends BaseDetectFiducialSquare<ImageUInt8> {
		protected DetectCorner() {
			super(FactoryShapeDetector.polygon(FactoryThresholdBinary.globalFixed(50, true, ImageUInt8.class),
					new ConfigPolygonDetector(4,false),ImageUInt8.class),100, ImageUInt8.class);
		}

		@Override
		public boolean processSquare(ImageFloat32 square, Result result) {

			int w2 = square.width/2;
			int h2 = square.height/2;
			int w = square.width;
			int h = square.height;

			float sum[] = new float[4];
			sum[0] = ImageStatistics.sum(square.subimage(0,0,w2,h2,null));
			sum[1] = ImageStatistics.sum(square.subimage(w2,0,w,h2,null));
			sum[2] = ImageStatistics.sum(square.subimage(w2,h2,w,h,null));
			sum[3] = ImageStatistics.sum(square.subimage(0,h2,w2,h,null));

			int indexMin = -1;
			float min = Float.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				if( sum[i] < min ) {
					min = sum[i];
					indexMin = i;
				}
			}

			result.lengthSide = 2.0;
			// number of image clockwise rotations to put min in lower-left corner
			result.rotation = 3-indexMin;
			return true;
		}
	}
}