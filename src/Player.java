import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import javafx.concurrent.Task;

public class Player {
    private static int index;
    private static int numCards;

    private static HashMap<Rank, Vector<Card>> hand;
    private static Vector<Rank> books;

    // Connection variables
    private static Socket socketFromServer;
    private static ObjectOutputStream outputToServer;
    private static ObjectInputStream inputFromServer;

    private static ServerSocket fromNeighbor;
    private static Vector<Socket> neighbors;

    private static final String Arizona = ".cs.arizona.edu";
    private static final String Address = "harpsichord" + Arizona;
    private static String computerName = "";

    private static Scanner sc = new Scanner(System.in);

    private static ObjectOutputStream toSender;
    private static ObjectInputStream fromSender;
    private static ObjectOutputStream toReceiver;
    private static ObjectInputStream fromReceiver;

    private static Object clientResponse = new Object();
    private static Object serverResponse = new Object();
    private static Object yourTurnMonitor;

    public static void main(String args[]) throws IOException {
	initializeComputerName();
	initializeBooksAndHand();
	makeServerConnection();
	makePlayerConnections();
	yourTurnMonitor = new Object();
	System.out.println("Welcome to Go Fish! Here are the possible commands:");

	if ("harbor".equals(computerName)) {
	    myTurn();
	    notMyTurn();
	} else {
	    notMyTurn();
	}

	while (true) {
	    myTurn();
	    notMyTurn();
	}
    }

    private static void initializeComputerName() {
	try {
	    computerName = (InetAddress.getLocalHost().getHostName());
	} catch (UnknownHostException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	System.out.println("Your name is " + computerName);
    }

    private static void initializeBooksAndHand() {
	// initialize the hand
	hand = new HashMap<Rank, Vector<Card>>();
	for (Rank r : Rank.values()) {
	    hand.put(r, new Vector<Card>());
	}

	// initialize the books
	books = new Vector<Rank>();
	System.out.println("Books initialized");
    }

    public synchronized static void myTurn() {
	System.out.println("1) gofish\n2) quit\n3) hand");
	while (sc.hasNext()) {
	    Command cmd = null;
	    String userString = sc.nextLine();
	    userString = userString.toLowerCase();

	    if ("quit".equals(userString)) {
		System.out.println("Thank you for playing Go Fish!");
		System.exit(0);
	    } else if ("gofish".equals(userString)) {
		int pNumber, cardNumber;

		while (true) {
		    System.out.print("Please enter the player you would like to request for a card (1-4): ");
		    pNumber = sc.nextInt();

		    if (getName(pNumber) == computerName) {
			System.out.println("Sorry! You cannot select yourself");
			continue;
		    } else if (pNumber > 4 || pNumber < 0) {
			System.out.println("Invalid player number.");
			continue;
		    } else
			break;
		}

		while (true) {
		    System.out.print("Please enter the card you would like to request: ");
		    // printOptions();
		    cardNumber = sc.nextInt();

		    if (isValid(cardNumber)) {
			askPlayer(pNumber, cardNumber);
			try {
			    clientResponse.wait();
			} catch (InterruptedException e) {
			    // TODO Auto-generated catch block
			    e.printStackTrace();
			}
			break;
		    } else {
			System.out.println("Sorry! That is not a valid choice.");
		    }
		}
	    } else if ("hand".equals(userString)) {
		System.out.println(hand);
	    } else {
		System.out.println("Sorry! Didn't recognize the command.");
		continue;
	    }

	    Command cmd4 = new Command(NetworkCommand.ENDTURN, getNextPlayer());
	    try {
		toSender.reset();
		toSender.writeObject(cmd4);
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    System.out.println("1) gofish\n2) quit\n3) hand");
	}

    }

    private static Object getNextPlayer() {
	if ("harbor.cs.arizona.edu".equals(computerName)) {
	    return "harvard.cs.arizona.edu";
	} else if ("harvard.cs.arizona.edu".equals(computerName)) {
	    return "harpoon.cs.arizona.edu";
	} else if ("harpoon.cs.arizona.edu".equals(computerName)) {
	    return "harlem.cs.arizona.edu";
	} else {
	    return "harbor.cs.arizona.edu";
	}
    }

    public static void notMyTurn() {
	synchronized (yourTurnMonitor) {
	    try {
		yourTurnMonitor.wait();
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    private static void makePlayerConnections() throws IOException {

	int connectionsMade = 0;

	if (("harlem" + Arizona).equals(computerName) || ("harvard" + Arizona).equals(computerName)) {
	    fromNeighbor = new ServerSocket(10495, 5);
	    int count = 1;
	    System.out.println(computerName + " listening on port 10495");
	    while (true) {
		Socket socket = fromNeighbor.accept();
		String connectorName = socket.getInetAddress().getHostName();
		System.out.println("connectorName: " + connectorName);

		// if we are harvard
		if (("harvard" + Arizona).equals(computerName)) {
		    // if we are connection to harbor
		    if (("harbor" + Arizona).equals(connectorName)) {
			toReceiver = new ObjectOutputStream(socket.getOutputStream());
			fromReceiver = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connected to harbor");
		    } else {
			// if we are talking harpoon
			toSender = new ObjectOutputStream(socket.getOutputStream());
			fromSender = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connected to harpoon");
		    }
		} else {
		    // if we are harlem
		    // if we are connection to harpoon
		    if (("harpoon" + Arizona).equals(connectorName)) {
			toReceiver = new ObjectOutputStream(socket.getOutputStream());
			fromReceiver = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connected to harpoon");
		    } else {
			// if we are talking harbor
			toSender = new ObjectOutputStream(socket.getOutputStream());
			fromSender = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connected to harbor");
		    }
		}

		System.out.println(connectorName + " is now connected.");
		count++;
		// Connections finished, connect the last 2 even players
		if (count == 3)
		    break;
	    } // end of while(true) loop for listeners

	} else {

	    // if you are a connector
	    Socket socket = new Socket("harvard" + Arizona, 10495);
	    if (("harbor" + Arizona).equals(computerName)) {
		toSender = new ObjectOutputStream(socket.getOutputStream());
		fromSender = new ObjectInputStream(socket.getInputStream());
	    } else {
		toReceiver = new ObjectOutputStream(socket.getOutputStream());
		fromReceiver = new ObjectInputStream(socket.getInputStream());
	    }
	    System.out.println("Connected to harvard");

	    Socket socket1 = new Socket("harlem" + Arizona, 10495);
	    if (("harpoon" + Arizona).equals(computerName)) {
		toSender = new ObjectOutputStream(socket1.getOutputStream());
		fromSender = new ObjectInputStream(socket1.getInputStream());
	    } else {
		toReceiver = new ObjectOutputStream(socket1.getOutputStream());
		fromReceiver = new ObjectInputStream(socket1.getInputStream());
	    }
	    System.out.println("Connected to harlem");
	}

	ListenForClientUpdates listener = new ListenForClientUpdates();
	// TODO 6: Start a new Thread that reads from the server
	// Note: Need setDaemon when started with a JavaFX App, or it
	// crashes.
	Thread thread = new Thread(listener);
	thread.setDaemon(true);
	thread.start();

	ListenForClientUpdates listener2 = new ListenForClientUpdates();
	// TODO 6: Start a new Thread that reads from the server
	// Note: Need setDaemon when started with a JavaFX App, or it
	// crashes.
	Thread thread1 = new Thread(listener);
	thread1.setDaemon(true);
	thread1.start();
    }

    public static Rank convertToRank(int c) {
	Rank requiredRank = null;

	for (Rank r : Rank.values()) {
	    if (c == r.getValue()) {
		requiredRank = r;
		break;
	    }
	}

	return requiredRank;
    }

    private static void askPlayer(int pNumber, int cardNumber) {

	Command cmd = new Command(NetworkCommand.GOFISHREQ, computerName, getName(pNumber), convertToRank(cardNumber));
	try {
	    toSender.reset();
	    toSender.writeObject(cmd);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private static boolean isValid(int cardNumber) {
	if (cardNumber < 2 || cardNumber > 14)
	    return false;
	return true;
    }

    private static void printOptions() {
	// TODO Auto-generated method stub

    }

    private static void makeServerConnection() {
	System.out.println("Starting to connect to " + Address + "....");
	// Our server is on our computer, but make sure to use the same port.
	try {
	    socketFromServer = new Socket(Address, 11495);
	    outputToServer = new ObjectOutputStream(socketFromServer.getOutputStream());
	    inputFromServer = new ObjectInputStream(socketFromServer.getInputStream());
	    System.out.println("Connected to server");
	    // SeverListener will have a while(true) loop
	    ListenForServerUpdates listener = new ListenForServerUpdates();
	    // TODO 6: Start a new Thread that reads from the server
	    // Note: Need setDaemon when started with a JavaFX App, or it
	    // crashes.
	    Thread thread = new Thread(listener);
	    thread.setDaemon(true);
	    thread.start();
	} catch (IOException e) {
	}
    }

    private static class ListenForClientUpdates extends Task<Object> {

	@Override
	public synchronized void run() {
	    System.out.println(computerName + " listener started!");
	    try {
		while (true) {
		    Command input = (Command) fromReceiver.readObject();

		    // parse the input
		    NetworkCommand commandType = input.getCommand();
		    Object param1 = input.getParam1();
		    Object param2 = input.getParam2();
		    Object param3 = input.getParam3();

		    if (commandType == NetworkCommand.GOFISHREQ) {
			String fromName = (String) param1;
			String toName = (String) param2;

			if (computerName.equals(fromName)) {
			    continue;
			} else if (computerName.equals(toName)) {
			    Rank r = (Rank) param3;
			    Vector<Card> returnCards = hand.get(r);
			    hand.put(r, new Vector<Card>());
			    Command cmd = new Command(NetworkCommand.GOFISHRES, param2, param1, returnCards);
			    Command cmd2 = new Command(NetworkCommand.GOFISHREQ, param1, param2, param3);
			    toSender.reset();
			    toSender.writeObject(cmd2);
			    toSender.reset();
			    toSender.writeObject(cmd);

			} else {
			    System.out.println(param1 + " requested " + param3 + " from " + param2);
			    Command cmd = new Command(NetworkCommand.GOFISHREQ, param1, param2, param3);
			    toSender.reset();
			    toSender.writeObject(cmd);
			}

		    } else if (commandType == NetworkCommand.GOFISHRES) {
			String fromName = (String) param1;
			String toName = (String) param2;

			if (computerName.equals(fromName)) {
			    continue;
			} else if (computerName.equals(toName)) {
			    Vector<Card> neighborCards = (Vector<Card>) param3;

			    if (neighborCards.size() == 0) {
				System.out.println(fromName + " didn't have any cards. Go Fish!");
				askServerForOneCard();
				try {
				    serverResponse.wait();
				} catch (InterruptedException e) {
				    // TODO Auto-generated catch block
				    e.printStackTrace();
				}
				clientResponse.notify();
			    } else {
				Card c = neighborCards.get(0);
				System.out.println("You recieved " + neighborCards.size() + " " + c.getRank() + " from "
					+ fromName + "!");
				Vector<Card> newCards = hand.get(c.getRank());
				newCards.addAll(neighborCards);
				hand.put(c.getRank(), newCards);
				checkForBooks();
				clientResponse.notify();
			    }
			}
		    } else if (commandType == NetworkCommand.ENDTURN) {
			String fromName = (String) param1;

			if (computerName.equals(fromName)) {
			    yourTurnMonitor.notify();
			}
		    }
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    } catch (ClassNotFoundException cnfe) {
		cnfe.printStackTrace();
	    }
	}

	@Override
	protected Object call() throws Exception {
	    // Not using this call, but we need to override it to compile
	    return null;
	}
    }

    private static class ListenForServerUpdates extends Task<Object> {

	@Override
	public void run() {
	    try {
		while (true) {
		    System.out.println("before reading object");
		    Command input = (Command) inputFromServer.readObject();
		    System.out.println("after reading object");
		    NetworkCommand commandType = input.getCommand();
		    Object param1 = input.getParam1();
		    Object param2 = input.getParam2();
		    Object param3 = input.getParam3();

		    if (commandType == NetworkCommand.INDEX) {
			initializeHand((Vector<Card>) param1);
			System.out.println("Welcome to Go Fish, " + computerName + "!");
			System.out.println("Here is your hand: ");
			System.out.println(hand);
		    } else if (commandType == NetworkCommand.GAMEOVER) {
			if (index == (int) param1)
			    System.out.println("Congratulations!!!! You won!");
			else
			    System.out.println("Player " + (int) param1 + " won the game!");
			sc.close();
			System.exit(0);
		    } else if (commandType == NetworkCommand.FIVECARDS) {
			numCards = (int) param1;
			initializeHand((Vector<Card>) param2);
			// check if there's a book
			checkForBooks();
			System.out.println("You received " + numCards + " cards: ");
			System.out.println(hand);
		    } else if (commandType == NetworkCommand.ONECARD) {
			numCards += 1;
			addOneCard((Card) param1);
			checkForBooks();
			serverResponse.notify();
			// check if there's a book
			System.out.println("You received: " + (Card) param1);
			System.out.println("New hand: \n" + hand);
		    } else if (commandType == NetworkCommand.OUTOFCARDS) {
			System.out.println("There are no more cards remaining in the deck!");
			serverResponse.notify();
		    }
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    } catch (ClassNotFoundException cnfe) {
		cnfe.printStackTrace();
	    }
	}

	private void addOneCard(Card c) {
	    Vector<Card> cards = hand.get(c.getRank());
	    cards.add(c);
	    hand.put(c.getRank(), cards);
	    if (cards.size() == 4)
		handleBook(c.getRank());
	}

	private void handleBook(Rank rank) {
	    // tell the server that you have a book
	    try {
		outputToServer.reset();
		outputToServer.writeObject(new Command(NetworkCommand.BOOK, computerName, rank));
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}

	private void initializeHand(Vector<Card> cards) {
	    for (Card c : cards) {
		Vector<Card> handVector = hand.get(c.getRank());
		handVector.add(c);
		hand.put(c.getRank(), handVector);
		if (handVector.size() == 4)
		    handleBook(c.getRank());
	    }
	}

	@Override
	protected Object call() throws Exception {
	    // Not using this call, but we need to override it to compile
	    return null;
	}
    }

    private void setIndex(int num) {
	this.index = num;
    }

    public static void askServerForOneCard() {
	Command cmd = new Command(NetworkCommand.ONECARD);
	try {
	    outputToServer.reset();
	    outputToServer.writeObject(cmd);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private static String getName(int index) {
	String name = "";

	switch (index) {
	case 1:
	    name = "harbor";
	    break;
	case 2:
	    name = "harvard";
	    break;
	case 3:
	    name = "harpoon";
	    break;
	case 4:
	    name = "harlem";
	    break;
	}
	return name + Arizona;
    }

    public static void checkForBooks() {
	Set<Rank> ranks = hand.keySet();

	for (Rank r : ranks) {
	    if (hand.get(r).size() == 4) {
		System.out.println("You formed a book of " + r);
		books.add(r);
		Command cmd = new Command(NetworkCommand.BOOK, computerName, r);
		try {
		    outputToServer.reset();
		    outputToServer.writeObject(cmd);
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}
    }

}
