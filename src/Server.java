import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class Server {

    private static ServerSocket serverSocket;
    // store vector containing all output streams
    private static List<ObjectOutputStream> players = new Vector<>();
    private static Vector<Socket> playerSockets = new Vector<>();

    // player states
    private static boolean playersDone[] = { false, false, false, false };
    private static HashMap<String, Vector<Rank>> playerBooks = new HashMap<>();

    private static int playerCtr = 0;

    private static Vector<Card> deck;

    private static final int NUM_PLAYERS = 4;

    public static void main(String[] args) throws IOException {
	serverSocket = new ServerSocket(11495, 5);
	System.out.println("Server Socket created for " +  (InetAddress.getLocalHost().getHostName()) + ".");
	setDeck();
	initializeVariables();
	// Setup the server to accept many clients
	while (true) {
	    System.out.println("Waiting to connect with player " + playerCtr + "...");
	    Socket socket = serverSocket.accept();
	    playerSockets.add(socket);

	    System.out.println(socket.getInetAddress().getHostName() + " has connected.");

	    ObjectOutputStream outputToClient = new ObjectOutputStream(socket.getOutputStream());
	    ObjectInputStream inputFromClient = new ObjectInputStream(socket.getInputStream());

	    players.add(outputToClient); // add the
	    // stream
	    // outputToClient.writeObject(new Command(NetworkCommand.WELCOME));

	    // Start the loop that reads any Client's writeObject in the
	    // background in a
	    // different Thread so this program can also wait for new Client
	    // connections.
	    // This thread allows the Server to wait for each client's writing
	    // of a String message.
	    // TODO 2: Start a new ClientHandler in a new Thread
	    ClientHandler clientHandler = new ClientHandler(inputFromClient, players.get(playerCtr), socket);
	    Thread thread = new Thread(clientHandler);
	    thread.start();
	    playerCtr++;

	    if (playerCtr == NUM_PLAYERS) {
		System.out.println("All four players have been connected.");
		for (int i = 0; i < NUM_PLAYERS; i++) {
		    System.out.println("i: " + i);
		    Vector<Card> hand = buildHand();
		    System.out.println(hand);
		    Command cmd = new Command(NetworkCommand.INDEX, hand);
		    players.get(i).writeObject(cmd);
		}
	    }
	}
    }

    private static void initializeVariables() {
	playerBooks.put("harbor.cs.arizona.edu", new Vector<Rank>());
	playerBooks.put("harpoon.cs.arizona.edu", new Vector<Rank>());
	playerBooks.put("harvard.cs.arizona.edu", new Vector<Rank>());
	playerBooks.put("harlem.cs.arizona.edu", new Vector<Rank>());
    }

    private static class ClientHandler implements Runnable {

	private ObjectInputStream input;
	private ObjectOutputStream output;
	private Socket socket;

	public ClientHandler(ObjectInputStream input, ObjectOutputStream output, Socket socket) {
	    this.output = output;
	    this.input = input;
	    this.socket = socket;
	}

	@Override
	public void run() {
	    // TODO 3: Complete this run method with a while(true) loop
	    // to read any new messages from the server. When a new read
	    // happens, write the new message to all Clients

	    while (true) {
		Command cmd = null;
		try {
		    if (input == null)
			System.out.println("THIS INPUT STREAM IS CLOSED");
		    cmd = (Command) input.readObject();
		    NetworkCommand commandType = cmd.getCommand();
		    Object param1 = cmd.getParam1();
		    Object param2 = cmd.getParam2();
		    Object param3 = cmd.getParam3();

		    if (commandType == NetworkCommand.FIVECARDS) {
			if (deck.size() > 0) {
			    Vector<Card> hand = buildHand();
			    Command returnCommand = new Command(NetworkCommand.FIVECARDS, hand.size(), hand, null);
			    System.out.println("There are " + deck.size() + " remaining in the deck.");
			    output.reset();
			    output.writeObject(returnCommand);
			} else {
			    Command noCards = new Command(NetworkCommand.OUTOFCARDS);
			    System.out.println("There are no more cards.");
			    output.reset();
			    output.writeObject(noCards);
			}
		    } else if (commandType == NetworkCommand.ONECARD) {
			if (deck.size() > 0) {
			    Collections.shuffle(deck);
			    Card returnCard = deck.remove(0);
			    System.out.println("There are " + deck.size() + " remaining in the deck.");
			    Command returnCommand = new Command(NetworkCommand.ONECARD, returnCard, null, null);

			    output.reset();
			    output.writeObject(returnCommand);
			} else {
			    System.out.println("There are no more cards.");
			    Command noCards = new Command(NetworkCommand.OUTOFCARDS);
			    output.reset();
			    output.writeObject(noCards);
			}
		    } else if (commandType == NetworkCommand.GAMEOVER) {
			markPlayerAsDone(socket.getInetAddress().getHostName());
			boolean gameOver = isGameOver();
			if (gameOver) {
			    signalTheGameIsOver();
			}
		    } else if (commandType == NetworkCommand.BOOK) {
			String fromPlayer = (String) param1;
			Rank bookRank = (Rank) param2;
			Vector<Rank> books = playerBooks.get(fromPlayer);
			if(!books.contains(bookRank)) {
			    System.out.println(fromPlayer + " got a book of " + bookRank + "!");
			    books.add(bookRank);
			}
			playerBooks.put(fromPlayer, books);
		    } 

		} catch (EOFException e) {
		    e.printStackTrace();
		    System.out.println("A client has left the game");
		    break; // break from the thread when client is closed
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    break; // break from the thread when client is closed
		} catch (ClassNotFoundException cnfe) {
		}
	    }
	}

	/*
	 * // TODO 4: This method is used to write message to all output streams
	 * private void writeVectorToClients(Vector<PaintObject> objects) {
	 * for(ObjectOutputStream stream: outputStreams) { try { stream.reset();
	 * // reset before sending the new list of objects
	 * stream.writeObject(objects); // write vector to all output streams }
	 * catch (IOException e) { } catch (ConcurrentModificationException e) {
	 * } } }
	 */

    }

    public static void markPlayerAsDone(String name) {
	if ("harbor.cs.arizona.edu".equals(name)) {
	    playersDone[0] = true;
	} else if ("harvard.cs.arizona.edu".equals(name)) {
	    playersDone[1] = true;
	} else if ("harpoon.cs.arizona.edu".equals(name)) {
	    playersDone[2] = true;
	} else {
	    playersDone[3] = true;
	}
    }

    public static boolean isGameOver() {
	for (boolean b: playersDone) {
	    if (b == false)
		return false;
	}
	return true;
    }

    public static void signalTheGameIsOver() {
	for (ObjectOutputStream stream : players) {
	    try {
		stream.reset();
		Command gameOver = new Command(NetworkCommand.GAMEOVER, getWinner());
		stream.writeObject(gameOver);
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
	System.exit(0);
    }

    private static String getWinner() {
	int maxBooks = -1;
	String winnerName = "";
	String playerNames[] = { "harbor.cs.arizona.edu", "harpoon.cs.arizona.edu", "harlem.cs.arizona.edu",
	"harvard.cs.arizona.edu" };

	// TODO: initialize the hashmap
	for (String name : playerNames) {
	    Vector<Rank> books = playerBooks.get(name); // books for that player
	    if (books.size() > maxBooks) {
		winnerName = name;
		maxBooks = books.size();
	    } else if (books.size() == maxBooks) {
		Vector<Rank> winnersBooks = playerBooks.get(winnerName);
		Rank winnerRank = Collections.max(winnersBooks);
		Rank currentRank = Collections.max(books);

		if (currentRank.getValue() > winnerRank.getValue()) {
		    winnerName = name;
		    maxBooks = books.size();
		}
	    }
	}
	return winnerName;
    }

    /**
     * 
     * @return a hand with 5 random cards from the deck until the deck is empty
     */
    private static Vector<Card> buildHand() {
	Vector<Card> hand = new Vector<>();

	for (int i = 0; i < 5 && deck.size() > 0; i++) {
	    hand.add(deck.remove(0));
	    Collections.shuffle(deck);
	}

	return hand;
    }

    private static void setDeck() {

	deck = new Vector<>();

	/*Card C2 = new Card(Rank.DEUCE, Suit.CLUBS);
	Card C3 = new Card(Rank.THREE, Suit.CLUBS);
	Card C4 = new Card(Rank.FOUR, Suit.CLUBS);
	Card C5 = new Card(Rank.FIVE, Suit.CLUBS);
	Card C6 = new Card(Rank.SIX, Suit.CLUBS);
	Card C7 = new Card(Rank.SEVEN, Suit.CLUBS);
	Card C8 = new Card(Rank.EIGHT, Suit.CLUBS);*/
	Card C9 = new Card(Rank.NINE, Suit.CLUBS);
	Card C10 = new Card(Rank.TEN, Suit.CLUBS);
	Card CJ = new Card(Rank.JACK, Suit.CLUBS);
	Card CQ = new Card(Rank.QUEEN, Suit.CLUBS);
	Card CK = new Card(Rank.KING, Suit.CLUBS);
	Card CA = new Card(Rank.ACE, Suit.CLUBS);

	/*	deck.add(C2);
	deck.add(C3);
	deck.add(C4);
	deck.add(C5);
	deck.add(C6);
	deck.add(C7);
	deck.add(C8);*/
	deck.add(C9);
	deck.add(C10);
	deck.add(CJ);
	deck.add(CQ);
	deck.add(CK);
	deck.add(CA);

	/*	Card D2 = new Card(Rank.DEUCE, Suit.DIAMONDS);
	Card D3 = new Card(Rank.THREE, Suit.DIAMONDS);
	Card D4 = new Card(Rank.FOUR, Suit.DIAMONDS);
	Card D5 = new Card(Rank.FIVE, Suit.DIAMONDS);
	Card D6 = new Card(Rank.SIX, Suit.DIAMONDS);
	Card D7 = new Card(Rank.SEVEN, Suit.DIAMONDS);
	Card D8 = new Card(Rank.EIGHT, Suit.DIAMONDS);*/
	Card D9 = new Card(Rank.NINE, Suit.DIAMONDS);
	Card D10 = new Card(Rank.TEN, Suit.DIAMONDS);
	Card DJ = new Card(Rank.JACK, Suit.DIAMONDS);
	Card DQ = new Card(Rank.QUEEN, Suit.DIAMONDS);
	Card DK = new Card(Rank.KING, Suit.DIAMONDS);
	Card DA = new Card(Rank.ACE, Suit.DIAMONDS);

	/*	deck.add(D2);
	deck.add(D3);
	deck.add(D4);
	deck.add(D5);
	deck.add(D6);
	deck.add(D7);
	deck.add(D8);*/
	deck.add(D9);
	deck.add(D10);
	deck.add(DJ);
	deck.add(DQ);
	deck.add(DK);
	deck.add(DA);

	/*	Card H2 = new Card(Rank.DEUCE, Suit.HEARTS);
	Card H3 = new Card(Rank.THREE, Suit.HEARTS);
	Card H4 = new Card(Rank.FOUR, Suit.HEARTS);
	Card H5 = new Card(Rank.FIVE, Suit.HEARTS);
	Card H6 = new Card(Rank.SIX, Suit.HEARTS);
	Card H7 = new Card(Rank.SEVEN, Suit.HEARTS);
	Card H8 = new Card(Rank.EIGHT, Suit.HEARTS);*/
	Card H9 = new Card(Rank.NINE, Suit.HEARTS);
	Card H10 = new Card(Rank.TEN, Suit.HEARTS);
	Card HJ = new Card(Rank.JACK, Suit.HEARTS);
	Card HQ = new Card(Rank.QUEEN, Suit.HEARTS);
	Card HK = new Card(Rank.KING, Suit.HEARTS);
	Card HA = new Card(Rank.ACE, Suit.HEARTS);

	/*	deck.add(H2);
	deck.add(H3);
	deck.add(H4);
	deck.add(H5);
	deck.add(H6);
	deck.add(H7);
	deck.add(H8);*/
	deck.add(H9);
	deck.add(H10);
	deck.add(HJ);
	deck.add(HQ);
	deck.add(HK);
	deck.add(HA);

	/*Card S2 = new Card(Rank.DEUCE, Suit.SPADES);
	Card S3 = new Card(Rank.THREE, Suit.SPADES);
	Card S4 = new Card(Rank.FOUR, Suit.SPADES);
	Card S5 = new Card(Rank.FIVE, Suit.SPADES);
	Card S6 = new Card(Rank.SIX, Suit.SPADES);
	Card S7 = new Card(Rank.SEVEN, Suit.SPADES);
	Card S8 = new Card(Rank.EIGHT, Suit.SPADES);*/
	Card S9 = new Card(Rank.NINE, Suit.SPADES);
	Card S10 = new Card(Rank.TEN, Suit.SPADES);
	Card SJ = new Card(Rank.JACK, Suit.SPADES);
	Card SQ = new Card(Rank.QUEEN, Suit.SPADES);
	Card SK = new Card(Rank.KING, Suit.SPADES);
	Card SA = new Card(Rank.ACE, Suit.SPADES);

	/*	deck.add(S2);
	deck.add(S3);
	deck.add(S4);
	deck.add(S5);
	deck.add(S6);
	deck.add(S7);
	deck.add(S8);*/
	deck.add(S9);
	deck.add(S10);
	deck.add(SJ);
	deck.add(SQ);
	deck.add(SK);
	deck.add(SA);

	Collections.shuffle(deck);
    }
}
