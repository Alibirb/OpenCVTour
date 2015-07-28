package com.thanh.photodetector;

import java.io.File;
import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import android.location.Location;
import android.media.ExifInterface;

public class TrainingImage {
	private String path_id;
	private long tour_id;
	private Mat image;
	private MatOfKeyPoint key_points;
	private Mat descriptors;
	private Location location;
	
	public TrainingImage(){}
	
	public TrainingImage(String image_path, long tour_item_id, Mat	given_image)
	{
		this(image_path, tour_item_id, given_image, null);
	}
	
	public TrainingImage(String image_path, long tour_item_id, 
			Mat	given_image, Mat given_descriptors)
	{
		path_id = image_path;
		tour_id = tour_item_id;
		image = given_image;
		descriptors = given_descriptors;
		addLocation(image_path);
	}
	
	public void addLocation(String path){
		try {
			ExifInterface exif = new ExifInterface(path);
			float [] latlong = new float[2];
			boolean isAvailable= exif.getLatLong(latlong);
			Location loc = new Location(" ");
			if (isAvailable){
				float latitude = latlong[0];
				float longitude = latlong[1];
				loc.setLatitude(latitude);
				loc.setLongitude(longitude);
				
				location = loc;
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setImage(Mat new_image){
		image = new_image;
	}
	
	public void setPathID(String newPath){
		path_id= newPath;
	}
	
	public void setTourID(long new_tour_id){
		if(new_tour_id>=0){
			tour_id = new_tour_id;
		}else{
			System.out.println("Tour ID must be non-negative");
		}
	}
	
	public void setDescriptors(Mat descrpt){
		descriptors=descrpt;
	}
	
	public void setKeyPoints(MatOfKeyPoint new_key_points)
	{
		key_points = new_key_points;
	}
	
	public String name(){
		String name = new File(path_id).getName();
		return name;
	}
	
	public Mat image(){
		return image;
	}
	
	public String pathID(){
		return path_id;
	}
	
	public long tourID(){
		return tour_id;
	}
	
	public Mat descriptors(){
		return descriptors;
	}
	
	public MatOfKeyPoint keyPoints(){
		return key_points;
	}

	public Location location(){
		return location;
	}
	
}
