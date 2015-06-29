package com.thanh.photodetector;

import org.opencv.core.Mat;

import android.net.Uri;

public class TrainingImage {
	private String path_id;
	private long tour_id;
	private Mat descriptors;
	private Uri uri;
	
	public TrainingImage(){}
	
	public TrainingImage(String image_path)
	{
		path_id = image_path;
	}
	
	public TrainingImage(String image_path, long tour_item_id, Mat given_descriptors)
	{
		path_id = image_path;
		tour_id = tour_item_id;
		descriptors = given_descriptors;
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
	
	public String pathID(){
		return path_id;
	}
	
	public long tourID(){
		return tour_id;
	}
	
	public Mat descriptors(){
		return descriptors;
	}
	
}
