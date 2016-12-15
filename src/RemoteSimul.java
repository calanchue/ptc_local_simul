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
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class RemoteSimul extends BasicGame{
	public RemoteSimul(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}


	public static int DISPLAY_X=600, DISPLAY_Y=400;
			

	/**
	 * @param args
	 */
	
	public ArrayList<Wall> walls = new ArrayList<Wall>();
	
	float rayPointX = DISPLAY_X/2.0F;
	float rayPointY = DISPLAY_Y/2.0F;
	float alpha = 96F;
	
	
	float moveValue = 5.0F;
	float rotateValue = (float)(-Math.PI*2.0/16.0); 
	float sampleErrorSize = 3F;	
	float moveErrorSize = (float) (moveValue*0.3F);
	float rotateErrorSize = (float)(rotateValue*0.3F);
	
	boolean renderSampleFromProb = true;
	
	int probSize = 1000;
	
	ArrayList<AffineObject> robotList = new ArrayList<AffineObject>();	
	AffineObject masterRobot, currRobot;
	
	
	public static Socket socket;
	public static class Receiver extends Thread{
		RemoteSimul bg;
		public Receiver(RemoteSimul bg) throws UnknownHostException, IOException{
			socket = new Socket("127.0.0.1",9999);
			this.bg = bg;
			System.out.println("created connection");
		}
		
		public void run() {
	        while(true){
	            try {
	            	ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream()); //Error Line!
	                String command= objectInput.readUTF();
	                if(command.equals("sample")){
	                	int robotIdx = objectInput.readInt();
		                Object object = objectInput.readObject();
		                LinkedList<Point2D> results = (LinkedList<Point2D>) object;
		                System.out.println("receive sp : "+robotIdx+"/" + results.size());
		                bg.getSampleFromOut(robotIdx, results);
	                }else if(command.equals("move")){
	                	int robotIdx = objectInput.readInt();
	                	Object object = objectInput.readObject();
		                Float[] results = (Float[]) object;
		                System.out.println("receive mv : " +robotIdx +  "/" + results[0].toString() + "/" + results[1].toString());
		                bg.sampleMoveProces(robotIdx, results[0], results[1]);
	                }else if(command.equals("robotInit")){
	                	Object object = objectInput.readObject();
	                	Point2D results = (Point2D) object;
		                System.out.println("receive ri : " + results.toString());
		                bg.initRobot(results);
	                }
	                

	                
	    			
	            } catch (ClassNotFoundException | IOException e) {
	                System.out.println("The title list has not come from the server");
	                e.printStackTrace();
	            }
	        }
	    }
	}

		

	public static void main(String[] args) throws SlickException, UnknownHostException, IOException {
		RemoteSimul bg = new RemoteSimul("RemoteSimul");
		
		
		new Receiver(bg).start();
		
		AppGameContainer appgc = new AppGameContainer(bg);
		appgc.setAlwaysRender(true);
		appgc.setDisplayMode(DISPLAY_X, DISPLAY_Y, false);
		appgc.setTargetFrameRate(5);
		appgc.start();
		
	}
	
	public boolean bounceOff = false;
	public float resol = 0.5F;
	
	public static final float maxSense = 100f;
	
	
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
			else if (gradient) {
				g.drawGradientLine(latx, laty, new Color(255, 0, 0, 255), dx, dy, new Color(255, 0, 0, 0));
			}else{
				g.drawLine(latx, laty, dx, dy);
			}
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
		
		float robotOvalSize = 10.0F;
		float noseOvalSize = 5.0F;
		float sampleOvalSize = 5.0F;

		Color probPosColor = Color.gray;
		probPosColor.a=0.2f;
		g.setColor(probPosColor);
		if(renderSampleFromProb){
			synchronized(sampleFromProbForRender){
				for(Point2D sample : sampleFromProbForRender){
					if (sample !=null){
						renderOval(g, sample,sampleOvalSize);
					}	
				}
			}
		}
		
		double sumProbX = 0;
		double sumProbY = 0;
		double sumProbNoseX = 0;
		double sumProbNoseY = 0;
		Color probPosSampleColor = Color.lightGray;
		probPosSampleColor.a=0.2f;
		g.setColor(probPosSampleColor);
		for(AffineObject ao : probPosList){
			renderOval(g, ao.position, robotOvalSize);
			g.drawLine((float)ao.position.getX(), (float)ao.position.getY(), 
					(float)ao.nose_position.getX(), (float)ao.nose_position.getY());
			
			sumProbX += ao.position.getX();
			sumProbY += ao.position.getY();
			sumProbNoseX += ao.nose_position.getX();
			sumProbNoseY += ao.nose_position.getY();			
		}
		
		
		// render actual robot position
		g.setColor(Color.blue);		
			
		drawRobot(g, masterRobot);
		
		LinkedList<Point2D> forRender = getSampleForRender();
		for(Point2D sample : forRender){
			if (sample !=null){
				renderOval(g, sample,sampleOvalSize);
			}
		}
		
		//mean probable
		g.setColor(Color.magenta);
		double meanProbX = sumProbX/(double)probSize;
		double meanProbY = sumProbY/(double)probSize;
		double meanNoseX = sumProbNoseX/(double)probSize;
		double meanNoseY = sumProbNoseY/(double)probSize;
		renderOval(g, new Point2D.Double(meanProbX, meanProbY), robotOvalSize);
		g.drawLine((float)meanProbX,(float)meanProbY,(float)meanNoseX,(float)meanNoseY);
		
		
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

	ArrayList<AffineObject> probPosList = new ArrayList<>();
	
	@Override
	public void init(GameContainer g) throws SlickException {
		//set random probable position
		Random random = new Random();
		for(int i =0; i<probSize ; i++){
			AffineTransform at = AffineTransform.getTranslateInstance(random.nextFloat()*DISPLAY_X, random.nextFloat()*DISPLAY_Y);
			at.rotate(random.nextFloat()*Math.PI*2.0);
			probPosList.add(new AffineObject(at));
		}
		
//		AffineObject nearRobot = new AffineObject(robot_actual.t);
//		nearRobot.translate(1.0f, 0);
//		probPosList.add(nearRobot);
	}
	
	
	
	public boolean reactLeft = true;
	public boolean reactRight = true;
	
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
	
	LinkedList<Point2D> realSampleListForRender = new LinkedList<Point2D>();
	LinkedList<Point2D> realSampleList = new LinkedList<Point2D>();
	
	public synchronized LinkedList<Point2D> getSampleForRender(){
		return realSampleListForRender;
	}
	
	public synchronized void setSampleForRender(LinkedList<Point2D> list){
		realSampleListForRender = list;
	}
	
	public synchronized void getSampleFromOut(int robotIdx, LinkedList<Point2D> results){
		realSampleList = results;
		System.out.println("rest render sample : " + results.toString());
		System.out.println("robot_actual : " + robot_actual.t.toString());									
		LinkedList<Point2D> realSampleListForRenderTemp = new LinkedList<Point2D>();
		
		for(Point2D result : results){
			Point2D renderPoint = new Point2D.Double();
			if(result != null)
				robot_actual.t.transform(result, renderPoint);
			realSampleListForRenderTemp.add(renderPoint);
		}
		setSampleForRender(realSampleListForRenderTemp);
		System.out.println("sample for render : " + realSampleListForRender.toString());
		resampling();
	}
	
	LinkedList<Point2D> sampleFromProbForRender = new LinkedList<>();
	double maxSenseSq = Math.pow(maxSense, 2.0); 
		
	public int processState = 0;
	
	public synchronized void resampling(){		
		ArrayList<Double> weightList = new ArrayList<>();
		LinkedList<Point2D> sampleFromProbForRenderTemp = new LinkedList<>();
		
		if (processState == 0){//calculate score
			for(AffineObject ao : probPosList){
				LinkedList<Point2D> results = sampleAround(gameContainer, ao);
				
				if(renderSampleFromProb){
					for(Point2D result : results){
						Point2D renderPoint = new Point2D.Double();
						ao.t.transform(result, renderPoint);
						System.out.printf("render point : %s", renderPoint.toString());
						//robot_actual.t.transform(result, result);
						sampleFromProbForRenderTemp.add(renderPoint);	
					}					
				}
				
				double weight = 1.0;
				for(int i=0; i< results.size(); i++){
					Point2D realSample = realSampleList.get(i);
					Point2D probSample = results.get(i);
					if(realSample != null && probSample != null){
						weight *= Math.exp(-realSample.distanceSq(probSample)/maxSenseSq);
					}else if(realSample != null && probSample == null){
					 	weight *= Math.exp(-Math.pow(maxSense - realSample.distance(0.0, 0.0),2.0)/maxSenseSq);
					}else if(realSample == null && probSample != null){
						weight *= Math.exp(-Math.pow(maxSense - probSample.distance(0.0, 0.0),2.0)/maxSenseSq);
					}else if(realSample == null && probSample == null){
						weight *=1;
					}
				}
				weightList.add(weight);
			}
		}
		synchronized(sampleFromProbForRender){
			sampleFromProbForRender = sampleFromProbForRenderTemp;
		}
		
		System.out.printf("probLen : %d weightLen : %d\n", probPosList.size(), weightList.size());
		
		Random random = new Random();
		int n = probPosList.size();
		ArrayList<AffineObject> newProbPosList = new ArrayList<>();
		int index = random.nextInt(n);
		double beta = 0.0;
		double mw = Collections.max(weightList);
		for(int i = 0; i<n; i++){
			beta += random.nextDouble() *2.0*mw;
			while(beta>weightList.get(index)){
				beta -=weightList.get(index);
				index = (index+1)%n;
			}
			newProbPosList.add(new AffineObject(new AffineTransform(probPosList.get(index).t)));
		}
		System.out.printf("new:%d, old:%d", newProbPosList.size(), probPosList.size());
		probPosList = newProbPosList;
		
	}
	
	public static Random smr = new Random();
	public synchronized void sampleMoveProces(int robotIdx, float move, float rotation){
		
		for (AffineObject ao: probPosList){
			if (move != 0){
				ao.translate(0, move+(float)smr.nextGaussian()*moveErrorSize);		
			}
			if (rotation!=0){
				ao.rotate(rotation+(float)smr.nextGaussian()*rotateErrorSize);	
			}
		}
		
		if (move != 0){
			robot_actual.translate(0, move);		
		}
		if (rotation!=0){
			robot_actual.rotate(rotation);	
		}
	}
	
	public synchronized void initRobot(Point2D position){
		masterRobot = new AffineObject(AffineTransform.getTranslateInstance(position.getX(), position.getY()));
		currRobot = masterRobot;
		robotList.add(masterRobot);		
	}
	
	public void keyPressed(int key, char c) {
		if(c=='1'){
			System.out.println("load map");
	        loadMap();
		}
		if(c=='0'){
			System.out.println("save map");
	        saveMap();
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
		


//		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
//			resol += delta * .0005F;
//		}
//		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
//			if (resol > 0.01F && delta * 0.0005F < 0.01F) resol -= delta * .0005F;
//		}
//		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
//			stepLimit += 5;
//		}
//		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
//			if (stepLimit > 0) stepLimit -= 5;
//		}
//		if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
//			if (alpha < 256) alpha += 0.1F;
//		}
//		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
//			if (alpha > 0) alpha -= 0.1F;
//		}



		
	}

}