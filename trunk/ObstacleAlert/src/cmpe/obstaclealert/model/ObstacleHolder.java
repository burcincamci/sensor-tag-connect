package cmpe.obstaclealert.model;

import com.google.gson.annotations.SerializedName;

public class ObstacleHolder {
	public Obstacle[] getObstacles() {
		return obstacles;
	}

	public void setObstacles(Obstacle[] obstacles) {
		this.obstacles = obstacles;
	}

	@SerializedName("obstacle")
	private Obstacle[] obstacles;
	
}
