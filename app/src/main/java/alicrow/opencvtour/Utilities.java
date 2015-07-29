/*
 * Copyright 2015 Lafayette College
 *
 * This file is part of OpenCVTour.
 *
 * OpenCVTour is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCVTour is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenCVTour.  If not, see <http://www.gnu.org/licenses/>.
 */

package alicrow.opencvtour;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by daniel on 6/2/15.
 *
 * Various utility functions
 *
 * Most of the image-loading code was adapted from sample code at http://developer.android.com/training/displaying-bitmaps/index.html, which was licensed under the Apache 2.0 License.
 */
public class Utilities {

	private static final String TAG = "Utilities";

	public static final int REQUEST_IMAGE_CAPTURE = 1;

	private static final LruCache<String, Bitmap> _bitmap_cache = new LruCache<>(32);

	/**
	 * Creates and returns a Bitmap of the image at the given filepath, scaled down to fit the area the Bitmap will be displayed in
	 * @param image_file_path location of the image to sample
	 * @param reqWidth width at which the resultant Bitmap will be displayed
	 * @param reqHeight height at which the resultant Bitmap will be displayed
	 * @return a Bitmap large enough to cover the given area
	 */
	public static Bitmap decodeSampledBitmap(String image_file_path, int reqWidth, int reqHeight) {
		Log.v(TAG, "creating bitmap for " + image_file_path);

		long start = android.os.SystemClock.uptimeMillis();

		final BitmapFactory.Options options = getBitmapBounds(image_file_path);

		options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;

		Bitmap sampled = BitmapFactory.decodeFile(image_file_path, options);
		long sampled_time = android.os.SystemClock.uptimeMillis();
		Log.v(TAG, "created sampled bitmap for " + image_file_path + ", took " + (sampled_time-start) + "ms");

		Bitmap oriented = fixOrientation(sampled, image_file_path);
		long orientation_fixed_time = android.os.SystemClock.uptimeMillis();
		Log.v(TAG, "fixed orientation for " + image_file_path + ", took " + (orientation_fixed_time-sampled_time) + "ms");

		Bitmap final_bitmap = ThumbnailUtils.extractThumbnail(oriented, reqWidth, reqHeight);
		long end = android.os.SystemClock.uptimeMillis();
		Log.v(TAG, "finished resizing bitmap for " + image_file_path + ", took " + (end-orientation_fixed_time) + "ms");
		Log.v(TAG, "finished creating bitmap for " + image_file_path + ", took " + (end-start) + "ms");

		return final_bitmap;
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

	/**
	 * creates a new Bitmap rotated according to the orientation specified in a source file
	 * @param image the Bitmap to rotate
	 * @param image_file_path the file containing the Bitmap's source
	 * @return a new Bitmap with the correct rotation
	 */
	public static Bitmap fixOrientation(Bitmap image, String image_file_path) {
		try {
			/// Retrieve orientation info from the file
			ExifInterface exif = new ExifInterface(image_file_path);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			float rotation;
			switch (orientation) {
				case ExifInterface.ORIENTATION_NORMAL:
					rotation = 0;
					break;
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotation = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					rotation = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotation = 270;
					break;
				default:
					rotation = 0;
					break;
			}
			return rotateImage(image, rotation);
		} catch(IOException e) {
			Log.w(TAG, "Unable to open '" + image_file_path + "' to read orientation");
			return image;
		}
	}

	/**
	 * Returns a rotated version of a Bitmap
	 * @param image the image to rotate
	 * @param angle the amount to rotate the image by, in degrees
	 * @return a new Bitmap with the correct rotation
	 */
	public static Bitmap rotateImage(Bitmap image, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
	}

	/**
	 * Contains the parameters used to generate a smaller Bitmap from a larger image.
	 */
	public static class ReducedBitmapInfo {
		public String full_image_filename;
		public int width;
		public int height;

		public ReducedBitmapInfo(String filename, int width, int height) {
			this.full_image_filename = filename;
			this.width = width;
			this.height = height;
		}

		/// Using ReducedBitmapInfo as a key in our cache doesn't work (no clue why), so we use Strings instead.
		@Override
		public String toString() {
			return full_image_filename + " " + width + " " + height;
		}
	}

	public static void addToCache(ReducedBitmapInfo info, Bitmap bitmap) {
		if(_bitmap_cache.get(info.toString()) == null) {
			Log.v(TAG, "Adding bitmap to cache");
			_bitmap_cache.put(info.toString(), bitmap);
		}
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	public static void loadBitmap(ImageView view, String filename, int width, int height, Context context) {
		ReducedBitmapInfo info = new ReducedBitmapInfo(filename, width, height);
		Log.v(TAG, "loading bitmap " + info.toString());

		Bitmap bitmap = _bitmap_cache.get(info.toString());
		if(bitmap != null) {
			Log.v(TAG, "bitmap already created");
			view.setImageBitmap(bitmap);
		} else {

			File cached_image_file = new File(context.getExternalCacheDir(), info.toString().substring(Tour.getToursDirectory(context).getPath().length()) + ".jpg");
			cached_image_file.getParentFile().mkdir();

			Utilities.BitmapWorkerTask task = new Utilities.BitmapWorkerTask(view, width, height, cached_image_file);

			Bitmap placeholder_bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.loading_thumbnail);

			final AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(), placeholder_bitmap, task);
			view.setImageDrawable(asyncDrawable);
			task.execute(filename);
		}
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	public static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private int _width, _height;
		private File _cached_image_file;

		public BitmapWorkerTask(ImageView imageView, int width, int height, File cached_image_file) {
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<>(imageView);
			Log.v(TAG, "Creating BitmapWorkerTask");
			_width = width;
			_height = height;
			_cached_image_file = cached_image_file;
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(String... params) {
			try {
				String image_filepath = params[0];
				Bitmap image;

				if(_cached_image_file != null && _cached_image_file.exists()) {
					Log.v(TAG, "loading cached bitmap file " + _cached_image_file.getPath());
					long start = android.os.SystemClock.uptimeMillis();
					image = BitmapFactory.decodeFile(_cached_image_file.getAbsolutePath());
					long end = android.os.SystemClock.uptimeMillis();
					Log.v(TAG, "finished loading cached bitmap file " + _cached_image_file.getPath() + ", took " + (end-start) + "ms");
				} else {
					image = Utilities.decodeSampledBitmap(image_filepath, _width, _height);

					/// Save this thumbnail to disk, so we don't need to run sampling for it again.
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(_cached_image_file);
						image.compress(Bitmap.CompressFormat.JPEG, 97, out);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (out != null) {
								out.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				addToCache(new ReducedBitmapInfo(image_filepath, _width, _height), image);
				return image;
			} catch(Exception e) {
				Log.e(TAG, e.toString());
				return null;
			}
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				Log.d(TAG, "BitmapWorkerTask was canceled");
				bitmap.recycle();
				bitmap = null;
			}

			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
				if (this == bitmapWorkerTask && imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}

	/**
	 * Converts a size in density-independent pixels to the actual pixel value on this device.
	 * @param dp size in density-independent pixels
	 * @return actual pixel value on this device
	 */
	public static int dp_to_px(float dp) {
		final float scale = Resources.getSystem().getDisplayMetrics().density;
		return (int)( dp * scale + 0.5f);
	}

	public static Uri createImageFile(Context context, boolean is_temp) throws IOException {
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File file;
		if(is_temp)
			file = new File(context.getExternalCacheDir(), "temp" + ".jpg");
		else
			file = new File(Tour.getCurrentTour().getDirectory(), timestamp + ".jpg");
		return Uri.fromFile(file);
	}

	public static Uri takePicture(Activity activity, boolean is_temp) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Uri photo_uri = null;
		if (intent.resolveActivity(activity.getPackageManager()) != null) {
			// Create a file to save the photo to
			try {
				photo_uri = Utilities.createImageFile(activity, is_temp);
			} catch (IOException ex) {
				Log.e(TAG, ex.toString());
			}

			if (photo_uri != null) {
				intent.putExtra(MediaStore.EXTRA_OUTPUT, photo_uri);
				activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
				Log.i(TAG, "started image capture request");
			} else {
				Log.e(TAG, "failed to create file for image");
			}
		}
		return photo_uri;
	}

	public static void compressFolder(String input_path, String output_path, boolean skip_images) {
		try {
			FileOutputStream fos = new FileOutputStream(output_path);
			ZipOutputStream zos = new ZipOutputStream(fos);
			File srcFile = new File(input_path);
			File[] files = srcFile.listFiles();
			Log.d(TAG, "Zip directory: " + srcFile.getName());
			for (File file : files) {
				if(skip_images && file.getName().endsWith(".jpg")) {
					Log.d(TAG, "Skipping image file " + file.getName());
				} else {
					Log.d(TAG, "Adding file: " + file.getName());
					byte[] buffer = new byte[1024];
					FileInputStream fis = new FileInputStream(file);
					zos.putNextEntry(new ZipEntry(file.getName()));
					int length;
					while ((length = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, length);
					}
					zos.closeEntry();
					fis.close();
				}
			}
			zos.close();
		} catch (IOException ioe) {
			Log.e(TAG, ioe.getMessage());
		}
	}
	public static void extractFolder(String input_path, String output_path) {
		try {
			extractFolder(new FileInputStream(input_path), output_path);
		} catch(Exception ex) {
			Log.e(TAG, ex.getMessage());
		}
	}
	public static void extractFolder(InputStream is, String output_path) {
		try {
			File output_folder = new File(output_path);
			if(!output_folder.exists())
				output_folder.mkdir();

			if(!output_path.endsWith("/"))
				output_path = output_path + "/";

			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
			ZipEntry ze;
			byte[] buffer = new byte[1024];
			int count;
			String filename;
			while ((ze = zis.getNextEntry()) != null) {
				filename = ze.getName();
				Log.d(TAG, "Extracting file " + filename);

				if (ze.isDirectory()) {
					File fmd = new File(output_path + filename);
					fmd.mkdirs();
					continue;
				}

				FileOutputStream fout = new FileOutputStream(output_path + filename);

				while ((count = zis.read(buffer)) != -1) {
					fout.write(buffer, 0, count);
				}

				fout.close();
				zis.closeEntry();
			}

			zis.close();
		} catch (IOException ioe) {
			Log.e(TAG, ioe.getMessage());
		}
	}

}
