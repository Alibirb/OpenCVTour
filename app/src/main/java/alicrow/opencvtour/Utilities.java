package alicrow.opencvtour;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Created by daniel on 6/2/15.
 *
 * Various utility functions
 *
 * decodeSampledBitmap and calculateInSampleSize adapted from sample code at http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
 */
public class Utilities {

	private static final String TAG = "Utilities";

	/**
	 * Creates and returns a Bitmap of the image at the given filepath, scaled down to fit the area the Bitmap will be displayed in
	 * @param image_file_path location of the image to sample
	 * @param reqWidth width at which the resultant Bitmap will be displayed
	 * @param reqHeight height at which the resultant Bitmap will be displayed
	 * @return a Bitmap large enough to cover the given area
	 */
	public static Bitmap decodeSampledBitmap(String image_file_path, int reqWidth, int reqHeight) {
		Log.v(TAG, "creating bitmap for " + image_file_path);

		final BitmapFactory.Options options = getBitmapBounds(image_file_path);

		options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(image_file_path, options);
	}

	/**
	 * Calculates the sample size to scale the image to be displayed at a given size
	 * @param raw_width raw width of the image
	 * @param raw_height raw height of the image
	 * @param reqWidth the width the image will be displayed at
	 * @param reqHeight the height the image will be displayed at
	 * @return the sample size
	 */
	private static int calculateInSampleSize(int raw_width, int raw_height, int reqWidth, int reqHeight) {
		int inSampleSize = 1;

		if (raw_height > reqHeight || raw_width > reqWidth) {

			final int halfHeight = raw_height / 2;
			final int halfWidth = raw_width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/**
	 * Retrieves the bounds of the given image
	 * @param filepath path to the image file
	 * @return a BitmapFactory.Options object where outWidth and outHeight are the width and height of the image
	 */
	public static BitmapFactory.Options getBitmapBounds(String filepath) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filepath, options);

		return options;
	}

}
