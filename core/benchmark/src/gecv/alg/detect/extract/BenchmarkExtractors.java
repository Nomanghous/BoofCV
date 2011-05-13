/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.extract;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.drawing.impl.ImageInitialization_F32;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkExtractors {

	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static float threshold = 1.0f;
	static long TEST_TIME = 1000;

	static ImageFloat32 intensity;
	static QueueCorner corners;

	static Random rand = new Random(33456);

	public static class FastNonMax extends PerformerBase {
		FastNonMaxCornerExtractor corner = new FastNonMaxCornerExtractor(windowRadius, windowRadius, threshold);

		@Override
		public void process() {
			corners.reset();
			corner.process(intensity, corners);
		}
	}

	public static class NonMax extends PerformerBase {
		NonMaxCornerExtractorNaive corner = new NonMaxCornerExtractorNaive(windowRadius, threshold);

		@Override
		public void process() {
			corners.reset();
			corner.process(intensity, corners);
		}
	}

	public static class Threshold extends PerformerBase {
		ThresholdCornerExtractor corner = new ThresholdCornerExtractor(threshold);

		@Override
		public void process() {
			corners.reset();
			corner.process(intensity, corners);
		}
	}

	public static void main(String args[]) {
		intensity = new ImageFloat32(imgWidth, imgHeight);
		corners = new QueueCorner(imgWidth * imgHeight);

		// have about 1/20 the image below threshold
		ImageInitialization_F32.randomize(intensity, rand, 0, threshold * 20.0f);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		for (int radius = 1; radius < 10; radius += 1) {
			System.out.println("Radius: " + radius);
			System.out.println();
			windowRadius = radius;

			ProfileOperation.printOpsPerSec(new FastNonMax(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new NonMax(), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Threshold(), TEST_TIME);
		}

	}
}