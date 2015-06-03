package alicrow.opencvtour;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by daniel on 6/2/15.
 * decodeSampledBitmap and calculateInSampleSize taken from sample code at http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
 */
public class Utilities {

	public static Bitmap decodeSampledBitmap(String image_file_path, int reqWidth, int reqHeight) {
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(image_file_path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(image_file_path, options);
	}

	/**
	 * Calculates the sample size to scale the image to displayed at a given size
	 * @param options information on the image
	 * @param reqWidth the width the image will be displayed at
	 * @param reqHeight the height the image will be displayed at
	 * @return the sample size
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}


	public static BitmapFactory.Options getBitmapBounds(String filepath) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filepath, options);

		return options;
	}

}
