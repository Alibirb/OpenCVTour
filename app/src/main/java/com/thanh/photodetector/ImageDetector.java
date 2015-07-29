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

package com.thanh.photodetector;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
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
	/*
	 * Declare objects that support the process of images detecting
	 */
	private FeatureDetector fDetector;
	private DescriptorExtractor dExtractor;
	private DescriptorMatcher dMatcher;

	/*
	 * Variables that support drawCurrentMatches method
	 */
	TrainingImage CURRENT_QUERY_IMAGE;
	TrainingImage CURRENT_RESULT_IMAGE;
	MatOfDMatch CURRENT_GOOD_MATCHES;

	/*
	 *  Maximum length of the sides of images.
	 *  Used to scale down images before storing
	 */
	int max_side;
	
	/*
	 * Threshold used for 2nd-best filter in findBestMatch method
	 */
	double filter_ratio;

	/*
	 * Threshold used for location filter in locationFilter method
	 */
	double distance_bound;

	/*
	 * Tag for messages printed to LogCat
	 */
	protected static final String TAG = "ImageDetector";

	/*
	 * Tag for Error messages printed to LogCat
	 */
	protected static final String ERROR = "Error in ImageDetector";

	/*
	 * List of all train images
	 */
	private List<TrainingImage> training_library;

	/*
	 * Default constructor.
	 * Uses FAST detector, ORB extractor, and BRUTEFORCE_HAMMINGLUT matcher.
	 */
	public ImageDetector() {
		this(FeatureDetector.FAST, DescriptorExtractor.ORB, DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
	}

	/*
	 * Constructor that uses detecting algorithms specified by the parameters
	 */
	public ImageDetector(int detector_type, int extractor_type, int matcher_type)
	{
		fDetector = FeatureDetector.create
				(detector_type);
		dExtractor = DescriptorExtractor.create
				(extractor_type);
		dMatcher= DescriptorMatcher.create
				(matcher_type);
		training_library= new ArrayList<TrainingImage>();
		
		// Specific values selected after experimenting with different data sets
		max_side = 300;
		filter_ratio = 5;
		distance_bound = 50;
	}

	/*
	 * Method that adds a new image to the train library
	 * @param image_path the path of the image
	 * @param tour_item_id the id of the tour item (whom the image belongs to)
	 */
	public void addToLibrary(String image_path, long tour_item_id)
	{
		Mat imgDescriptor;
		TrainingImage training_img;
		File descriptor_file = new File(image_path + ".descriptors.yaml");
		// Check if the image's features have already been extracted
		if(!descriptor_file.exists()) {
			Mat img = Imgcodecs.imread(image_path);
			// reduce the image's size to increase the runtime and save the memory
			Mat resized_img = resize(img);  
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

	/*
	 * Method that identifies the tour item the given image belongs to
	 * @param iamge_path the path of the image used for identification
	 * @param item_ids the list of qualified items
	 * @return the id
	 */
	public long identifyObject(String image_path, List<Long> item_ids)
	{
		TrainingImage result = detectPhoto(image_path, item_ids);
		if(result == null)
			return -1;
		return result.tourID();
	}

	/*
	 * Methods that resize a given image to certain size
	 * The size is specified by the class variable max_side 
	 * @param src_image the image to be resized
	 * @return the resized image
	 */
	public Mat resize(Mat src_img)
	{
		// scale down images
		double h = src_img.size().height;
		double w = src_img.size().width;
		double multiplier = max_side/Math.max(h,w);
		Size size= new Size(w*multiplier, h*multiplier);
		Imgproc.resize(src_img, src_img, size);
		return src_img;
	}

	/*
	 * 
	 */
	public TrainingImage detectPhoto(String query_path){
		List<Long> ids = new ArrayList<>();
		for(TrainingImage img : training_library) {
			if(!ids.contains(img.tourID()))
				ids.add(img.tourID());
		}
		return detectPhoto(query_path, ids);
	}

	/*
	 * Method that detects a given image based on the training library
	 * @param query_path the path of the image to be detected
	 * @param item_ids the list of qualified items
	 * @return the best match image
	 */
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

	/*
	 * 
	 */
	private List<DMatch> filterByItem(List<DMatch> matches, List<Long> item_ids) {
		List<DMatch> filtered_matches = new ArrayList<>();
		for(DMatch match : matches) {
			if(item_ids.contains(training_library.get(match.imgIdx).tourID()))
				filtered_matches.add(match);
		}
		return filtered_matches;
	}

	/*
	 * Method that filters the matches between the query image and the best match image
	 * @param good_matches the total matches based on the training library
	 * @param bestMatch the image that has the highest number of matches
	 * @return a MatOfDMatch of the matches of the best match image
	 */
	private MatOfDMatch getCurrentGoodMatches(List<DMatch> good_matches,TrainingImage bestMatch)
	{
		List<DMatch> matches_of_bestMatch = new ArrayList<DMatch>();
		// loop to filter matches of train images, which are not the bestMatch image
		for(DMatch aMatch: good_matches){
			TrainingImage trainImg = training_library.get(aMatch.imgIdx);
			// Check if the match result is the bestMatch image
			if (trainImg == bestMatch)
			{
				matches_of_bestMatch.add(aMatch);
			}
		}
		MatOfDMatch result = new MatOfDMatch();
		result.fromList(matches_of_bestMatch);
		return result;
	}

	/*
	 * Method that creates an Mat image of the query and result images
	 * @param n the number of matches to be visualized in the image
	 * @return the image
	 */
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

	/*
	 * Method that sorts and returns submat of a MatOfDMatch
	 * @param the mat of matches
	 * @param start the starting index of matches to be returned
	 * @param end the ending index of matches to be returned 
	 */
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

	/*
	 * Method that returns a matrix of descriptors for a given image
	 * Using rgb-descriptors
	 */
	public Mat imgDescriptor_rgb(TrainingImage train_img)
	{
		Mat img = train_img.image();
		Mat imgDescriptor = new Mat();
		// detect the matrix of key points of that image
		MatOfKeyPoint imgKeyPoints = new MatOfKeyPoint();
		fDetector.detect(img, imgKeyPoints);

		Log.i(TAG, "imgKeyPoints size:  "+ imgKeyPoints.size());

		// Compute the descriptor from those key points
		// Using RGB channels to describe
		Mat img_r = new Mat(img.rows(), img.cols(), CvType.CV_8UC1);
		Mat img_g = new Mat(img.rows(), img.cols(), CvType.CV_8UC1);
		Mat img_b = new Mat(img.rows(), img.cols(), CvType.CV_8UC1);
		double[] rgb;
		// assign R, G, B values to each image
		for(int x=0; x < img.cols();x++){
			for(int y=0; y < img.rows(); y++){
				rgb = img.get(y,x);
				img_r.put(y, x, new double[]{rgb[0]});
				img_g.put(y, x, new double[]{rgb[1]});
				img_b.put(y, x, new double[]{rgb[2]});				
			}
		}

		Mat imgDescriptor_r = new Mat();
		Mat imgDescriptor_g = new Mat();
		Mat imgDescriptor_b = new Mat();
		dExtractor.compute(img_r,imgKeyPoints, imgDescriptor_r);
		dExtractor.compute(img_g,imgKeyPoints, imgDescriptor_g);
		dExtractor.compute(img_b,imgKeyPoints, imgDescriptor_b);

		Mat imgDescriptor_x3 = new Mat();
		// Concatenate the R, G, B descriptors
		List<Mat> lmat = Arrays.asList(
				imgDescriptor_r.submat(0,imgDescriptor_r.rows(),0,16),
				imgDescriptor_g.submat(0,imgDescriptor_g.rows(),0,16),
				imgDescriptor_b.submat(0,imgDescriptor_b.rows(),0,16));
		Core.hconcat(lmat, imgDescriptor_x3);
		Log.i(TAG, "imgDescriptor_x3 size:  "+ imgDescriptor_x3.size());
		
		imgDescriptor = imgDescriptor_x3;
		img_r.release();
		img_g.release();
		img_b.release();
		imgDescriptor_r.release();
		imgDescriptor_g.release();
		imgDescriptor_b.release();
		
		train_img.setKeyPoints(imgKeyPoints);
		train_img.setDescriptors(imgDescriptor);
		return imgDescriptor;
	}
	
	/*
	 * Method that returns a matrix of descriptors for a given image
	 */
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

	/*
	 * 
	 */
	public Mat loadImageDescriptors(Map<String,Object> data) {
		Mat m = new Mat((Integer) data.get("rows"),(Integer) data.get("columns"),(Integer) data.get("type"));
		byte[] bytes = (byte[]) data.get("bytes");
		m.put(0,0,bytes);

		return m;
	}
	
	/*
	 * 
	 */
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
	
	/*
	 * 
	 */
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

	/*
	 * Method that finds the best match from a list of matches
	 */
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
		HashMap<TrainingImage,Integer> filtered_hm = locationFilter(hm,query_image);
		hm = filtered_hm;

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
		else{ 
			// 2nd-best filter
			int diff = hm.get(bestMatch)-hm.get(secondBestMatch) ;
			if ( diff * diff > filter_ratio * hm.get(bestMatch)){
				return bestMatch;
			}
			else{
				Log.i(TAG, "Found no best match for the query image!");
				return null;
			}
		}
	}

	/*
	 * Method that filters the map of matches by their locations
	 * @param hm the map of match images
	 * @param query_image 
	 * @return the map of images within the location bound
	 */
	public HashMap<TrainingImage,Integer> locationFilter(HashMap<TrainingImage,Integer> hm, TrainingImage query_image)
	{
		Location query_location = query_image.location();
		if(query_location == null){
			Log.i(TAG, "Image's location is not available");
			return hm;
		}else{
			HashMap<TrainingImage,Integer> new_hm = new HashMap<TrainingImage,Integer>();
			for(TrainingImage trainImg: hm.keySet()){
				double distance = query_location.distanceTo(trainImg.location());
				if(distance < distance_bound){
					int count = hm.get(trainImg);
					new_hm.put(trainImg,count);
				}
			}
			return new_hm;
		}
	}

	/*
	 * Method that draws the features of the image
	 * @param rgba the image to be detected features and drawn to 
	 */
	public void drawFeatures(Mat rgba){
		MatOfKeyPoint keyPoints = new MatOfKeyPoint();
		Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2RGB);
		fDetector.detect(rgba, keyPoints);
		Features2d.drawKeypoints(rgba,keyPoints,rgba);
		Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2RGBA);
	}

	/*
	 * Method that draws the features of the image
	 * @param rgba the image to be detected features and drawn to 
	 * @param keyPoints the key points of the given image
	 */
	public void drawFeatures(Mat rgba,MatOfKeyPoint keyPoints){
		Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGBA2RGB);
		Features2d.drawKeypoints(rgba,keyPoints,rgba);
		Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2RGBA);
	}
}
