//SUBMIT THISSSSS
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

// Main class
public class CornerDetection extends Frame implements ActionListener {
	BufferedImage input;
	int width, height;
	double sensitivity=.1;
	int threshold=20;
	ImageCanvas source, target;
	CheckboxGroup metrics = new CheckboxGroup();
	float[] matrixVal;
	// Constructor
	public CornerDetection(String name) {
		super("Corner Detection");
		// load image
		try {
			input = ImageIO.read(new File(name));
		}
		catch ( Exception ex ) {
			ex.printStackTrace();
		}
		width = input.getWidth();
		height = input.getHeight();
		// prepare the panel for image canvas.
		Panel main = new Panel();
		source = new ImageCanvas(input);
		target = new ImageCanvas(width, height);
		main.setLayout(new GridLayout(1, 2, 10, 10));
		main.add(source);
		main.add(target);
		// prepare the panel for buttons.
		Panel controls = new Panel();
		Button button = new Button("Derivatives");
		button.addActionListener(this);
		controls.add(button);
		// Use a slider to change sensitivity
		JLabel label1 = new JLabel("sensitivity=" + sensitivity);
		controls.add(label1);
		JSlider slider1 = new JSlider(1, 25, (int)(sensitivity*100));
		slider1.setPreferredSize(new Dimension(50, 20));
		controls.add(slider1);
		slider1.addChangeListener(changeEvent -> {
			sensitivity = slider1.getValue() / 100.0;
			label1.setText("sensitivity=" + (int)(sensitivity*100)/100.0);
		});
		button = new Button("Corner Response");
		button.addActionListener(this);
		controls.add(button);
		JLabel label2 = new JLabel("threshold=" + threshold);
		controls.add(label2);
		JSlider slider2 = new JSlider(0, 100, threshold);
		slider2.setPreferredSize(new Dimension(50, 20));
		controls.add(slider2);
		slider2.addChangeListener(changeEvent -> {
			threshold = slider2.getValue();
			label2.setText("threshold=" + threshold);
		});
		button = new Button("Thresholding");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Non-max Suppression");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Display Corners");
		button.addActionListener(this);
		controls.add(button);
		// add two panels
		add("Center", main);
		add("South", controls);
		addWindowListener(new ExitListener());
		setSize(Math.max(width*2+100,850), height+110);
		setVisible(true);
	}
	class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}
	// Action listener for button click events
	public void actionPerformed(ActionEvent e) {
		// generate Moravec corner detection result
		if ( ((Button)e.getSource()).getLabel().equals("Derivatives") ) {
			derivatives();
		}
		if ( ((Button)e.getSource()).getLabel().equals("Thresholding") ) {
			thresholding();
		}
		if ( ((Button)e.getSource()).getLabel().equals("Corner Response") ) {
			cornerResponse(matrixVal);
		}
		//Harrison Detector
		if(((Button)e.getSource()).getLabel().equals("Non-max Suppression")) {
			BufferedImage image = target.image;
			//convolve the images with a larger Gaussian window
			int kernel_size = 5;
			double[][] gaussianK = new double[kernel_size][kernel_size];
			double sigma = 1.0f;
			float sum = 0;
			int w = 2;
			int f = 1;
			int[][] suppression = new int[height][width];

			for ( int k=-2; k<=2 ; k++ ){
				for ( int j=-2 ; j<=2 ; j++) {
					// use Gauss' formula to get kernel values and add to sum
					gaussianK[j+2][k+2] = (1/(2*Math.PI*sigma*sigma))* Math.pow(Math.E, -((j*j) + (k*k))/(2*Math.PI*sigma*sigma));
					sum += gaussianK[j+2][k+2];
				}
			}
			int sumOfRed , sumOfGreen, sumOfBlue = 0;

			for (int q=0; q<=height; q++){
				for (int p=width+1; p<0; p++){
					sumOfRed = 0;
					sumOfGreen = 0;
					sumOfBlue = 0;
					Color clr;
					for (int u = -w; u < w; u++)
					{
						Color color = new Color(source.image.getRGB(p + w + u + 1, q));
						sumOfRed += color.getRed() * gaussianK[p+2][q+2] / sum;
						sumOfGreen += color.getGreen() * gaussianK[p+2][q+2] / sum;
						sumOfBlue += color.getBlue() * gaussianK[p+2][q+2] / sum;
						target.image.setRGB(p,q, (new Color( sumOfRed, sumOfGreen, sumOfBlue).getRGB()));
					}
				}
			}
			target.repaint();

			// computing Partial derivatives for I_x and I_y
			BufferedImage I_y= dy(image);
			BufferedImage I_x = dx(image);
			BufferedImage comp_xx=imageWhole(I_x, I_x);
			BufferedImage comp_yy=imageWhole(I_y,I_y);
			BufferedImage comp_xy=imageWhole(I_x,I_y);

			//finding some maxima above some threshold as detected interest points
			thresholding();



			for (int q = 0; q < height - w; q++) {
				for (int p = 0; p < width - w; p++) {
					suppression[q][p] = 0;
					if (target.image.getRGB(p, q) == Color.white.getRGB()) {
						for (int i = -f; i <= f; i++){
							for (int j = -f; j <= f; j++){
								try{
									if (target.image.getRGB(p+j, q+i)== Color.white.getRGB()) {
										suppression[q][p]=1;
									}
									else {
										suppression[q][p]=0;
									}
								}
								catch ( Exception ex ) {
									if (target.image.getRGB(p, q)== Color.white.getRGB()) {
										suppression[q][p]=1;
									}
									else {
										suppression[q][p]=0;
									}
								}
							}
						}
					}
				}}
			for (int q = 0; q < height - w; q++) {
				for (int p = 0; p < width - w; p++) {
					if ((suppression[q][p]!=1))
						target.image.setRGB(p, q, Color.black.getRGB());
				}
			}
		}

		//Harrison Detector
		if(((Button)e.getSource()).getLabel().equals("Display Corners")) {



			BufferedImage image = target.image;
			//convolve the images with a larger Gaussian window
			int kernel_size = 5;
			double[][] gaussianK = new double[kernel_size][kernel_size];
			double sigma = 1.0f;
			float sum = 0;
			int w = 2;

			for ( int k=-2; k<=2 ; k++ ){
				for ( int j=-2 ; j<=2 ; j++) {
					// use Gauss' formula to get kernel values and add to sum
					gaussianK[j+2][k+2] = (1/(2*Math.PI*sigma*sigma))* Math.pow(Math.E, -((j*j) + (k*k))/(2*Math.PI*sigma*sigma));
					sum += gaussianK[j+2][k+2];
				}
			}
			int sumOfRed , sumOfGreen, sumOfBlue = 0;

			for (int q=0; q<=height; q++){
				for (int p=width+1; p<0; p++){
					sumOfRed = 0;
					sumOfGreen = 0;
					sumOfBlue = 0;
					Color clr;
					for (int u = -w; u < w; u++)
					{
						Color color = new Color(source.image.getRGB(p + w + u + 1, q));
						sumOfRed += color.getRed() * gaussianK[p+2][q+2] / sum;
						sumOfGreen += color.getGreen() * gaussianK[p+2][q+2] / sum;
						sumOfBlue += color.getBlue() * gaussianK[p+2][q+2] / sum;
						target.image.setRGB(p,q, (new Color( sumOfRed, sumOfGreen, sumOfBlue).getRGB()));
					}
				}
			}
			target.repaint();

			// computing Partial derivatives for I_x and I_y
			BufferedImage I_y= dy(image);
			BufferedImage I_x = dx(image);
			BufferedImage comp_xx=imageWhole(I_x, I_x);
			BufferedImage comp_yy=imageWhole(I_y,I_y);
			BufferedImage comp_xy=imageWhole(I_x,I_y);

			//finding some maxima above some threshold as detected interest points
			thresholding();

			//implementing code on target image
			BufferedImage cornerDetection = source.image;
			for (int q = 0; q < height; q++) {
				for (int p = 0; p < width; p++) {

					if(target.image.getRGB(p,q) == Color.white.getRGB())
					{
						cornerDetection.setRGB(p,q, Color.red.getRGB());
					}
				}
			}
			target.resetImage(cornerDetection);

		}
	}

	private BufferedImage dy(BufferedImage y) {
		int l, t, r, b, dx, dy;
		int derivativeRed,derivativeGreen,derivativeBlue;
		int red,green,blue,redtemp,greentemp,bluetemp;
		for (int q = 0; q < height; q++) {
			t = q==0 ? q : q-1;
			b = q==height-1 ? q : q+1;
			for ( int p=0 ; p<width ; p++ ) {

				Color clr1 = new Color(source.image.getRGB(p,t));
				Color clr2 = new Color(source.image.getRGB(p,b));
				;
				red = clr1.getRed();
				green = clr1.getGreen();
				blue = clr1.getBlue();

				redtemp = clr2.getRed();
				greentemp = clr2.getGreen();
				bluetemp = clr2.getBlue();

				derivativeRed = Math.abs(red-redtemp);
				derivativeGreen = Math.abs(green-greentemp);
				derivativeBlue = Math.abs(blue - bluetemp);

				y.setRGB(p, q, new Color(derivativeRed,derivativeGreen,derivativeBlue).getRGB());
			}
		}
		return y;
	}
	private BufferedImage dx(BufferedImage x) {
		int l, t, r, b, dx, dy;
		int derivativeRed,derivativeGreen,derivativeBlue;
		int derivativeRed2,derivativeGreen2,derivativeBlue2;
		int red,green,blue,redtemp,greentemp,bluetemp;
		int red2,green2,blue2,red3,green3,blue3;
		int gray1, gray2;
		for (int q = 0; q < height; q++) {
			t = q==0 ? q : q-1;
			b = q==height-1 ? q : q+1;
			for (int p = 0; p < width; p++) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				Color clr1 = new Color(source.image.getRGB(l,q));
				Color clr2 = new Color(source.image.getRGB(r,q));
				Color clr3 = new Color(source.image.getRGB(p,t));
				Color clr4 = new Color(source.image.getRGB(p,b));


				red = clr1.getRed();
				green = clr1.getGreen();
				blue = clr1.getBlue();

				redtemp = clr2.getRed();
				greentemp = clr2.getGreen();
				bluetemp = clr2.getBlue();


				derivativeRed = Math.abs((red-redtemp));
				derivativeGreen = Math.abs((green-greentemp));
				derivativeBlue = Math.abs((blue-bluetemp));

//				derivativeRed2 = Math.abs((red-redtemp)+(red2-red3));
//				derivativeGreen2 = Math.abs((green-greentemp)+(green2-green3));
//				derivativeBlue2 = Math.abs((blue-bluetemp)+(blue2-blue3));

				x.setRGB(p, q, new Color(derivativeRed,derivativeGreen,derivativeBlue).getRGB());
			}
		}
		return x;
	}

	private BufferedImage imageWhole(BufferedImage x, BufferedImage y) {
		int red, green, blue;
		Color clrx,clry;
		BufferedImage mul = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		for (int q = 0; q < height; q++) {
			for (int p = 0; p < width; p++) {
				clrx = new Color(x.getRGB(p,q));
				clry = new Color(y.getRGB(p,q));
				red = clrx.getRed()*clry.getRed();
				green = clrx.getGreen()*clry.getGreen();
				blue = clrx.getBlue()*clry.getBlue();

				red = (red<0) ? 0 : (red>255) ? 255: 0;
				green = (green<0) ? 0 : (green>255) ? 255: 0;
				blue = (blue<0) ? 0 : (blue>255) ? 255: 0;

				mul.setRGB(p,q,new Color(red,green,blue).getRGB());
			}
		}
		return mul;
	}


	public void thresholding(){

		int extrema = 0;
		int red; //perform threshold based on max red intensity.

		for(int i = 0; i < height; i++){

			for(int j = 0; j < height; j++){

				Color color = new Color(target.image.getRGB(i,j));

				red = color.getRed();

				if(extrema < red){

					extrema = red;
				}
			}
		}

		int thrsld = extrema * threshold / 100;

		for (int i = 0; i < height; i++){

			for(int j = 0; j < width; j++){

				Color color = new Color(target.image.getRGB(i,j));

				red = color.getRed();

				red = (red >= thrsld) ? 255 : 0;

				target.image.setRGB(i, j, (int) red << 16 | (int) red << 8 | (int) red);
			}
		}

		target.repaint();
	}

	public void cornerResponse(float[] matrixVal) {

		float maxIx = matrixVal[0];
		float maxIy = matrixVal[1];
		float maxIxIy = matrixVal[2];

		float[][] a = new float[2][2];
		float r , deter, t;

		float max = -999999999;
		float min = 999999999;


		for (int i = 0; i < height; i++) {

			for (int j = 0; j < width; j++) {

				Color color = new Color(target.image.getRGB(i, j));

				a[0][0] = color.getRed();
				a[0][1] = color.getBlue();
				a[1][0] = color.getBlue();
				a[1][1] = color.getGreen();

				deter = a[0][0] * a[1][1] - a[0][1] * a[1][0];

				t = a[0][0] + a[1][1];

				r = (float) (deter - sensitivity * t * t);

				if (r > max) {

					max = r;
				}
			}
		}

		for (int i = 0; i < height; i++) {

			for (int j = 0; j < width; j++) {

				Color color = new Color(target.image.getRGB(i, j));

				a[0][0] = color.getRed();
				a[0][1] = color.getBlue();
				a[1][0] = color.getBlue();
				a[1][1] = color.getGreen();

				deter = a[0][0] * a[1][1] - a[0][1] * a[1][0];

				t = a[0][0] + a[1][1];

				r = (float) (deter - sensitivity * t * t);

				if (r <= 0) {

					r = 0;

				} else if (r > 0) {

					r = (r / max) * 255;
				}
				target.image.setRGB(i, j, (int) r << 16 | (int) r << 8 | (int) r);
			}
		}
		target.repaint();
	}



	void derivatives()
	{
		int[] gaussianX  = { -1, -2 , 0, 2 , 1,
				-4, -10, 0, 10, 4,
				-7, -17, 0, 17, 7,
				-4, -10, 0, 10, 4,
				-1, -2 , 0, 2 , 1 };

		int[] gaussianY = { -1, -4 , -7 , -4 , -1,
				-2, -10, -17, -10, -2,
				0,  0 ,  0 ,  0 ,  0,
				2,  10,  17,  10,  2,
				1,  4 ,  7 ,  4 ,  1 };

		int[] gaussianXY = { 1, 4 , 6 , 4 , 1,
				4, 16, 24, 16, 1,
				6, 24, 36, 24, 6,
				4, 16, 24, 16, 4,
				1, 4 , 6 , 4 , 1 };

		int position= 2;
		int GRAY;
		int[] temporaryX = new int[25];
		int[] temporaryY = new int[25];
		int[] temporaryXY = new int[25];
		int temp_dx = 0, temp_dy = 0, temp_dxy = 0;
		double dx, dy, dxy;
		double dx2, dy2;

		for ( int q = position; q < height - position; q++ ){
			for ( int p = position; p < width - position; p++ ){
				int i = 0;
				temp_dx = 0;
				temp_dy = 0;
				temp_dxy = 0;
				for ( int v = -position; v <= position; v++ ){
					for ( int u = -position; u <= position; u++ ){
						Color clr = new Color(source.image.getRGB(q+v,p+u));
						GRAY = (clr.getRed() + clr.getGreen() + clr.getBlue())/3;
						temporaryX[i] = GRAY*gaussianX[i];
						temporaryY[i] = GRAY*gaussianY[i];
						temporaryXY[i] = GRAY*gaussianXY[i];
						i++;
					}
				}
				for ( int t = 0; t < gaussianX.length; t++ ){
					temp_dx += temporaryX[t];
				}
				for ( int t = 0; t < gaussianY.length; t++ ){
					temp_dy += temporaryY[t];
				}
				for ( int t = 0; t < gaussianXY.length; t++ ){
					temp_dxy += temporaryXY[t];
				}

				dx = temp_dx/58;
				dx2 = dx*dx*0.05;
				if ( dx2 > 255 )
					dx2 = 255;

				dy = temp_dy/58;
				dy2 = dy*dy*0.05;

				if ( dy2 > 255 )
					dy2 = 255;

				dxy = dx*dy*0.09;
				if ( dxy < 0 )
					dxy = 0;

				if ( dxy > 255 )
					dxy = 255;

				target.image.setRGB(q, p, new Color((int)dx2, (int)dy2, (int)dxy).getRGB());
			}
		}
		target.repaint();
	}
	// method to implement the color channels separately
	public int GetRGB(int omega, int red, int green, int blue) /////////////////////////////////////////
	{

		int PIXEL_FINAL = 0;
		PIXEL_FINAL += omega;
		PIXEL_FINAL = PIXEL_FINAL << 8;
		PIXEL_FINAL += red; PIXEL_FINAL = PIXEL_FINAL << 8;
		PIXEL_FINAL += green; PIXEL_FINAL = PIXEL_FINAL << 8;
		PIXEL_FINAL += blue;

		return PIXEL_FINAL;
	}

	public static void main(String[] args) {
		new CornerDetection(args.length==1 ? args[0] : "signal_hill.png");
	}
}
