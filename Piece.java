/*
    ---TODO---

*/

import java.util.*;
public class Piece 
{
    public static final Piece blankSquare = new Piece();
    
    private boolean white;
    private char piece;
    private int x;
    private int y;
    private ArrayList<String> history;
    private boolean hasMoved;
    private boolean hasCastled;

    private boolean occupied;
    
    public Piece(boolean white, char piece, int x, int y, boolean hasMoved, boolean hasCastled, boolean trackHistory)
    {
        this.white = white;
        this.piece = piece;
        this.x = x;
        this.y = y;
        this.hasMoved = hasMoved;
        this.hasCastled = hasCastled;

        if (trackHistory) history = new ArrayList<String>();
        else history = null;
        occupied = true;
    }

    public Piece()
    {
        occupied = false;
        history = null;
    }
    
    // geters
    public boolean isWhite() {return white;}
    public char getPiece() {return piece;}
    public int getX() {return x;}
    public int getY() {return y;}
    public ArrayList<String> getHistory() {if (history == null) return new ArrayList<>(); else return new ArrayList<>(history);}
    public boolean isOccupied() {return occupied;}
    public boolean hasMoved() {return hasMoved;}
    public boolean hasCastled() {return hasCastled;}
    
    // seters
    public void setX(int x) {if (occupied) this.x = x;}
    public void setY(int y) {if (occupied) this.y = y;}
    public void setXY(int x, int y) {if (occupied) this.x = x; if (occupied) this.y = y;}
    public void setPiece(char piece) {if (occupied) this.piece = piece;}
    public void addHistory(String move) {if (occupied && history != null) history.add(move);}
    public void setHasMoved(boolean hasMoved) {if (occupied) this.hasMoved = hasMoved;}
    public void setHasCastled(boolean hasCastled) {if (occupied) this.hasCastled = hasCastled;}

    public int pieceToIndex()
    {
        int index = 0;
        switch (getPiece()) 
        {
            case 'P': index = 0; break;
            case 'N': index = 1; break;
            case 'B': index = 2; break;
            case 'R': index = 3; break;
            case 'Q': index = 4; break;
            case 'K': index = 5; break;
        }
        if (!isWhite()) index += 6;
        return index;
    }

    public String toIcon()
    {
        switch (getPiece())
        {
            case 'K': return "\u2654";
            case 'Q': return "\u2655";
            case 'R': return "\u2656";
            case 'B': return "\u2657";
            case 'N': return "\u2658";
            case 'P': return "\u2659";
        }
        return "";
    }
}