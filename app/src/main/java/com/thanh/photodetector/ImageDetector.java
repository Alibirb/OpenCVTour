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

import android.location.Location;
import android.util.Log;

public class ImageDetector {
	// Declare objects that support the process of images detecting
	private FeatureDetector fDetector;
	private DescriptorExtractor dExtractor;
	private DescriptorMatcher dMatcher;

    double filter_ratio;

	// tag of messages printed to LogCat
	protected static final String TAG = "ImageDetector";

	// tag of Error messages printed to LogCat
	protected static final String ERROR = "Error in ImageDetector";

	// A list of all training photos
	private List<TrainingImage> training_library;

	public ImageDetector() {
		this(FeatureDetector.FAST, DescriptorExtractor.ORB, DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
	}

	public ImageDetector(int detector_type, int extractor_type, int matcher_type)
	{
		fDetector = FeatureDetector.create
				(detector_type);
		dExtractor = DescriptorExtractor.create
				(extractor_type);
		dMatcher= DescriptorMatcher.create
				(matcher_type);
		training_library= new ArrayList<TrainingImage>();
		filter_ratio = 1.25;
	}

	public void addToLibrary(String image_path, long tour_item_id)
	{
		Mat imgDescriptor;
		TrainingImage training_img;
		File descriptor_file = new File(image_path + ".descriptors.yaml");
		if(!descriptor_file.exists()) {
			Mat img = Imgcodecs.imread(image_path);
			Mat resized_img = resize(img);  // scale down the image
			training_img = new TrainingImage(image_path, tour_item_id, resized_img);
			imgDescriptor = imgDescriptor(training_img);
		} else {
			imgDescriptor = loadImageDescriptors(descriptor_file);
			training_img = new TrainingImage(image_path, tour_item_id, null, imgDescriptor);
		}

		// add image to dMacher's internal training library
		dMatcher.add(Arrays.asList(imgDescriptor));

		// add image to training_library
		training_library.add(training_img);
	}

	public void clearLibrary()
	{
		// clear ImageDetector's library
		training_library= new ArrayList<TrainingImage>();
		// clear dMatcher's internal library
		dMatcher.clear();
	}

	public long identifyObject(String image_path, List<Long> item_ids)
	{
		TrainingImage result = detectPhoto(image_path, item_ids);
		if(result == null)
			return -1;
		return result.tourID();
	}

	public Mat resize(Mat src_img)
	{
		// scale down images
		double h = src_img.size().height;
		double w = src_img.size().width;
		double multiplier = 300/Math.max(h,w);
		Size size= new Size(w*multiplier, h*multiplier);
		Imgproc.resize(src_img, src_img, size);
		return src_img;
	}

	// variables for drawCurrentMatches method
	TrainingImage CURRENT_QUERY_IMAGE;
	TrainingImage CURRENT_RESULT_IMAGE;
	MatOfDMatch CURRENT_GOOD_MATCHES;

	public TrainingImage detectPhoto(String query_path){
		List<Long> ids = new ArrayList<>();
		for(TrainingImage img : training_library) {
			if(!ids.contains(img.tourID()))
				ids.add(img.tourID());
		}
		return detectPhoto(query_path, ids);
	}

	// Method that detects a given image based on the training library
	public TrainingImage detectPhoto(String query_path, List<Long> item_ids){
		Mat img = Imgcodecs.imread(query_path);
		Mat resized_img = resize(img); // scale down the query image
		TrainingImage query_image = new TrainingImage(query_path,0,resized_img);

		// get descriptors of the query image
		// detect the matrix of key points of that image
		Mat query_descriptors = imgDescriptor(query_image);

		// Match the descriptors of a query image
		// to descriptors in the training collection.
		MatOfDMatch matches= new MatOfDMatch();
		dMatcher.match(query_descriptors, matches);

		// filter good matches
		List<DMatch> list_of_matches = matches.toList();

		// filter out any items not in our list
		list_of_matches = filterByItem(list_of_matches, item_ids);

		// find the image that matches the most
		TrainingImage bestMatch = findBestMatch(list_of_matches, query_image);

		// update variables for drawCurrentMatches method
		CURRENT_QUERY_IMAGE = query_image;
		CURRENT_RESULT_IMAGE = bestMatch;
		CURRENT_GOOD_MATCHES = getCurrentGoodMatches(list_of_matches, bestMatch);

		return bestMatch;
	}

	private List<DMatch> filterByItem(List<DMatch> matches, List<Long> item_ids) {
		List<DMatch> filtered_matches = new ArrayList<>();
		for(DMatch match : matches) {
			if(item_ids.contains(training_library.get(match.imgIdx).tourID()))
				filtered_matches.add(match);
		}
		return filtered_matches;
	}

	private MatOfDMatch getCurrentGoodMatches(List<DMatch> good_matches,TrainingImage bestMatch)
	{
		List<DMatch> matches_of_bestMatch = new ArrayList<DMatch>();
		// loop to filter matches of train images, which are not the bestMatch image
		for(DMatch aMatch: good_matches){
			TrainingImage trainImg = training_library.get(aMatch.imgIdx);
			if (trainImg == bestMatch)
			{
				matches_of_bestMatch.add(aMatch);
			}
		}
		MatOfDMatch result = new MatOfDMatch();
		result.fromList(matches_of_bestMatch);
		return result;
	}

	public Mat drawCurrentMatches(int n)
	{
		Mat img1 = CURRENT_QUERY_IMAGE.image();
		MatOfKeyPoint kp1= CURRENT_QUERY_IMAGE.keyPoints();
		Mat img2 = CURRENT_RESULT_IMAGE.image();
		MatOfKeyPoint kp2= CURRENT_RESULT_IMAGE.keyPoints();
		Mat result = new Mat();

		Features2d.drawMatches(img1, kp1, img2, kp2,
				sortedKMatches(CURRENT_GOOD_MATCHES,0,n), result);
		return result;
	}

	public MatOfDMatch sortedKMatches(MatOfDMatch matches, int start, int end)
	{
		List<DMatch> list = matches.toList();
		Collections.sort(list, new Comparator<DMatch>() {
			@Override
			public int compare(final DMatch object1, final DMatch object2) {
				return (int)(object1.distance - object2.distance);
			}
		});
		if(list.size()<end){
			Log.i(TAG,"Only found "+list.size()+" matches. Can't return "+end);
			end = list.size();
		}
		List<DMatch> subllist = list.subList(start, end);
		MatOfDMatch result = new MatOfDMatch();
		result.fromList(subllist);
		return result;
	}

	// Method that returns a matrix of descriptors for a given image
	public Mat imgDescriptor(TrainingImage train_img)
	{
		Mat img = train_img.image();
		Mat imgDescriptor = new Mat();
		// detect the matrix of key points of that image
		MatOfKeyPoint imgKeyPoints = new MatOfKeyPoint();
		fDetector.detect(img, imgKeyPoints);

		// compute the descriptor from those key points
		dExtractor.compute(img,imgKeyPoints, imgDescriptor);
		train_img.setKeyPoints(imgKeyPoints);
		train_img.setDescriptors(imgDescriptor);
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
		for(TrainingImage image : training_library) {
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

	HashMap<TrainingImage, Integer> CURRENT_MATCH_FREQUENCY;
	// Method that finds the best match from a list of matches
	private TrainingImage findBestMatch(List<DMatch> good_matches, TrainingImage query_image)
	{
		HashMap<TrainingImage,Integer> hm= new HashMap<TrainingImage, Integer>();
		// count the images matched
		for(DMatch aMatch: good_matches){
			TrainingImage trainImg = training_library.get(aMatch.imgIdx);
			if(hm.get(trainImg)==null){
				hm.put(trainImg,1);
			}else{
				hm.put(trainImg, hm.get(trainImg)+1);
			}
		}

    	// location filter
    	HashMap<TrainingImage,Integer> filtered_hm = locationFilter(hm,query_image.location());
    	hm = filtered_hm;
    	
		CURRENT_MATCH_FREQUENCY = hm;
		// search for the image that matches the largest number of descriptors.
		TrainingImage bestMatch= null;
		TrainingImage secondBestMatch= null;

		for(TrainingImage trainImg: hm.keySet()){
			if(bestMatch == null){
				bestMatch= trainImg;				
			}else{
				if(hm.get(trainImg)> hm.get(bestMatch)){
					secondBestMatch = bestMatch;
					bestMatch= trainImg;
				}else{
					if (secondBestMatch == null){
						secondBestMatch = trainImg;
					}else{
						if(trainImg.tourID() != bestMatch.tourID() 
								&& hm.get(trainImg)> hm.get(secondBestMatch)){
							secondBestMatch = trainImg;
						}
					}
				}
			}
		}

		// print result
		for(TrainingImage trainImg: hm.keySet()){
			Log.i(TAG, "Matched img result:  "+ trainImg.pathID() +
					", numOfMatches: "+hm.get(trainImg));
		}
		
		if (secondBestMatch == null){
			return bestMatch;
		}
		else if (hm.get(bestMatch) > filter_ratio*hm.get(secondBestMatch)){
			return bestMatch;
		}
		else{
			Log.i(TAG, "Found no best match for the query image!");
			return null;
		}
	}

    public HashMap<TrainingImage,Integer> locationFilter(HashMap<TrainingImage,Integer> hm, Location query_location)
    {
    	if(query_location == null){
    		Log.i(TAG, "Image's location is not available");
    		return hm;
    	}else{
        	HashMap<TrainingImage,Integer> new_hm = new HashMap<TrainingImage,Integer>();
	    	for(TrainingImage trainImg: hm.keySet()){
	    		double distance = query_location.distanceTo(trainImg.location());
	    		if(distance < 50){
	    			int count = hm.get(trainImg);
	    			new_hm.put(trainImg,count);
	    		}
	    	}
	    	return new_hm;
    	}
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

	public void drawFeatures(Mat rgba,MatOfKeyPoint keyPoints){
		Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2RGB);
		Features2d.drawKeypoints(rgba,keyPoints,rgba);
		Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2RGBA);
	}
}
