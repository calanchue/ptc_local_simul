import java.io.Serializable;

class Wall implements Serializable {
		public float x;
		public float y;
		public float w;
		public float h;
		
		public Wall(float x, float y, float w, float h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
		
	}
