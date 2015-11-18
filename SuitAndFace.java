import java.awt.Color;

public class SuitAndFace {
	private Suit suit;
	private Face face;

	public SuitAndFace(Suit s, Face f){
		suit = s;
		face = f;
	}

	public String toString() {
		return face.name() + " of " + suit.name();
	}

	public Suit getSuit(){ return suit; }

	public Face getFace(){ return face; }

	private Color getColor(){
		if (suit.equals(Suit.Hearts) || suit.equals(Suit.Diamonds))
			return Color.RED;
		else
			return Color.BLACK;
	}

	public boolean canStackOn(SuitAndFace card) {
		if (face.ordinal()+1 == card.getFace().ordinal() &&
			this.getColor() != card.getColor()) 
			return true;
		else 
			return false;
	}

	// Required if using this class as a key in a HashMap
	@Override
	public boolean equals(Object o) {
		// Handle case where object is a different type
		if (!(o instanceof SuitAndFace)) { 
			return false;
		
		// If same type, compare suit and face
		} else {
			SuitAndFace card = (SuitAndFace)o;
			if (suit == card.getSuit() && face == card.getFace()) {
				return true;

			} else {
				return false;
			}
		}
	}

	// Required method if using this class as a key in a HashMap
	@Override
	public int hashCode(){

		final int prime = 37;
		
		int suitint = suit.ordinal();
		int faceint = face.ordinal();

		int result = 1;

		result = prime * result + suitint;
		result = prime * result + faceint;

		return result;
	}

}