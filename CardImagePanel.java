import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.io.*;
import javax.swing.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.*;
import javax.swing.border.LineBorder;

class CardImagePanel extends JPanel {
	private BufferedImage img;
	private SuitAndFace suitAndFace;

	// Constructor	
	public CardImagePanel(){
	}

	public CardImagePanel(BufferedImage im){
		super();
		this.setImage(im);
       	setOpaque(false);
	}

	public BufferedImage getImage(){ return this.img; }

	public void setSuitAndFace(SuitAndFace sf) {
		suitAndFace = sf;
	}

	public SuitAndFace getSuitAndFace() {
		return suitAndFace;
	}

	public Suit getSuit(){
		return suitAndFace.getSuit();
	}

	public Face getFace(){
		return suitAndFace.getFace();
	}

	public void setImage(BufferedImage im){
		this.img = im;
		revalidate();
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int w = (int)getPreferredSize().getWidth();
		int h = (int)getPreferredSize().getHeight();
		if (img != null) g.drawImage(img,0,0,w,h,null);
	}

	// Defined inside class JPanel
	// Called when layout manager asks how big it would like to be
	@Override
	public Dimension getPreferredSize(){
		// Choosing a value that is small but still visible. Could be anything.
		if (img==null) {
			return new Dimension(20,20);

		} else {
			// return new Dimension(img.getWidth()/4, img.getHeight()/4);
			// return new Dimension(125,182);
			return new Dimension(100,147);
		}
	}

	// protected MouseAdapter mouseAdapter = new MouseAdapter(){
	// 	public void mousePressed( MouseEvent e){
	// 		mousePressPosition = e.getPoint();
	// 		revalidate();
	// 		repaint();
	// 	}
	// 	public void mouseDragged(MouseEvent e){
	// 		Point p = e.getPoint();
	// 		int xdiff = (int)(p.getX() - mousePressPosition.getX());
	// 		int ydiff = (int)(p.getY() - mousePressPosition.getY());
	// 		setLocation( (int)(getLocation().getX() + xdiff), (int)(getLocation().getY() + ydiff) );
	// 		revalidate();
	// 		repaint();
	// 	}
	// };

	

	private static void build() {
		JFrame f = new JFrame();
		JPanel p = new JPanel();
		CardImagePanel cPanel = new CardImagePanel();

		p.add(cPanel);
		cPanel.setBorder(BorderFactory.createLineBorder(Color.black));

		f.setContentPane(p);
		f.setSize(600,900);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}


	public static void main(String[] args) {
		build();
	}

}