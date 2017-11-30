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
 * GOFISH:
 * 	param1: Player number
 * 	param2: card number
 * 
 * @author amitshankar
 *
 */
public enum NetworkCommand implements Serializable {
    GOFISHREQ, GOFISHRES, INDEX, ONECARD, FIVECARDS, GAMEOVER, WELCOME
}
