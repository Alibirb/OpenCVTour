package com.thanh.photodetector;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class TrainingImage {
	private String path_id;
	private long tour_id;
	private Mat image;
	private MatOfKeyPoint key_points;
	private Mat descriptors;
	
	public TrainingImage(){}
	
	public TrainingImage(String image_path, long tour_item_id, Mat	given_image)
	{
		path_id = image_path;
		tour_id = tour_item_id;
		image = given_image;
	}
	
	public TrainingImage(String image_path, Mat	given_image, Mat given_descriptors)
	{
		path_id = image_path;
		image = given_image;
		descriptors = given_descriptors;
	}
	
	public TrainingImage(String image_path, long tour_item_id, 
			Mat	given_image, Mat given_descriptors)
	{
		path_id = image_path;
		tour_id = tour_item_id;
		image = given_image;
		descriptors = given_descriptors;
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
	
}
