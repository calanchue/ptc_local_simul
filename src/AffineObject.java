import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

class AffineObject{
		final static Point2D position_standard = new Point2D.Float(0.0F, 0.0F);
		final static Point2D nose_standard = new Point2D.Float(0.0F, 10.0F);
		
		public Point2D position;		
		public Point2D nose_position;
		public AffineTransform t;
		
		public AffineObject(AffineTransform t){
			this.t = t;
			position = new Point2D.Float(0.0F, 0.0F) ;		
			nose_position =new Point2D.Float(0.0F, 1.0F);
			updatePosition();
		}
		
		public void translate(float x, float y){
			t.translate(x, y);
			updatePosition();
		}
		
		public void rotate(float theta){
			t.rotate(theta);
			updatePosition();
		}
		
		private void updatePosition(){
			position = t.transform(position_standard, null);
			nose_position = t.transform(nose_standard, null);
		}
	}
