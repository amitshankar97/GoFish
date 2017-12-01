import java.io.Serializable;

/**
 * INDEX:
 * 	param1: int index
 * 	param2: Vector<Card> hand
 * 	param3: Vector<Socket> player sockets
 * 
 * GAMEOVER:
 * 	param1: int index of winner
 * 
 * ONECARD:
 * 	param1: Card newCard
 * 
 * FIVECARDS:
 * 	param1: List<Card> hand
 * 
 * GOFISHREQ:
 * 	param1: Player name (from)
 * 	param2: Player name (to)
 * 	param3: Rank
 * 
 * GOFISHRES:
 * 	param1: Player name (from)
 * 	param2: Player name (to)
 * 	param3: Vector<Card> or null(if no card was found)
 * 
 * @author amitshankar
 *
 */
public enum NetworkCommand implements Serializable {
    GOFISHREQ, GOFISHRES, INDEX, ONECARD, FIVECARDS, GAMEOVER, WELCOME, ENDTURN
}
