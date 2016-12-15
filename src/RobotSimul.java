import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.math.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;


public class RobotSimul extends BasicGame {
	public static int DISPLAY_X=600, DISPLAY_Y=400;
			
	public RobotSimul(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	
	public ArrayList<Wall> walls = new ArrayList<Wall>();
	
	float alpha = 96F;
	
	
	float moveValue = 5.0f;
	float rotateValue = (float)(-Math.PI*2.0/16.0);
	
	float moveError = moveValue * 0.4f;
	float rotateError = moveValue * 0.4f;
	
	float obsMoveError = moveValue * 0.01f; //observing other robot error
	float obsRotateError = moveValue * 0.01f;
	
	float sampleErrorSize = 3F;	
	
	public static final float maxSense = 100f;
	
	public static Socket socket;

	
	ArrayList<AffineObject> robotList = new ArrayList<AffineObject>();
	AffineObject masterRobot;
	AffineObject currRobot;
	int robotIdx = 0;
	
	
	public static void main(String[] args) throws SlickException, IOException {
		AppGameContainer appgc = new AppGameContainer(new RobotSimul("RobotSimul"));
		
        ServerSocket myServerSocket = new ServerSocket(9999);
        socket = myServerSocket.accept(); 
        
		appgc.setDisplayMode(DISPLAY_X, DISPLAY_Y, false);
		appgc.setTargetFrameRate(10);
		appgc.start();
		
	}


	
	public boolean bounceOff = false;
	public float resol = 0.5F;
	
	
	public void sendRobotInit(Point2D robotPosition){
		 try 
        {
            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            objectOutput.writeUTF("robotInit");
            objectOutput.writeObject(robotPosition);               
            System.out.println("send ri: " + robotPosition.toString());
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
	}
	
	public void sendSample(int robotIdx, List<Point2D> sampleList){
		 try 
         {
             ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
             objectOutput.writeUTF("sample");
             objectOutput.writeInt(robotIdx);
             objectOutput.writeObject(sampleList);               
             System.out.println("send ok : " + sampleList.toString());
         } 
         catch (IOException e) 
         {
             e.printStackTrace();
         } 
	}
	
	public void sendMove(int robotIdx, float move, float rotate){
		 try 
        {
            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            objectOutput.writeUTF("move");
            Float[] data = new Float[]{move, rotate};
            objectOutput.writeInt(robotIdx);
            objectOutput.writeObject(data);               
            System.out.println("send ok : " + data.toString());
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
	}
	
	public void sendRelatvie(){
		int robotSize = robotList.size();
		ArrayList<AffineTransform> relativeAFList = new ArrayList<AffineTransform>();
		for(int i=1 ;i<robotSize; i++){
			AffineObject robot = robotList.get(i);
			AffineTransform relativeAffine = (AffineTransform)masterRobot.t.clone();
			try {
				relativeAffine.invert();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			relativeAffine.concatenate(robot.t);
			relativeAFList.add(relativeAffine);
		}
		
		 try 
	        {
	            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
	            objectOutput.writeUTF("relative");
	            objectOutput.writeObject(relativeAFList);
	                           
	            System.out.println("send relative");
	        } 
	        catch (IOException e) 
	        {
	            e.printStackTrace();
	        }
	}
	
	public Point2D rayCast(float startx, float starty, float diffx, float diffy, Graphics g, int steplimit, GameContainer gc) {
		float dx = startx;
		float dy = starty;
		float latx = startx;
		float laty = starty;
		boolean dox = true;
		boolean isFar = false;
		int step = 0;
		for (Wall w : walls) {
			if (dx > w.x && dx < w.x + w.w && dy > w.y && dy < w.y + w.h) {
				dox = false;
				break;
			}
		}
		if (dox) {
			while (true) {
				step++;
				if (step > steplimit) {
					if (!(infiniteBounce)) break;
				}
				float lx = dx;
				dx += diffx;			
				float ly = dy;
				dy += diffy;

				if (!(bounceOff)) {
					boolean outerbreak = false;
					for (Wall w : walls) {
						if (dx > w.x && dx < w.x + w.w && dy > w.y && dy < w.y + w.h) {
							outerbreak = true;
							break;
						}
					}
					if (outerbreak) break;
				}
				
				
				if (Math.pow(dx-latx,2.0)+Math.pow(dy-laty,2.0) > Math.pow(maxSense,2.0)){
					isFar = true;
					break;
				};
				if (dx > gc.getWidth()) break;
				if (dx < 0) break;
				if (dy > gc.getHeight()) break;
				if (dy < 0) break;
			}

		}
		
		if (g != null){
			if (isFar){
				
			}
			
			g.drawGradientLine(latx, laty, new Color(255, 0, 0, 255), dx, dy, new Color(255, 0, 0, 0));
//			else if (gradient) {
//				g.drawGradientLine(latx, laty, new Color(255, 0, 0, 255), dx, dy, new Color(255, 0, 0, 0));
//			}else{
//				g.drawLine(latx, laty, dx, dy);
//			}
		}
		
		if(isFar){
			return null;
		}else{
			return new Point2D.Float(dx, dy); 
		}
		
	}
	
	public LinkedList<Point2D> sampleAround(GameContainer gc, AffineObject sampler){
		LinkedList<Point2D> results = new LinkedList<Point2D>();
		AffineTransform invT;
		Random random = new Random();
		try {
			invT = sampler.t.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			return null;
		}
		int delta = 360/8;
		for(int i=0; i<360; i+=delta){
			double radian;
			radian = Math.toRadians(i);
			float diffx=(float)Math.cos(radian);
			float diffy=(float)Math.sin(radian);
			Point2D samplePoint = rayCast((float)sampler.position.getX(), (float)sampler.position.getY(), diffx, diffy, null, stepLimit, gc);
			if(samplePoint != null){
				samplePoint.setLocation(samplePoint.getX()+random.nextGaussian()*sampleErrorSize, samplePoint.getY()+random.nextGaussian()*sampleErrorSize);
				invT.transform(samplePoint , samplePoint);
				results.add(samplePoint);
			}else{
				results.add(null);
			}
		}
		return results;
	}

	
	boolean renderPlat = true;
	
	boolean dragging = false;
	int startx = 0;
	int starty = 0;
	int endx = 0;
	int endy = 0;
	
	public boolean gradient = false;
	
	public int stepLimit = 10000;
	
	public boolean infiniteBounce = false;
	
	
	public float goX = 0F;
	public float goY = 0F;
	
	//public AffineObject robot_actual = new AffineObject(AffineTransform.getTranslateInstance(DISPLAY_X*3.0/4.0, DISPLAY_Y*1.0/4.0));
	public ArrayList<AffineObject> robot_expected_list = new ArrayList<AffineObject>();
	
	
	public void renderOval(Graphics g, Point2D point, float ovalSize){
		g.drawOval((float)point.getX()-ovalSize/2.0F, (float)point.getY()-ovalSize/2.0F, ovalSize, ovalSize);
	}
	
	public void drawRobot(Graphics g, AffineObject robot_actual){
		float robotOvalSize = 10.0F;
				
		//draw_master_robot
		renderOval(g, robot_actual.position, robotOvalSize);
		g.drawLine((float)robot_actual.position.getX(), (float)robot_actual.position.getY(), 
				(float)robot_actual.nose_position.getX(), (float)robot_actual.nose_position.getY());
	}
	
	public GameContainer gameContainer;
	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		gameContainer = gc;
		
		g.setColor(new Color(255, 0, 0, (int)alpha));
		g.setLineWidth(1f);
		
		
		//draw sampling line		
		int delta = 360/8;
		for(int i=0; i<360; i+=delta){
			double radian;
			radian = Math.toRadians(i);
			float diffx=(float)Math.cos(radian);
			float diffy=(float)Math.sin(radian);
			rayCast((float)currRobot.position.getX(), (float)currRobot.position.getY(), diffx, diffy, g, stepLimit, gc);
		}	
	
		// render wall
		if (renderPlat) {
			g.setColor(Color.white);
			for (Wall w : walls) {
				g.fillRect(w.x, w.y, w.w, w.h);
			}
		}
		
		// render affine object
		g.setColor(Color.red);
		
		
		// render actual robot position
		g.setColor(Color.blue);		
		float sampleOvalSize = 5.0F;
				
				
		//draw_master robot
		drawRobot(g, masterRobot);
		
		
		for(Point2D sample : sampleForRender){
			if(sample != null)
				renderOval(g, sample,sampleOvalSize );
		}
		
		if (dragging) {
			g.setLineWidth(2f);
			g.setColor(Color.green);
			int sx = startx;
			int sy = starty;
			int ex = endx;
			int ey = endy;
			if (sx > ex) {
				int j1 = sx;
				int j2 = ex;
				ex = j1;
				sx = j2;
			}
			if (sy > ey) {
				int j1 = sy;
				int j2 = ey;
				ey = j1;
				sy = j2;
				
			}
			g.drawRect(sx, sy, ex - sx, ey - sy);
		}
		g.setColor(Color.green);
		g.drawString("sampleErrorSize : " + sampleErrorSize +"\n", 5, 30);
	}

	@Override
	public void init(GameContainer g) throws SlickException {
		//init master robot
		 masterRobot = initRobot(DISPLAY_X*3.0/4.0, DISPLAY_Y*1.0/4.0);
		 currRobot = masterRobot;
	}
	
	
	
	public boolean reactLeft = true;
	public boolean reactRight = true;
	public boolean reactMiddle = true;
	
	public boolean GetCollision(float playerX, float playerY, float playerWidth,
            float playerHeight, float EnemyX, float EnemyY, float EnemyWidth,
            float EnemyHeight)
        {
            if ((playerX + playerWidth) > EnemyX &&
                                    (playerX) < EnemyX + EnemyWidth &&
                                    (playerY + playerHeight) > EnemyY &&
                                    (playerY) < EnemyY + EnemyHeight)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

	
	public void saveMap(){
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("map.tmp");

			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(walls);
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadMap(){
		FileInputStream fis;
		try {
			fis = new FileInputStream("map.tmp");

			ObjectInputStream ois = new ObjectInputStream(fis);
			walls = (ArrayList<Wall>) ois.readObject();
			ois.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	LinkedList<Point2D> sampleForRender = new LinkedList<Point2D>();
	
	public AffineObject initRobot(double d, double e){
		AffineObject robot_affine = new AffineObject(AffineTransform.getTranslateInstance(d, e));
		robotList.add(robot_affine);
		return robot_affine;
	}
	
	Random rnd = new Random();
	public void keyPressed(int key, char c) {
//		if (c == 'h' || c == 'H') {
//			gradient = !gradient;
//		}
//		if (c == 'j' || c == 'J') {
//			bounceOff = !bounceOff;
//		}
//		if (c == 'k' || c == 'K') {
//			infiniteBounce = !infiniteBounce;
//		}
//		if (c == 'u' || c == 'U') {
//			renderPlat = !renderPlat;
//		}
		if(c=='1'){
			System.out.println("load map");
	        loadMap();
		}
		if(c=='0'){
			System.out.println("save map");
	        saveMap();
		}
		if(c=='n'){
			robotIdx = (robotIdx+1)%robotList.size();
			currRobot = robotList.get(robotIdx);
			System.out.println("selectNextRobot");
		}
		
		boolean obs;
		if (robotList.size() ==1 ){
			obs = false;
		}else{
			obs = true;
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			float realRotate = rotateValue + (rnd.nextFloat()*2-1)*rotateError;			
			currRobot.rotate(realRotate);
			if(obs == false){
				sendMove(robotIdx, 0, rotateValue);	
			}else{
				sendMove(robotIdx, 0, realRotate + (rnd.nextFloat()*2-1)*obsRotateError);
			}
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			float realRotate = -rotateValue + (rnd.nextFloat()*2-1)*rotateError;			
			currRobot.rotate(realRotate);
			if(obs == false){
				sendMove(robotIdx, 0, -rotateValue);	
			}else{
				sendMove(robotIdx, 0, realRotate + (rnd.nextFloat()*2-1)*obsRotateError);
			}
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			float realMove = moveValue + (rnd.nextFloat()*2-1)*rotateError;			
			currRobot.translate(0, realMove);
			if(obs == false){
				sendMove(robotIdx, moveValue, 0);	
			}else{
				sendMove(robotIdx, realMove + (rnd.nextFloat()*2-1)*obsMoveError ,0);
			}
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			float realMove = -moveValue + (rnd.nextFloat()*2-1)*rotateError;			
			currRobot.translate(0, realMove);
			if(obs == false){
				sendMove(robotIdx, -moveValue, 0);	
			}else{
				sendMove(robotIdx, realMove + (rnd.nextFloat()*2-1)*obsMoveError ,0);
			}
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)){
			LinkedList<Point2D> results = sampleAround(gameContainer, currRobot);
			//sampleForRender = new LinkedList<Point2D>();
			sendSample(robotIdx, results);
			//System.out.println("robot_actual : " + currRobot.t.toString());
			//System.out.println("robot_actual : " + currRobot.position.toString());
			for(Point2D result : results){
				if(result != null){
					currRobot.t.transform(result, result);
				}
				//sampleForRender.add(result);
			}
			sampleForRender = results;
			System.out.println("sample for render : " + sampleForRender.toString());
			
			
		}
	
	}
	
	@Override
	public void update(GameContainer gc, int delta) throws SlickException {
		if (Mouse.isButtonDown(0)) {
			if (reactLeft) {
				//MOUSE DOWN
				startx = Mouse.getX();
				starty = gc.getHeight() - Mouse.getY();
				endx = Mouse.getX();
				endy = gc.getHeight() - Mouse.getY();
				dragging = true;
				reactLeft = false;
			}
		}else{
			if (!(reactLeft)) {
				//MOUSE UP
				int sx = startx;
				int sy = starty;
				int ex = endx;
				int ey = endy;
				if (sx > ex) {
					int j1 = sx;
					int j2 = ex;
					ex = j1;
					sx = j2;
				}
				if (sy > ey) {
					int j1 = sy;
					int j2 = ey;
					ey = j1;
					sy = j2;
				}
				walls.add(new Wall(sx, sy, ex - sx, ey - sy));
				dragging = false;
				reactLeft = true;
			}
		}
		if (Mouse.isButtonDown(1)) {
			if (reactRight) {
				//MOUSE DOWN
				startx = Mouse.getX();
				starty = gc.getHeight() - Mouse.getY();
				endx = Mouse.getX();
				endy = gc.getHeight() - Mouse.getY();
				dragging = true;
				reactRight = false;
			}
		}else{
			if (!(reactRight)) {
				//MOUSE UP
				int sx = startx;
				int sy = starty;
				int ex = endx;
				int ey = endy;
				if (sx > ex) {
					int j1 = sx;
					int j2 = ex;
					ex = j1;
					sx = j2;
				}
				if (sy > ey) {
					int j1 = sy;
					int j2 = ey;
					ey = j1;
					sy = j2;
				}
				List<Wall> remwall = new ArrayList<Wall>();
				for (Wall w : walls) if (GetCollision(w.x, w.y, w.w, w.h, sx, sy, ex - sx, ey - sy)) remwall.add(w);
				for (Wall w : remwall) walls.remove(w);
				dragging = false;
				reactRight = true;
			}
		}
		if (dragging) {
			endx = Mouse.getX();
			endy = gc.getHeight() - Mouse.getY();
		}
		
		
		goX = Mouse.getX();
		goY = gc.getHeight() - Mouse.getY();
		
		if (Mouse.isButtonDown(1)) {
			if (reactMiddle){
				reactMiddle = false;	
			}
			if(reactMiddle){
				initRobot(Mouse.getX(), Mouse.getY());
				reactMiddle = true;
			}
			
		}
		


		
	}
	
	



}