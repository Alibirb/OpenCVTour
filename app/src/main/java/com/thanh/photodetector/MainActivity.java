package com.thanh.photodetector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import alicrow.opencvtour.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;


public class MainActivity extends Activity implements CvCameraViewListener2 {
	// tag of messages printed to LogCat
    protected static final String TAG = "MainActivity";
    
    // tag of Error messages printed to LogCat
    protected static final String ERROR = "Error in MainActivity";
    
    // Whether an asynchronous menu action is in progress.
 	// If so, menu interaction should be disabled.
 	private boolean mIsMenuLocked;
 	
 	// Whether the next camera frame should be saved as a photo.   
 	private boolean mIsTakingPhoto;  

 	// Whether the object in next camera frame should be detected
 	private boolean mIsObjectDetecting;
 	
 	// Whether the library of training images is being loaded
 	private boolean mIsLoadingLib;
 	
 	// A camera object that allows the app to access the device's camera
    private CameraBridgeViewBase mOpenCvCameraView;    
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.photo_detector_layout);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.PhotoDetectorView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
    	if(mIsMenuLocked){
    		Log.i(TAG, "onOptionsItemSelected. mIsMenuLocked:" + mIsMenuLocked);
    		return true;
    	}
    	Log.i(TAG, "onOptionsSelected.menu is not locked");
        int id = item.getItemId();
        switch(id){
        case R.id.action_settings:
            return true;
        case R.id.menu_take_photo:
        	mIsMenuLocked= true;        	
        	//Next frame, take the photo
        	mIsTakingPhoto = true;        	
        	return true;    
        case R.id.menu_detect_object:	
        	mIsMenuLocked= true;      	
        	//Next frame, detect the photo
        	mIsObjectDetecting =true;
        	return true;
        case R.id.menu_load_library:
        	mIsLoadingLib= true;
        	return true;
        default:
        	return super.onOptionsItemSelected(item);
        }
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat rgba= inputFrame.rgba();
    	
    	if(mIsTakingPhoto){
    		mIsTakingPhoto= false;  	}
    	if(mIsObjectDetecting){
    		mIsObjectDetecting=false;
    		runExperiment();
    	}
    	if(mIsLoadingLib){
    		mIsLoadingLib=false;
    	}
    	return rgba;
    }

    public void savePhoto(Mat rgba, String photoName, String albumPath)
    {
    	// Determine the path and meta data for the photo. 
		final long currentTimeMillis = System.currentTimeMillis(); 
		final String appName = getString(R.string.app_name); 
		final String photoPath = albumPath + File.separator + photoName; 
		final ContentValues values = new ContentValues();  
		values.put(MediaStore.MediaColumns.DATA, photoPath); 
		values.put(Images.Media.TITLE, photoName); 
		values.put(Images.Media.DESCRIPTION, appName); 
		values.put(Images.Media.DATE_TAKEN, currentTimeMillis); 
		
		// Ensure that the album directory exists. 
		File album = new File(albumPath); 
		if (!album.isDirectory() && !album.mkdirs()) { 
			Log.e(TAG, "Failed to create album directory at"+ 
					albumPath); 
			onSavePhotoFailed(); 
			return; 
		}  
		
		// Try to create the photo.
		if (!Imgcodecs.imwrite(photoPath, rgba)) {
			Log.e(TAG, "Failed to save photo to " + photoPath);
			onSavePhotoFailed(); 
		} 
		Log.d(TAG, "Photo saved successfully to " + photoPath);
		
		// Try to insert the photo into the MediaStore.
		@SuppressWarnings("unused")
		Uri uri;
		try { 
			uri = getContentResolver().insert( 
					Images.Media.EXTERNAL_CONTENT_URI, values); 
		} catch (final Exception e) { 
			Log.e(TAG, "Failed to insert photo into MediaStore"); 
			e.printStackTrace(); 
			
			// Since the insertion failed, delete the photo.
			File photo = new File (photoPath);
			if (!photo.delete()) {
				Log.e(TAG, "Failed to delete non-inserted photo"); 
			}			
			onSavePhotoFailed(); 
			return;
		}
		
//		// Open the photo in DisplayResultActivity.
//        final Intent intent = new Intent(this, DisplayResultActivity.class);
//        intent.putExtra(DisplayResultActivity.EXTRA_PHOTO_URI,uri);
//        intent.putExtra(DisplayResultActivity.EXTRA_PHOTO_PATH,photoPath);
//        startActivity(intent);
    }
    
	private void onSavePhotoFailed() { 
		mIsMenuLocked = false; 
		// Show an error message. 
		final String errorMessage = 
				getString(R.string.photo_error_message); 
		runOnUiThread(new Runnable() { 
			@Override public void run() { 
				Toast.makeText(MainActivity.this, errorMessage,
						Toast.LENGTH_SHORT).show(); 
			}
		});
	}
	
	
    @Override
    public void onPause()
    {
        super.onPause();
        Log.i(TAG, "called onPause");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG, "called onResume");
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, 
        		this, mLoaderCallback);
        // reopen menu in case it was locked
		mIsMenuLocked = false; 
    }
    
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "called onDestroy");
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    @SuppressLint("SimpleDateFormat")
	public void runExperiment()
    {
		DateFormat dF= new SimpleDateFormat("yyyyMMdd_HHmmss");
		String time = dF.format(new Date());
    	String outputName = "outputData_"+time+".txt";
    	
    	String testName_m = "testData_m.csv";
    	String testName_mm = "testData_mm.csv";
    	String testName_mm_i = "testData_mm_i.csv";
    	
    	String folderName = "Research/output"+time;
    	String inputFolder = Environment.getExternalStoragePublicDirectory
    			(Environment.DIRECTORY_PICTURES)+ "/Research/database";
    	
    	int number_of_buildings =10;
    	int number_of_angles =5;
    	int variation_of_distance=4;
    	
    	// (!) WARNING: Changing the parameters of ImageDector may affect methods such as
    	// 				[key point filter], [good match filter], [count best match],
    	// 				which, in turn, affect the outcome.
		ImageDetector detector = new ImageDetector(	
				FeatureDetector.ORB,
				DescriptorExtractor.ORB,
				DescriptorMatcher.BRUTEFORCE_HAMMING);
		String detector_type = "ORB"; 
		String extractor_type = "ORB";
		String matcher_type = "BRUTEFORCE_HAMMING";
		
		try
        {
            File root = new File(Environment.getExternalStorageDirectory(), folderName);
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, outputName);
            FileWriter writer = new FileWriter(gpxfile);
            
            File gpxfile_m = new File(root, testName_m);
            FileWriter writer_m = new FileWriter(gpxfile_m);
            
            File gpxfile_mm = new File(root, testName_mm);
            FileWriter writer_mm = new FileWriter(gpxfile_mm);
            
            File gpxfile_mm_i = new File(root, testName_mm_i);
            FileWriter writer_mm_i = new FileWriter(gpxfile_mm_i);
            
            writer.append("[Experiment setup and results]" + "\n");
            writer.append("Number of buildings: " + number_of_buildings + "\n");
            writer.append("Number of angles: " + number_of_angles + "\n");
            writer.append("Variation of distance: " + variation_of_distance + "\n" + "\n");
            
            writer.append("Detector type: " + detector_type + "\n");
            writer.append("Extractor type: " + extractor_type + "\n");
            writer.append("Matcher type: " + matcher_type + "\n" + "\n");            

    		//	(!) WARNING: 	hard code in ImageDetector class
            //					number_of_keypoint = 1000
            //					image_resizing_factor = 0.5
            writer.append("Number of key points for each image: 1000"+"\n"); //HARD CODE
    		writer.append("Image resizing factor: 0.5"+"\n"+"\n"); //HARD CODE
    		
	    	//// Build the library    	
	    	long start= System.currentTimeMillis();
	    	// load using image paths from device
	    	int count_training_images = 0;
	    	for (int a = 0; a < 1 ; a++) {
		    	for (int b = 0; b < number_of_buildings ; b++) {
		    		int d=1;
					String photoName= b+"_"+a+"_"+d+".jpg";
					String photoPath = inputFolder +"/"+"b"+b+"/"+ photoName;
					detector.addToLibrary(photoPath, b);
					Log.i(TAG, "Added 1 new image to the library");
					count_training_images++;
				}    	
	    	}
	    	
	    	long done_building_lib= System.currentTimeMillis();
	    	
	    	Log.i(TAG, "Runtime to build library: "+ (done_building_lib - start)
	    			+" for "+count_training_images+ " training images" ); 
	    	writer.append("Runtime to build library: "+ (done_building_lib - start)
	    			+" for "+count_training_images+ " training images" + "\n"+ "\n");
	    	
	    	//// Detect photos 
    		long startD = System.currentTimeMillis();
	    	int count_detected_images = 0;
	    	int count_visualized_match = 0;
	    	int count_visualized_mismatch = 0;
			for (int a = 0; a < number_of_angles ; a++) {
				for (int d = 0; d < variation_of_distance ; d++) {
			    	int countCorrectMatch =0;
			    	for (int b = 0; b < number_of_buildings ; b++) {
			    		// load the query image
			    		
				    	String photoName= b+"_"+a+"_"+d+".jpg";
						String query_path = inputFolder +"/"+"b"+b+"/"+ photoName;
						TrainingImage result = detector.detectPhoto(query_path);

						if(result == null){
							Log.i(TAG, "Can't identify the image!");
						}else{
							String matchName = result.name();
					    	if(result.tourID() == b){
					    		countCorrectMatch++;
					    		
					    		// visualize and save the match
					    		if(!(a==0 && d==1)){
					    			count_visualized_match++;
					    			
					    			if(count_visualized_match < 20){
					    			// save visualized image
									String image_of_matches_name = System.currentTimeMillis()+ "_"
											+ b + "_" + a + "_" + d + "_to_" + matchName;
							    	Mat image_of_matches = detector.drawCurrentMatches(20);
						    		savePhoto(image_of_matches, image_of_matches_name,
						    				Environment.getExternalStorageDirectory().toString()+
						    				"/" + folderName + "/matches");
					    			}
						    		
						    		// print frequency
						    		String frequency = "Matches, ";
						    		for(TrainingImage trainImg: detector.CURRENT_MATCH_FREQUENCY.keySet()){
						        		Integer i=detector.CURRENT_MATCH_FREQUENCY.get(trainImg);
						        		frequency += i +", ";
						        	}
					        		Log.i(TAG, frequency);
					    			writer_m.append(count_visualized_match+", "+frequency+"\n");
					    			writer_m.flush();
					    		}
					    	}else{
					    		Log.i(TAG, "Mismatched: "+ photoName+" with "+matchName);
					    		writer.append( "Mismatched: "+photoName+" with "+matchName +"\n");
	
					    		// visualize and save the mismatch
				    			count_visualized_mismatch++;
				    			
				    			if(count_visualized_mismatch < 20){
				    			// save visualized image
								String image_of_matches_name = System.currentTimeMillis()+ "_"
										+ b + "_" + a + "_" + d + "_to_" + matchName;
						    	Mat image_of_matches = detector.drawCurrentMatches(20);
					    		savePhoto(image_of_matches, image_of_matches_name,
					    				Environment.getExternalStorageDirectory().toString()+
					    				"/" + folderName + "/mismatches");
				    			}
	
					    		// print frequency
					    		String frequency = "Mismatches, ";
					    		String mismatch_images =photoName+ "_Mismatch images, ";
					    		for(TrainingImage trainImg: detector.CURRENT_MATCH_FREQUENCY.keySet()){
					        		Integer i=detector.CURRENT_MATCH_FREQUENCY.get(trainImg);
					        		frequency += i +", ";
					        		mismatch_images += trainImg.name()+", ";
					        	}
				        		Log.i(TAG, frequency);
				    			writer_mm.append(count_visualized_mismatch+", "+frequency+"\n");
				    			writer_mm.flush();
				    			writer_mm_i.append(count_visualized_mismatch+", "+mismatch_images+"\n");
				    			writer_mm_i.flush();
					    	}
						}
				    	count_detected_images++;
			    	}
			    	double accuracy = (double)countCorrectMatch*100/number_of_buildings ;
			    	Log.i(TAG, "a"+a+"_d"+d+", accuracy: "+accuracy+"%");    
			    	writer.append("==> a"+a+"_d"+d+" "+accuracy+"%"+"\n"+"\n");
			    	writer.flush();
				}
			}
	    	long endD =System.currentTimeMillis();
	    	Log.i(TAG,"Runtime to detect 1 image: "+(endD-startD)/count_detected_images);
	    	writer.append("Runtime to detect 1 image: "+(endD-startD)/count_detected_images +"\n");
			writer.close();
			writer_m.close();
			writer_mm.close();
			writer_mm_i.close();
//          Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();	        
        }
        catch(IOException e)
        {
             e.printStackTrace();
        }
    }

    // Method that displays a given photo on the screen,
    // passing the URI to DisplayResultActivity
    public void displayPhoto(Uri photoUri)
    {
    	// Open the photo in DisplayResultActivity.
        final Intent intent = new Intent(this, DisplayResultActivity.class);
        intent.putExtra(DisplayResultActivity.EXTRA_PHOTO_URI,photoUri);
        startActivity(intent);
    }
    
    // Method that displays a given photo on the screen,
    // passing the ID to DisplayResultActivity
    public void displayPhoto(int ID)
    {
    	// Open the photo in DisplayResultActivity.
        final Intent intent = new Intent(this, DisplayResultActivity.class);
        intent.putExtra(DisplayResultActivity.EXTRA_PHOTO_ID,ID);
        startActivity(intent);
    }
    
    // Method that displays a given photo on the screen,
    // passing the path to DisplayResultActivity
    public void displayPhoto(String photoPath)
    {
    	// Open the photo in DisplayResultActivity.
        final Intent intent = new Intent(this, DisplayResultActivity.class);
        intent.putExtra(DisplayResultActivity.EXTRA_PHOTO_PATH,photoPath);
        startActivity(intent);
    }
    
    // Method that returns the path from an URI
    // (URL Source) http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework    
    public String getPath(Uri uri) 
        {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) return null;
            int column_index =             
            		cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String s=cursor.getString(column_index);
            cursor.close();
            return s;
        }
    
}
