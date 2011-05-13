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

package gecv.alg.filter.convolve.normalized;

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * Covolves a 1D kernel in the horizontal or vertical direction across an image's border only while re-normalizing the
 * kernel sum to one.
 * </p>
 * 
 * <p>
 * NOTE: Do not modify.  Automatically generated by {@link GenerateConvolveNormalizedBorderNaive}.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ConvolveNormalized_JustBorder {

	/**
	 * Performs a horizontal 1D convolution across the image's vertical border
	 *
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 */
	public static void horizontal(Kernel1D_F32 kernel, ImageFloat32 input, ImageFloat32 output ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;
		final float[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int width = input.getWidth();
		final int height = input.getHeight();

		for (int i = 0; i < height; i++) {
			int indexDest = output.startIndex + i * output.stride;
			int j = input.startIndex + i * input.stride;
			final int jStart = j;
			int jEnd = j + radius;

			for (; j < jEnd; j++) {
				float total = 0;
				float totalWeight = 0;
				int indexSrc = jStart;
				for (int k = kernelWidth - (radius + 1 + j - jStart); k < kernelWidth; k++) {
					float w = dataKer[k];
					totalWeight += w;
					total += (dataSrc[indexSrc++]) * w;
				}
				dataDst[indexDest++] = (total / totalWeight);
			}

			j += width - 2*radius;
			indexDest += width - 2*radius;

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				float total = 0;
				float totalWeight = 0;
				int indexSrc = j - radius;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					float w = dataKer[k];
					totalWeight += w;
					total += (dataSrc[indexSrc++]) * w;
				}
				dataDst[indexDest++] = (total / totalWeight);
			}
		}
	}

	/**
	 * Performs a vertical 1D convolution across the image's horizontal border
	 *
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 */
	public static void vertical(Kernel1D_F32 kernel, ImageFloat32 input, ImageFloat32 output ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;
		final float[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - radius;

		for (int y = 0; y < radius; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int i = input.startIndex + y * input.stride;
			final int iEnd = i + imgWidth;

			int kStart = radius - y;

			float weight = 0;
			for (int k = kStart; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for ( ; i < iEnd; i++) {
				float total = 0;
				int indexSrc = i - y * input.stride;
				for (int k = kStart; k < kernelWidth; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]) * dataKer[k];
				}
				dataDst[indexDst++] = (total / weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int i = input.startIndex + y * input.stride;
			final int iEnd = i + imgWidth;

			int kEnd = imgHeight - (y - radius);

			float weight = 0;
			for (int k = 0; k < kEnd; k++) {
				weight += dataKer[k];
			}

			for ( ; i < iEnd; i++) {
				float total = 0;
				int indexSrc = i - radius * input.stride;
				for (int k = 0; k < kEnd; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]) * dataKer[k];
				}
				dataDst[indexDst++] = (total / weight);
			}
		}
	}

	/**
	 * Performs a horizontal 1D convolution across the image's vertical border
	 *
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, ImageUInt8 input, ImageUInt8 output ) {
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int width = input.getWidth();
		final int height = input.getHeight();

		for (int i = 0; i < height; i++) {
			int indexDest = output.startIndex + i * output.stride;
			int j = input.startIndex + i * input.stride;
			final int jStart = j;
			int jEnd = j + radius;

			for (; j < jEnd; j++) {
				int total = 0;
				int totalWeight = 0;
				int indexSrc = jStart;
				for (int k = kernelWidth - (radius + 1 + j - jStart); k < kernelWidth; k++) {
					int w = dataKer[k];
					totalWeight += w;
					total += (dataSrc[indexSrc++] & 0xFF) * w;
				}
				dataDst[indexDest++] = (byte)(total / totalWeight);
			}

			j += width - 2*radius;
			indexDest += width - 2*radius;

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				int total = 0;
				int totalWeight = 0;
				int indexSrc = j - radius;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					int w = dataKer[k];
					totalWeight += w;
					total += (dataSrc[indexSrc++] & 0xFF) * w;
				}
				dataDst[indexDest++] = (byte)(total / totalWeight);
			}
		}
	}

	/**
	 * Performs a vertical 1D convolution across the image's horizontal border
	 *
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, ImageUInt8 input, ImageUInt8 output ) {
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - radius;

		for (int y = 0; y < radius; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int i = input.startIndex + y * input.stride;
			final int iEnd = i + imgWidth;

			int kStart = radius - y;

			int weight = 0;
			for (int k = kStart; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for ( ; i < iEnd; i++) {
				int total = 0;
				int indexSrc = i - y * input.stride;
				for (int k = kStart; k < kernelWidth; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = (byte)(total / weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int i = input.startIndex + y * input.stride;
			final int iEnd = i + imgWidth;

			int kEnd = imgHeight - (y - radius);

			int weight = 0;
			for (int k = 0; k < kEnd; k++) {
				weight += dataKer[k];
			}

			for ( ; i < iEnd; i++) {
				int total = 0;
				int indexSrc = i - radius * input.stride;
				for (int k = 0; k < kEnd; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = (byte)(total / weight);
			}
		}
	}

	/**
	 * Performs a horizontal 1D convolution across the image's vertical border
	 *
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, ImageSInt16 input, ImageSInt16 output ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int width = input.getWidth();
		final int height = input.getHeight();

		for (int i = 0; i < height; i++) {
			int indexDest = output.startIndex + i * output.stride;
			int j = input.startIndex + i * input.stride;
			final int jStart = j;
			int jEnd = j + radius;

			for (; j < jEnd; j++) {
				int total = 0;
				int totalWeight = 0;
				int indexSrc = jStart;
				for (int k = kernelWidth - (radius + 1 + j - jStart); k < kernelWidth; k++) {
					int w = dataKer[k];
					totalWeight += w;
					total += (dataSrc[indexSrc++]) * w;
				}
				dataDst[indexDest++] = (short)(total / totalWeight);
			}

			j += width - 2*radius;
			indexDest += width - 2*radius;

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				int total = 0;
				int totalWeight = 0;
				int indexSrc = j - radius;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					int w = dataKer[k];
					totalWeight += w;
					total += (dataSrc[indexSrc++]) * w;
				}
				dataDst[indexDest++] = (short)(total / totalWeight);
			}
		}
	}

	/**
	 * Performs a vertical 1D convolution across the image's horizontal border
	 *
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, ImageSInt16 input, ImageSInt16 output ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - radius;

		for (int y = 0; y < radius; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int i = input.startIndex + y * input.stride;
			final int iEnd = i + imgWidth;

			int kStart = radius - y;

			int weight = 0;
			for (int k = kStart; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for ( ; i < iEnd; i++) {
				int total = 0;
				int indexSrc = i - y * input.stride;
				for (int k = kStart; k < kernelWidth; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]) * dataKer[k];
				}
				dataDst[indexDst++] = (short)(total / weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int i = input.startIndex + y * input.stride;
			final int iEnd = i + imgWidth;

			int kEnd = imgHeight - (y - radius);

			int weight = 0;
			for (int k = 0; k < kEnd; k++) {
				weight += dataKer[k];
			}

			for ( ; i < iEnd; i++) {
				int total = 0;
				int indexSrc = i - radius * input.stride;
				for (int k = 0; k < kEnd; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]) * dataKer[k];
				}
				dataDst[indexDst++] = (short)(total / weight);
			}
		}
	}

}