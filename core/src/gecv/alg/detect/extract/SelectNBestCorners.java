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

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;
import pja.geometry.struct.point.Point2D_I16;
import pja.sorting.QuickSelectF;


/**
 * This extractor selects N best results from the extractor is is wrapped around.
 *
 * @author Peter Abeles
 */
public class SelectNBestCorners {

	// list of the found best corners
	QueueCorner bestCorners;
	int indexes[];
	float inten[];

	public SelectNBestCorners(int maxCorners) {
		bestCorners = new QueueCorner(maxCorners);
		indexes = new int[maxCorners];
		inten = new float[maxCorners];
	}

	public void process(ImageFloat32 intensityImage, QueueCorner origCorners) {
		final int numFoundFeatures = origCorners.num;
		bestCorners.reset();

		if (numFoundFeatures <= bestCorners.getMaxSize()) {
			// make a copy of the results with no pruning since it already
			// has the desired number, or less
			for (int i = 0; i < numFoundFeatures; i++) {
				Point2D_I16 pt = origCorners.points[i];
				bestCorners.add(pt.x, pt.y);
			}
		} else {
			// prune the list after finding the N best using quick select

			// grow internal data structures
			if (numFoundFeatures > inten.length) {
				inten = new float[numFoundFeatures];
				indexes = new int[numFoundFeatures];
			}

			// extract the intensities for each corner
			Point2D_I16[] points = origCorners.points;
			int size = origCorners.size();

			for (int i = 0; i < size; i++) {
				Point2D_I16 pt = points[i];
				// quick select selects the k smallest
				// I want the k-biggest so the negative is used
				inten[i] = -intensityImage.get(pt.getX(), pt.getY());

			}

			QuickSelectF.selectIndex(inten, bestCorners.getMaxSize(), size, indexes);

			for (int i = 0; i < bestCorners.getMaxSize(); i++) {
				Point2D_I16 pt = origCorners.points[indexes[i]];
				bestCorners.add(pt.x, pt.y);
			}
		}
	}

	public QueueCorner getBestCorners() {
		return bestCorners;
	}
}