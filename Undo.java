public class Undo 
{
    public int[] blackKingLoc;
    public int[] whiteKingLoc;

    public boolean whiteCastled;
    public boolean whiteCanCastleLong;
    public boolean whiteCanCastleShort;

    public boolean blackCastled;
    public boolean blackCanCastleLong;
    public boolean blackCanCastleShort;

    public int[] enPassantSquare;
    public int timeSinceCaptureOrPawnPush;

    public Piece capture;
    public int[] captureLoc;
    public boolean promotion;

    public Undo(Board board, int[] end)
    {
        whiteKingLoc = board.getWhiteKingLoc();
        blackKingLoc = board.getBlackKingLoc();

        whiteCastled = board.whiteCastled;
        whiteCanCastleLong = board.whiteCanCastleLong;
        whiteCanCastleShort = board.whiteCanCastleShort;

        blackCastled = board.blackCastled;
        blackCanCastleLong = board.blackCanCastleLong;
        blackCanCastleShort = board.blackCanCastleShort;

        enPassantSquare = board.getEnPassantSquare();
        timeSinceCaptureOrPawnPush = board.getTimeSinceCaptureOrPawnPush();

        capture = board.getPiece(end);
        captureLoc = end;
        promotion = false;
    }
}
