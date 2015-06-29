package com.thanh.photodetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.yaml.snakeyaml.Yaml;

import android.util.Log;

public class ImageDetector {
	// Declare objects that support the process of images detecting
    private FeatureDetector fDetector;
    private DescriptorExtractor dExtractor;
    private DescriptorMatcher dMatcher;

    // tag of messages printed to LogCat
    protected static final String TAG = "ImageDetector";

    // tag of Error messages printed to LogCat
    protected static final String ERROR = "Error in ImageDetector";

    // A list of all training photos
    private List<TrainingImage> traing_library;

    public ImageDetector()
    {
    	fDetector = FeatureDetector.create
				(FeatureDetector.FAST);
		dExtractor = DescriptorExtractor.create
				(DescriptorExtractor.ORB);
		dMatcher= DescriptorMatcher.create
				(DescriptorMatcher.BRUTEFORCE_HAMMING);
		traing_library= new ArrayList<TrainingImage>();
    }

    public void addToLibrary(String image_path, long tour_item_id)
	{
		Mat imgDescriptor;
		File descriptor_file = new File(image_path + ".descriptors.yaml");
		if(!descriptor_file.exists()) {
			Mat img = Imgcodecs.imread(image_path);
			Mat resized_img = resize(img, 0.5);  // scale down the image
			imgDescriptor = imgDescriptor(resized_img);
		} else
			imgDescriptor = loadImageDescriptors(descriptor_file);

		// add image to dMacher's internal training library
		dMatcher.add(Arrays.asList(imgDescriptor));

		// add image to traing_library
		TrainingImage training_img= new TrainingImage
				(image_path, tour_item_id, imgDescriptor);
		traing_library.add(training_img);
	}

    public void clearLibrary()
    {
    	// clear ImageDetector's library
    	traing_library= new ArrayList<TrainingImage>();
    	// clear dMatcher's internal library
    	dMatcher.clear();
    }

    public long identifyObject(String image_path)
    {
    	TrainingImage result = detectPhoto(image_path);
    	if(result == null)
    		return -1;
    	return result.tourID();
    }

    public Mat resize(Mat src_img, double multiplier)
    {
		// scale down images
		Mat resized_img= new Mat();
		Size size= new Size(src_img.size().width*multiplier, src_img.size().height*multiplier);
		Imgproc.resize(src_img, resized_img, size);
		return resized_img;
    }

    // Method that detects a given image based on the training library
    public TrainingImage detectPhoto(String query_path){
    	Log.i(TAG, "called detectFeatures");
    	long start= System.currentTimeMillis();

    	MatOfDMatch matches= new MatOfDMatch();
    	Mat img = Imgcodecs.imread(query_path);
    	Mat rgbaQuery = resize(img, 0.5); // scale down the dquery image

    	// get descriptors of the query image
    	// detect the matrix of key points of that image
    	Mat query_descriptors = imgDescriptor(rgbaQuery);
		Log.i(TAG, "query img descriptors:  "+ query_descriptors.size());

    	// Match the descriptors of a query image
    	// to descriptors in the training collection.
    	dMatcher.match(query_descriptors, matches);
    	Log.i(TAG, "matrix of matches size:  "+ matches.size());

    	// filter good matches
    	List<DMatch> total_matches = matches.toList();
    	List<DMatch> good_matches = filterGoodMatches(total_matches);
    	Log.i(TAG, "list of all matches size:  "+ total_matches.size());
    	Log.i(TAG, "list of good matches size:  "+ good_matches.size());

    	// find the image that matches the most
    	TrainingImage bestMatch = findBestMatch(good_matches);
    	Log.i(TAG, "bestMatch img:  "+ bestMatch.pathID());

    	long done_matching= System.currentTimeMillis();
    	Log.i(TAG, "Runtime to match: "+ (done_matching - start));
    	Log.i(TAG, "finishing detectFeatures");
    	return bestMatch;
    }

    // Method that returns a matrix of descriptors for a given image
    public Mat imgDescriptor(Mat img)
    {
    	Mat imgDescriptor = new Mat();
    	// detect the matrix of key points of that image
		MatOfKeyPoint imgKeyPoints = new MatOfKeyPoint();
		fDetector.detect(img, imgKeyPoints);

		// filter the best key points
		imgKeyPoints= topKeyPoints(imgKeyPoints, 500);

		// compute the descriptor from those key points
		dExtractor.compute(img,imgKeyPoints, imgDescriptor);
		return imgDescriptor;
    }

	public Mat loadImageDescriptors(Map<String,Object> data) {
		Mat m = new Mat((Integer) data.get("rows"),(Integer) data.get("columns"),(Integer) data.get("type"));
		byte[] bytes = (byte[]) data.get("bytes");
		m.put(0,0,bytes);

		return m;
	}
	public Mat loadImageDescriptors(File file) {
		try {
			Log.d(TAG, "Attempting to load image descriptors from " + file.getName());
			Yaml yaml = new Yaml();

			Map<String, Object> data = (Map<String, Object>) yaml.load(new FileReader(file));
			return loadImageDescriptors(data);
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			return null;
		}
	}
	public void saveImageDescriptors() {
		for(TrainingImage image : traing_library) {
			Log.d(TAG, "saving " + image.pathID() + " to Map.");
			Map<String,Object> descriptor_data = new HashMap<>();
			Mat m = image.descriptors();
			byte[] bytes = new byte[(int) (m.total() * m.elemSize())];
			m.get(0, 0, bytes);
			descriptor_data.put("type", m.type());
			descriptor_data.put("columns", m.cols());
			descriptor_data.put("rows", m.rows());
			descriptor_data.put("bytes", bytes);

			Yaml yaml = new Yaml();
			try {
				File file = new File(image.pathID() + ".descriptors.yaml");
				FileWriter writer = new FileWriter(file);
				yaml.dump(descriptor_data, writer);
				writer.close();
				Log.d(TAG, "saved '" + file + "'");
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
		}
	}

    // Method that returns the top 'n' best key points
    private MatOfKeyPoint topKeyPoints(MatOfKeyPoint imgKeyPoints, int n)
    {
		Log.i(TAG, "imgKeyPoints size:  "+ imgKeyPoints.size());
		// Sort and select n best key points
		List<KeyPoint> listOfKeypoints = imgKeyPoints.toList();
		if(listOfKeypoints.size()<n){
			Log.i(ERROR, "The requested number of key points is less than that of given key points");
			return imgKeyPoints;
		}
		Collections.sort(listOfKeypoints, new Comparator<KeyPoint>() {
		    @Override
		    public int compare(KeyPoint kp1, KeyPoint kp2) {
		        // Sort them in descending order, so the best response KPs will come first
		        return (int) (kp2.response - kp1.response);
		    }
		});
		Log.i(TAG, "listOfKeypoints size:  "+ listOfKeypoints.size());
		List<KeyPoint> bestImgKeyPoints = listOfKeypoints.subList(0,n);
		Log.i(TAG, "bestImgKeyPoints size:  "+ bestImgKeyPoints.size());

		MatOfKeyPoint result = new MatOfKeyPoint();
		result.fromList(bestImgKeyPoints);
		return result;
    }

    // Method that filters good matches from given list of matches,
    // using arbitrary bounds
    private List<DMatch> filterGoodMatches(List<DMatch> total_matches)
    {
    	List<DMatch> goodMatches = new ArrayList<DMatch>();
    	double max_dist = 0; double min_dist = 100;
    	// calculate max and min distances between keypoints
    	for( DMatch dm: total_matches)
    	{
    		double dist = dm.distance;
    	    if( dist < min_dist ) min_dist = dist;
    	    if( dist > max_dist ) max_dist = dist;
    	}
    	for(DMatch aMatch: total_matches){
    		// *note: arbitrary constants 3 & 0.02
    		if( aMatch.distance <= Math.max(3*min_dist, 0.02)){
    			goodMatches.add(aMatch);
    		}
    	}
    	return goodMatches;
    }

    // Method that finds the best match from a list of matches
    private TrainingImage findBestMatch(List<DMatch> good_matches)
    {
    	HashMap<TrainingImage,Integer> hm= new HashMap<TrainingImage, Integer>();
    	// count the images matched
    	for(DMatch aMatch: good_matches){
    		TrainingImage trainImg = traing_library.get(aMatch.imgIdx);
    		if(hm.get(trainImg)==null){
    			hm.put(trainImg,1);
    		}else{
    			hm.put(trainImg, hm.get(trainImg)+1);
    		}
    	}

    	// search for the image that matches the largest number of descriptors.
    	TrainingImage bestMatch= null;
    	Integer greatestCount=0;
    	Log.i(TAG, "hashmap of matches size:  "+ hm.size());
    	for(TrainingImage trainImg: hm.keySet()){
    		Log.i(TAG, "train img:  "+ trainImg);
    		Integer count=hm.get(trainImg);
    		if(count> greatestCount){
    			greatestCount= count;
    			bestMatch= trainImg;
    		}
    	}

    	// print result
    	for(TrainingImage trainImg: hm.keySet()){
    		Log.i(TAG, "Matched img result:  "+ trainImg.pathID() +
    				", numOfMatches: "+hm.get(trainImg));
    	}
    	return bestMatch;
    }

    // Method that displays the image and its features
    // on the device's screen
    public void drawFeatures(Mat rgba){
    	MatOfKeyPoint keyPoints = new MatOfKeyPoint();
    	Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2RGB);
    	fDetector.detect(rgba, keyPoints);
    	Features2d.drawKeypoints(rgba,keyPoints,rgba);
    	Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2RGBA);
    }

    private void alternateFilter()
    {
//		// set threshold to 100 (instead of 1) to reduce the number of key points
//		// not work for new opencv
//		try {
//			File outputDir = getCacheDir(); // If in an Activity (otherwise getActivity.getCacheDir();
//			File outputFile = File.createTempFile("orbDetectorParams", ".YAML", outputDir);
//			writeToFile(outputFile, "%YAML:1.0\nthreshold: 100 \nnonmaxSupression: true\n");
//			fDetector.read(outputFile.getPath());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

    }

    // (URL Source) http://answers.opencv.org/question/3167/java-how-to-set-parameters-to-orb-featuredetector/?answer=17296#post-id-17296
    private void writeToFile(File file, String data) {
        try {
			FileOutputStream stream = new FileOutputStream(file);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
			outputStreamWriter.write(data);
			outputStreamWriter.close();
			stream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void ratioTest()
    {

//    	// match with ratio test
//    	List<MatOfDMatch> match_list = new ArrayList<MatOfDMatch>();
//    	dMatcher.knnMatch(query_descriptors, match_list, 2);
//    	Log.i(TAG, "knnMatch, k=2, match_list size:  "+  match_list.size());
//    	List<DMatch> tested_dMatch = new ArrayList<DMatch>();
//    	for(MatOfDMatch mat: match_list)
//    	{
//    		List<DMatch> dMatches = mat.toList();
//    		Log.i(TAG, "dMatches.size:  "+  dMatches.size());
//    		if(dMatches.size() <2)
//    		{
//    			tested_dMatch.add(dMatches.get(0));
//    		}else{
//	    		Log.i(TAG, "dMatches get (0):  "+  
//	    				dMatches.get(0).distance +"  "+
//	    				dMatches.get(0).imgIdx +"  "+ 
//	    				dMatches.get(0).queryIdx +"  "+ 
//	    				dMatches.get(0).trainIdx);
//	    		Log.i(TAG, "dMatches get (1):  "+  
//	    				dMatches.get(1).distance +"  "+
//	    				dMatches.get(1).imgIdx +"  "+ 
//	    				dMatches.get(1).queryIdx +"  "+ 
//	    				dMatches.get(1).trainIdx);
//	    		if(dMatches.get(0).distance < 0.75 * dMatches.get(1).distance)
//	    		{
//	    			tested_dMatch.add(dMatches.get(0));
//	    		}
//    		}
//    	}
//    	// filter good matches
//    	List<DMatch> total_matches = tested_dMatch;
//    	List<DMatch> good_matches = tested_dMatch;

    }
}
