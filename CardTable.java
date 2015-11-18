import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.*;
import java.lang.InterruptedException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.util.HashMap;

enum Face {Ace, Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten, Jack, Queen, King};
enum Suit {Diamonds, Hearts, Spades, Clubs};

class CardTable extends JPanel {
	private List<CardImagePanel> cards;
	private List<List<CardImagePanel>> cascades;
	private List<CardImagePanel> freeCells;
	private List<List<CardImagePanel>> foundations;
	private SuitAndFace clickedCard;
	private int srccolumn, dstcolumn, startingZOrder;
	private Point mousePressPosition, cardStartingPoint;
	private final int startX        = 50;
	private final int startY        = 50;
	private final int cardWidth     = 100;
	private final int columnWidth   = 115;
	private final int cardHeight    = 147;
	private final int rowHeight     = 40;
	private final int rowHeightFull = cardHeight;
	private static HashMap<SuitAndFace, CardImagePanel> hmap = new HashMap<SuitAndFace, CardImagePanel>();
	private static final String[] numNames = {
	    "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
	};
		
	public CardTable() {
		cards = new ArrayList<CardImagePanel>();
		cascades = new ArrayList<List<CardImagePanel>>(8);
		freeCells = new ArrayList<CardImagePanel>(4);
		foundations = new ArrayList<List<CardImagePanel>>();

		for (int i=0; i<4; i++){
			freeCells.add(i, null);
			// System.out.println(freeCells.get(i));
			// System.out.println(freeCells.size());
			// System.out.println(Arrays.asList(freeCells.toArray()));
		}
		this.setLayout(null);
		this.addMouseListener(mouseAdapter);
		this.addMouseMotionListener(mouseAdapter);
	}

	// public ArrayList<CardImagePanel> getCards(){
	// 	return cards;
	// }

	private int getColumn(Point p){
		int xPos = (int)p.getX();
		int column = 0;

		if (xPos > startX && xPos < startX + (10*columnWidth) ){
			// Which column was clicked?
			column = (xPos - startX) / columnWidth;

			// Did they click in the gutter between columns?
			if ((xPos - startX) % columnWidth > cardWidth){
				System.out.println("Clicked in a gutter");
				return -1;
			} else {
				return column;
			}
		} else {
			return -1;
		}
	}

	private int getRow(Point p){
		int yPos = (int)p.getY();
		int column = getColumn(p);
		int row;
		if (column < 0) return -1; 

		// FreeCell or Foundation column
		if (column == 8 || column == 9){
			row = (yPos - startY) / (cardHeight + 15);

			// Did they click in the empty space between rows?
			if (yPos < startY || yPos > (startY + (4*(cardHeight+15))))
				return -1;
			else if ((yPos - startY) % cardHeight > cardHeight)
				return -1;
			else 
				return row;

		// Cascade column
		} else {
			List<CardImagePanel> cascade = cascades.get(column);

			// How many rows does this cascade have?
			int rowsInCascade = cascade.size();

			// Did they click above or below the cascade?
			if (yPos < startY || yPos > startY + (rowsInCascade-1)*rowHeight + rowHeightFull) {
				System.out.println("Clicked above or below the cascade");
				return -1;
			}

			// Which row was clicked?
			if (yPos > startY + (rowsInCascade-1)*rowHeight){
				row = rowsInCascade - 1; // Bottom card
				System.out.print("Bottom card (row " + row + "), ");
			} else {
				row = (yPos - startY) / rowHeight;
				System.out.print("Row " + row + ", ");
			}

			return row;
		}
	}

	protected MouseAdapter mouseAdapter = new MouseAdapter(){
		public void mousePressed( MouseEvent e){
			mousePressPosition = e.getPoint();
			int column = getColumn( e.getPoint() );
			int row = getRow( e.getPoint() );
			srccolumn = column;
			clickedCard = null;

			if (column < 0 || row < 0) return;
			
			System.out.print("Column " + column + ", ");
			
			if (column < 8) {
				List<CardImagePanel> cascade = cascades.get(column);
				clickedCard = cascade.get(row).getSuitAndFace();
				cardStartingPoint = cascade.get(row).getLocation();
				startingZOrder = getComponentZOrder(hmap.get(clickedCard));
				System.out.println(clickedCard + " was clicked");

				// Check for valid tableau if row != cascade.size()-1

			} else if (column == 8) {
				// Is there a free cell? If so, put card in row (if it's empty) OR first empty row
				if (freeCells.get(row) != null){
					clickedCard = freeCells.get(row).getSuitAndFace();
					cardStartingPoint = freeCells.get(row).getLocation();
					startingZOrder = getComponentZOrder(hmap.get(clickedCard));
				}
				System.out.println(clickedCard + " was clicked");

			} else if (column == 9) {
				// Do nothing
			}
		}

		public void mouseReleased(MouseEvent e){
			int column = getColumn( e.getPoint() );
			int dstcolumn = column;
			int dstrow = getRow( e.getPoint() );

			if (column < 0) { 
				System.out.println("Released outside a column");
				returnToStartingPoint(hmap.get(clickedCard));
				return;
			} 

			if (srccolumn >= 0 && dstcolumn >=0 && clickedCard != null){
				// 5 Cases
				
				// Cascade to Cascade
				if (srccolumn < 8 && dstcolumn < 8) {
					if (canMoveBetweenCascades(clickedCard, srccolumn, dstcolumn)){
						new Thread() {
							public void run() {
								doMoveBetweenCascades(clickedCard, srccolumn, dstcolumn);
							}
						}.start();
					} 
					else {
						new Thread() {
							public void run() {
								returnToStartingPoint(hmap.get(clickedCard));
							}
						}.start();
					}

				// Cascade to Free Cell
				} else if (srccolumn < 8 && dstcolumn == 8 && dstrow >= 0){
					if (canMoveFromCascadesToFreecells(clickedCard, srccolumn, dstcolumn)){
						new Thread() {
							public void run() {
								doMoveFromCascadesToFreecells(clickedCard, srccolumn, dstcolumn, dstrow);
							}
						}.start();
					}
					else {
						new Thread() {
							public void run() {
								returnToStartingPoint(hmap.get(clickedCard));
							}
						}.start();
					}

				// Cascade to Foundation
				} else if (srccolumn < 8 && dstcolumn == 9){
					if (canMoveFromCascadesToFoundations(clickedCard, srccolumn, dstcolumn)){
						new Thread() {
							public void run() {
								doMoveFromCascadesToFoundations(clickedCard, srccolumn, dstcolumn);
							}
						}.start();
					}
					else {
						new Thread() {
							public void run() {
								returnToStartingPoint(hmap.get(clickedCard));
							}
						}.start();
					}

				// Free Cell to Cascade
				} else if (srccolumn == 8 && dstcolumn < 8){
					if (canMoveFromFreecellsToCascades(clickedCard, srccolumn, dstcolumn)){
						new Thread() {
							public void run() {
								doMoveFromFreecellsToCascades(clickedCard, srccolumn, dstcolumn);
							}
						}.start();
					}
					else {
						new Thread() {
							public void run() {
								returnToStartingPoint(hmap.get(clickedCard));
							}
						}.start();
					}

				// Free Cell to Foundation
				} else if (srccolumn == 8 && dstcolumn == 9){
					if (canMoveFromFreecellsToFoundations(clickedCard, srccolumn, dstcolumn)){
						new Thread() {
							public void run() {
								doMoveFromFreecellsToFoundations(clickedCard, srccolumn, dstcolumn);
							}
						}.start();
					}
					else {
						new Thread() {
							public void run() {
								returnToStartingPoint(hmap.get(clickedCard));
							}
						}.start();
					}
				}
			}

			System.out.println("Released in column " + column);
		}

		public void mouseDragged(MouseEvent e){
			if (clickedCard != null){
				new Thread() {
					public void run() {

						Point p = e.getPoint();
						int xdiff = (int)(p.getX() - mousePressPosition.getX());
						int ydiff = (int)(p.getY() - mousePressPosition.getY());
						CardImagePanel card = hmap.get(clickedCard);

						if (isBottomOfCascade(clickedCard) || getColumn(cardStartingPoint) == 8){
							card.setLocation( (int)p.getX() - cardWidth/2, 
											  (int)p.getY() - cardHeight/2 );
							setComponentZOrder(card, 0);
							SwingUtilities.invokeLater( new Runnable() {
								public void run() {
									revalidate();
									repaint();
								}
							});
						} else if (isTopOfTableau(clickedCard)){
							// System.out.println("Picking up tableau");
							// need to handle this scenario and animate the whole tableau
							List<CardImagePanel> cascade = cascades.get(getColumn(mousePressPosition));
							int index = cascade.indexOf(card);
							for (int i=index; i<cascade.size(); i++){
								card = cascade.get(i);
								card.setLocation( (int)p.getX() - cardWidth/2, 
											  	  (int)p.getY() - cardHeight/2 + (rowHeight*(i-index+1)));
							
								setComponentZOrder(card, 0);
								SwingUtilities.invokeLater( new Runnable() {
									public void run() {
										revalidate();
										repaint();
									}
								});
							}
						} 
					}
				}.start();
			}
		}
	};

	private void returnToStartingPoint(CardImagePanel card){

		if (isTopOfTableau(card.getSuitAndFace())) {
			// Return all the cards in the tableau
			List<CardImagePanel> cascade = cascades.get(getColumn(mousePressPosition));
			int index = cascade.indexOf(card);
			for (int i=index; i<cascade.size(); i++){
				card = cascade.get(i);
				card.setLocation( (int)cardStartingPoint.getX(), 
							  	  (int)cardStartingPoint.getY() + (rowHeight*(i-index)));
				setComponentZOrder(card, startingZOrder-(i-index));
			}
		}
		else {
			card.setLocation(cardStartingPoint);
			setComponentZOrder(card, startingZOrder);
		}
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				revalidate();
				repaint();
			}
		});
	}

	private boolean canMoveBetweenCascades(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		if (isTopOfTableau(clickedCard)){
			
			List<CardImagePanel> cascade = cascades.get(srccolumn);
			int tableauSize = cascade.size() - cascade.indexOf(hmap.get(clickedCard));

			// How many empty cascades are there?
			int emptyCascades = 0;
			for (int i=0; i < cascades.size(); i++) {
				if (cascades.get(i).size() == 0)
					emptyCascades++;
			}

			// Can't move a tableau that is bigger than 1 + number of free cells
			if (tableauSize > emptyCascades + getEmptyFreeCells() + 1)
				return false;

			// Can we stack on bottom card in dstcolumn?
			List<CardImagePanel> destCascade = cascades.get(dstcolumn);
			if (destCascade.size() == 0) {
				return true;
			}
			else {
				SuitAndFace bottomCard = destCascade.get(destCascade.size() - 1).getSuitAndFace();
				if (clickedCard.canStackOn(bottomCard)){
					return true;
				}
			}
		}
		return false;
	}

	private void doMoveBetweenCascades(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		List<CardImagePanel> cascade = cascades.get(srccolumn);
		int index = cascade.indexOf(hmap.get(clickedCard));
		List<CardImagePanel> tableau = new ArrayList<CardImagePanel>();

		for (int i=index; i < cascade.size(); i++)
			 tableau.add(cascade.get(i));

		cascade.removeAll(tableau);
		int rowsAtDestination = cascades.get(dstcolumn).size();
		cascades.get(dstcolumn).addAll(tableau);

		for (int i=0; i < tableau.size(); i++){
			CardImagePanel card = tableau.get(i);
			int x = startX + (columnWidth * dstcolumn);
			int y = startY + (rowHeight * (rowsAtDestination+i));
			
			remove(card);
		card.setSize(card.getPreferredSize());
			card.setLocation(x, y);

			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					add(card);
					setComponentZOrder(card, 0);
					revalidate();
					repaint();
				}
			});
			System.out.println("Doing the move between cascades!");
		}
	}

	private boolean canMoveFromCascadesToFreecells(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		if (isBottomOfCascade(clickedCard) && dstcolumn == 8 && getEmptyFreeCells() > 0)
			return true;
		else 
			return false;
	}

	private void doMoveFromCascadesToFreecells(SuitAndFace clickedCard, int srccolumn, int dstcolumn, int dstrow){
		List<CardImagePanel> cascade = cascades.get(srccolumn);
		CardImagePanel card = hmap.get(clickedCard);
		// Remove card from cascade
		cascade.remove(card);
		// Add card to freecell
		int row = 0;
		System.out.println("Desired row: " + dstrow);

		// Add card to free cell user released on if it's available (or else the next available free cell)
		if (dstrow >= freeCells.size()){
			freeCells.add(dstrow, card);
			row = dstrow;
		}
		else if (freeCells.toArray()[dstrow] == null) {
			freeCells.set(dstrow, card);
			row = dstrow;
		}
		else {
			for (int i=0; i<freeCells.size(); i++){
				if (freeCells.toArray()[i] == null) {
					freeCells.set(i, card);
					row = i;
					break;
				}
			}
		}

		// Update GUI
		int x = startX + (columnWidth * dstcolumn);
		int y = startY + (cardHeight * row);
		if (row > 0)
			y += (15 * row);

		remove(card);
		card.setSize(card.getPreferredSize());
		card.setLocation(x, y);

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				add(card);
				setComponentZOrder(card, 0);
				revalidate();
				repaint();
			}
		});
		System.out.println("Doing the move!");
	}
	private boolean canMoveFromCascadesToFoundations(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		// Card must be at bottom of cascade
		if (! isBottomOfCascade(clickedCard))
			return false;
		
		// Aces can always move to foundation
		if (clickedCard.getFace() == Face.Ace)
			return true;
		else {
			Suit s = clickedCard.getSuit();
			int row = getFoundationRowForSuit(s);		
			// If there's a foundation for the current suit
			if (row != -1){
				// See if clicked card can be stacked on the foundation card
				List<CardImagePanel> foundation = foundations.get(row);
				CardImagePanel topFoundationCard = foundation.get(foundation.size()-1);
				
				if (topFoundationCard.getFace().ordinal() == clickedCard.getFace().ordinal() - 1)
					return true;
				else
					return false;
			}
			else {
				return false;
			}	
		}
	}
	private void doMoveFromCascadesToFoundations(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		List<CardImagePanel> cascade = cascades.get(srccolumn);
		CardImagePanel card = hmap.get(clickedCard);
		cascade.remove(card);
		addCardToFoundation(card);
	}
	private boolean canMoveFromFreecellsToCascades(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		List<CardImagePanel> destCascade = cascades.get(dstcolumn);

		if (destCascade.size() == 0) {
			return true;
		}
		else {
			SuitAndFace bottomCard = destCascade.get(destCascade.size() - 1).getSuitAndFace();
			if (clickedCard.canStackOn(bottomCard))
				return true;
			else
				return false;
		}
	}
	private void doMoveFromFreecellsToCascades(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		CardImagePanel card = hmap.get(clickedCard);

		// Remove card from Free Cell
		freeCells.set(freeCells.indexOf(card), null);
		// Add card to the cascade
		cascades.get(dstcolumn).add(card);

		int rowsAtDestination = cascades.get(dstcolumn).size();

		System.out.println("doMoveFromFreecellsToCascades has been called!");
		int x = startX + (columnWidth * dstcolumn);
		int y = startY + (rowHeight * (rowsAtDestination - 1));
		
		remove(card);
		card.setSize(card.getPreferredSize());
		card.setLocation(x, y);

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				add(card);
				setComponentZOrder(card, 0);
				revalidate();
				repaint();
			}
		});
		System.out.println("Doing the move!");


	}
	private boolean canMoveFromFreecellsToFoundations(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		// Ace
		if (clickedCard.getFace() == Face.Ace)
			return true;
		else {
			// See if there's a foundation for the current suit
			Suit s = clickedCard.getSuit();
			int row = getFoundationRowForSuit(s);		
			if (row != -1){
				// See if clicked card is next in face order
				List<CardImagePanel> foundation = foundations.get(row);
				CardImagePanel topFoundationCard = foundation.get(foundation.size()-1);
				
				if (topFoundationCard.getFace().ordinal() == clickedCard.getFace().ordinal() - 1)
					return true;
				else
					return false;
			}
			else {
				return false;
			}
		}
	}

	private void doMoveFromFreecellsToFoundations(SuitAndFace clickedCard, int srccolumn, int dstcolumn){
		CardImagePanel card = hmap.get(clickedCard);

		freeCells.set(freeCells.indexOf(card), null);

		addCardToFoundation(card);
	}

	private boolean isTopOfTableau(SuitAndFace clickedCard){
		List<CardImagePanel> cascade = null;
		for (List<CardImagePanel> c : cascades)
			if (c.contains(hmap.get(clickedCard))) cascade = c;

		if (cascade == null)
			return false;

		CardImagePanel cPanel = hmap.get(clickedCard);

		int row = cascade.indexOf(cPanel);
		if (row == cascade.size()-1){
			return true;
		} else {
			boolean isTableau = true;

			while (row < cascade.size()-1){
				SuitAndFace c1 = cascade.get(row).getSuitAndFace();
				row++;
				SuitAndFace c2 = cascade.get(row).getSuitAndFace();
				if (! c2.canStackOn(c1) )
					isTableau = false;
			}
			return isTableau;
		}
	}

	private boolean isBottomOfCascade(SuitAndFace clickedCard){
		List<CardImagePanel> cascade = null;
		for (List<CardImagePanel> c : cascades)
			if (c.contains(hmap.get(clickedCard))) cascade = c;

		if (cascade == null)
			return false;

		CardImagePanel cPanel = hmap.get(clickedCard);

		return (cascade.indexOf(cPanel) == cascade.size()-1);
	}

	private int getFoundationRowForSuit(Suit suit){
		int foundationRow = -1;
		for (List<CardImagePanel> foundation : foundations){
			if ( suit == foundation.get(0).getSuit())
				foundationRow = foundations.indexOf(foundation);
		}
		return foundationRow;
	}

	private void addCardToFoundation(CardImagePanel card){
		SuitAndFace clickedCard = card.getSuitAndFace();
		// Add to existing foundation or start a new one
		Suit s = clickedCard.getSuit();
		int row = getFoundationRowForSuit(s);		
		if (row != -1){
			foundations.get(row).add(card);
		}
		else {
			List<CardImagePanel> foundation = new ArrayList<CardImagePanel>();
			foundation.add(card);
			foundations.add(foundation);
			row = foundations.size()-1;
		}

		// Update GUI
		int x = startX + (columnWidth * 9);
		int y = startY + (cardHeight * row);
		if (row > 0)
			y += (15 * row);

		remove(card);
		card.setSize(card.getPreferredSize());
		card.setLocation(x, y);

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				add(card);
				setComponentZOrder(card, 0);
				revalidate();
				repaint();
			}
		});
	}

	private int getEmptyFreeCells(){
		int count = 0;
		for (int i=0; i<freeCells.size(); i++){
			if (freeCells.get(i) == null) {
				count++;
			}
		}
		return count;
	}

	private void layoutCards(ArrayList<SuitAndFace> deck){

		int currentColumn = 0;
		int currentRow = 0;
		int xPosition = startX;
		int yPosition = startY;

		// Create CardImagePanels from the shuffled deck
		// Happens on its own thread (not the event thread)
		for (SuitAndFace sf: deck){
			CardImagePanel card = hmap.get(sf);
			card.setSuitAndFace(sf);
			card.setSize(card.getPreferredSize());
			card.setLocation(xPosition, yPosition);
			
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					add(card);
					setComponentZOrder(card, 0);
					revalidate();
					repaint();
				}
			});

			// Sleep for a moment to create deal animation
			try { Thread.sleep(25);	} 
			catch (InterruptedException e) { System.out.println(e); }

			if (currentRow == 0){
				ArrayList<CardImagePanel> tmp = new ArrayList<CardImagePanel>();
				tmp.add(card);
				cascades.add(tmp);
			} else {
				cascades.get(currentColumn).add(card);	
			}			

			currentColumn++;

			if (currentColumn < 8){
				xPosition += columnWidth;
			} else {
				// Start a new row
				currentColumn = 0;
				currentRow++;
				xPosition = startX;
				yPosition += rowHeight;
			}
		}

		for (List<CardImagePanel> cascade : cascades){	
			for (CardImagePanel card : cascade){
				System.out.println(card.getSuitAndFace().toString());
			}
			System.out.println("");
		}
	}

	private void deal(){
		// Clear cards from table
		this.removeAll();
		cascades = new ArrayList<List<CardImagePanel>>(8);
		System.out.println("Cascades size: " + cascades.size());
		System.out.println("Cascades array: " + Arrays.asList(cascades.toArray()));
		foundations.clear();

		freeCells.clear();
		for (int i=0; i<4; i++){
			freeCells.add(i, null);
			// System.out.println(freeCells.get(i));
			// System.out.println(freeCells.size());
			// System.out.println(Arrays.asList(freeCells.toArray()));
		}
		System.out.println("Freecells size: " + freeCells.size());
		System.out.println("Freecells array: " + Arrays.asList(freeCells.toArray()));

		// Create a deck with all suit and face value pairs
		ArrayList<SuitAndFace> deck = new ArrayList<SuitAndFace>();
		for (Suit s: Suit.values()) {
			for (Face f: Face.values()) {
				deck.add(new SuitAndFace(s,f));
			}
		}

		// Shuffle the deck ArrayList
		Random r = new Random();
		for (int i=1; i<10000; i++) {
			int j = r.nextInt(deck.size());
			int k = r.nextInt(deck.size());
			if (j != k) {
				SuitAndFace temp = deck.get(j);
				deck.set(j,deck.get(k));
				deck.set(k,temp);
			}
		}

		new Thread() {
			public void run() {
				layoutCards(deck);
			}
		}.start();
	}

	private static void loadCardImages(){
		File cardImageDir = new File("png-cards/");
		File[] imageArray = cardImageDir.listFiles();
		BufferedImage buff = null;
		CardImagePanel cip = null;

		// Go through list of files looking for ones whose file name is in the correct format to match enums
		for (int i = 0; i < imageArray.length; i++){
			try {
				buff = ImageIO.read(imageArray[i]);
			} catch (IOException e){
				System.err.println("Caught IOException: " + e.getMessage());
			}

			cip = new CardImagePanel(buff);
			
			// create a SuitAndFace object based on file name
			String[] tokens = imageArray[i].getName().split("_");

			if (tokens.length >= 3) {
				String faceToken = tokens[0].toLowerCase();
				String suitToken = tokens[2].split("\\.")[0].toLowerCase();

				// Handle file names with numbers for face values instead of strings
				if (faceToken.length() <= 2) {
					faceToken = numNames[Integer.parseInt(faceToken)];
				}

				// Get the lowercase version of enum elements
				ArrayList<String> suitStrings = new ArrayList<String>();
				ArrayList<String> faceStrings = new ArrayList<String>();

				for (Suit s : Suit.values()) {
					suitStrings.add(s.name().toLowerCase());
				}
				for (Face f : Face.values()) {
					faceStrings.add(f.name().toLowerCase());
				}
				
				// If suit and face from tokenized file name are in the enum
				if (suitStrings.contains(suitToken) && faceStrings.contains(faceToken) ) {
					// is there a better way to initialize?
					Suit mySuit = null;
					Face myFace = null;
					
					// Get the enum value that matches the tokens	
					for (Suit s : Suit.values()) {
				        if (s.name().toLowerCase().equals(suitToken.toLowerCase())) {
				            mySuit = s;
				        }
				    }

				    for (Face f : Face.values()) {
				        if (f.name().toLowerCase().equals(faceToken.toLowerCase())) {
				            myFace = f;
				        }
				    }

				    // Create a SuitAndFace and add it to the hashmap
					SuitAndFace sf = new SuitAndFace(mySuit, myFace);
					hmap.put(sf, cip);
				}
			}
		}
	}

	public static void build(){
		JFrame f = new JFrame();
		CardTable ct = new CardTable();
		JMenuBar menuBar = new JMenuBar();
		JMenu gameMenu = new JMenu("Game");
		JMenuItem newMenuItem = new JMenuItem("New");

		ct.setBackground(new Color(54, 127, 39));
		ct.setLayout(null);

		newMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				ct.deal();
			}
		});

		// Set up menu and JFrame
		gameMenu.add(newMenuItem);
		menuBar.add(gameMenu);
		f.setJMenuBar(menuBar);
		f.add(ct);
		f.setContentPane(ct);
		f.setSize(1300,800);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void main(String[] args){
		loadCardImages();
		build();
	}
}