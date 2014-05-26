package cmpe.obstaclealert.model;

import com.google.gson.annotations.SerializedName;

public class Obstacle {
	@SerializedName("longitude")
	private double longitude;
	
	@SerializedName("latitude")
	private double latitude;
	
	@SerializedName("type")
	private String type;
	
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
