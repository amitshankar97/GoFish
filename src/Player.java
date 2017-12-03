import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.InputMismatchException;
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

    private static Object clientResponse;
    private static Object serverResponse;
    private static Object yourTurnMonitor;
    private static Object lock;

    private static boolean deckEmpty = false;
    private static boolean handEmpty = false;
    private static boolean hasToldServer = false;

    public static void main(String args[]) throws IOException {
	initializeComputerName();
	initializeBooksAndHand();

	yourTurnMonitor = new Object();
	clientResponse = new Object();
	serverResponse = new Object();
	lock = new Object();

	makeServerConnection();
	makePlayerConnections();

	try {
	    System.out.println("Loading game....\n\n");
	    Thread.sleep(10000);
	} catch (InterruptedException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	if (("harbor" + Arizona).equals(computerName)) {
	    myTurn();
	    System.out.println("Waiting for my turn...");
	    synchronized (yourTurnMonitor) {
		try {
		    yourTurnMonitor.wait();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		System.out.println("Monitor was signaled!");
	    }
	} else {
	    System.out.println("Waiting for my turn...");
	    synchronized (yourTurnMonitor) {
		try {
		    yourTurnMonitor.wait();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}

	while (true) {
	    myTurn();
	    System.out.println("Waiting for my turn...");
	    synchronized (yourTurnMonitor) {
		try {
		    yourTurnMonitor.wait();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
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
	isHandEmpty();
	if (!deckEmpty && handEmpty) {
	    askServerForFiveCards();
	    synchronized (serverResponse) {
		try {
		    serverResponse.wait();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	    isHandEmpty();
	}
	if (deckEmpty && handEmpty) {
	    if (!hasToldServer) {
		Command cmd9 = new Command(NetworkCommand.GAMEOVER);
		try {
		    outputToServer.writeObject(cmd9);

		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		hasToldServer = true;
	    }

	    Command cmd4 = new Command(NetworkCommand.ENDTURN, getNextPlayer());
	    
	    try {
		toSender.reset();
		toSender.writeObject(cmd4);
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    System.out.println("\nWaiting for other players to finish");
	    return;
	}

	System.out.println("Here are the possible commands:");
	System.out.println(
		"1) gofish: Lets you ask a player for a card, if unsuccessful, you will receive a card from the deck, if there are any"
			+ "\n2) quit: Quits the game" + "\n3) hand: Prints your current hand"
			+ "\n4) books: Prints the number of books");

	while (sc.hasNext()) {

	    Command cmd = null;
	    String userString = sc.nextLine();
	    userString = userString.toLowerCase();

	    if ("quit".equals(userString)) {
		System.out.println("Thank you for playing Go Fish!");
		System.exit(0);
	    } else if ("books".equals(userString)) {
		if (books.size() > 0) {
		    System.out.println("\nYou have the following books:");
		    System.out.println(books);
		} else {
		    System.out.println("\nYou have no books :(");
		}
	    } else if ("gofish".equals(userString)) {
		int pNumber, cardNumber;

		while (true) {
		    System.out.println(
			    "\nPlease select the player you would like to ask" + " for a card from the list below:");

		    for (int i = 1; i < 5; i++) {
			String name = getName(i);
			if (computerName.equals(name))
			    continue;
			System.out.println(i + ": " + name);
		    }

		    System.out.print("\nEnter player number: ");
		    try {
			pNumber = sc.nextInt();
		    } catch (Exception e) {
			System.out.println("\nInvalid player number.");
			continue;
		    }
		    if (getName(pNumber).equals(computerName)) {
			System.out.println("\nSorry! You cannot select yourself");
			continue;
		    } else if (pNumber > 4 || pNumber < 0) {
			System.out.println("\nInvalid player number.");
			continue;
		    } else
			break;
		}

		while (true) {
		    System.out.println("\nPlease enter the card you would like to request: ");
		    printOptions();
		    System.out.print("\nEnter card number: ");
		    cardNumber = sc.nextInt();

		    if (isValid(cardNumber)) {
			askPlayer(pNumber, cardNumber);
			synchronized (clientResponse) {
			    try {
				clientResponse.wait();
			    } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    }
			}
		    } else {
			System.out.println("Sorry! That is not a valid choice.");
			continue;
		    }
		    break;
		}
		break;
	    } else if ("hand".equals(userString)) {
		printHand();
	    } else {
		System.out.println("Sorry! Didn't recognize the command.");
	    }

	    System.out.println(
		    "\n1) gofish: Lets you ask a player for a card, if unsuccessful, you will receive a card from the deck, if there are any"
			    + "\n2) quit: Quits the game" + "\n3) hand: Prints your current hand"
			    + "\n4) books: Prints the number of books");
	}


	if (!deckEmpty && handEmpty) {
	    askServerForFiveCards();
	    synchronized (serverResponse) {
		try {
		    serverResponse.wait();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}

	printHand();
	System.out.println("Your turn has ended");
	Command cmd4 = new Command(NetworkCommand.ENDTURN, getNextPlayer());
	try {
	    toSender.reset();
	    toSender.writeObject(cmd4);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private static void askServerForFiveCards() {
	try {
	    outputToServer.writeObject(new Command(NetworkCommand.FIVECARDS));
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private static String getNextPlayer() {
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

    private static void makePlayerConnections() throws IOException {

	int connectionsMade = 0;

	if (("harlem" + Arizona).equals(computerName) || ("harvard" + Arizona).equals(computerName)) {
	    fromNeighbor = new ServerSocket(10495, 5);
	    int count = 1;
	    System.out.println(computerName + " listening on port 10495");
	    while (true) {
		Socket socket = fromNeighbor.accept();

		String connectorName = socket.getInetAddress().getHostName();
		// System.out.println("connectorName: " + connectorName);

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

	ListenForClientUpdates listener = new ListenForClientUpdates(clientResponse, yourTurnMonitor);
	// TODO 6: Start a new Thread that reads from the server
	// Note: Need setDaemon when started with a JavaFX App, or it
	// crashes.
	Thread thread = new Thread(listener);
	thread.setDaemon(true);
	thread.start();

	ListenForClientUpdates listener2 = new ListenForClientUpdates(clientResponse, yourTurnMonitor);
	// TODO 6: Start a new Thread that reads from the server
	// Note: Need setDaemon when started with a JavaFX App, or it
	// crashes.
	Thread thread1 = new Thread(listener);
	thread1.setDaemon(true);
	thread1.start();
	System.out.println("All connections have been made!");
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

	// if the user doesn't have that card in their hand
	if (hand.get(convertToRank(cardNumber)).size() == 0)
	    return false;

	return true;
    }

    private static void printOptions() {
	for (Rank r : Rank.values()) {
	    Vector<Card> cards = hand.get(r);
	    if (cards.size() > 0) {
		// TODO: remove 4 cards from hand hashmap if we have a book
		printOptions(r);
	    }
	}
    }

    private static void printOptions(Rank r) {
	System.out.println("For " + r + " type " + r.getValue());
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
	    ListenForServerUpdates listener = new ListenForServerUpdates(lock, serverResponse);
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

	private Object clientResponse;
	private Object yourTurnMonitor;

	public ListenForClientUpdates(Object clientResponse, Object yourTurnMonitor) {
	    this.clientResponse = clientResponse;
	    this.yourTurnMonitor = yourTurnMonitor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void run() {
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
			    System.out.println("I was asked for " + r);
			    Vector<Card> returnCards = hand.get(r);
			    hand.put(r, new Vector<Card>());
			    isHandEmpty();
			    Command cmd = new Command(NetworkCommand.GOFISHRES, param2, param1, returnCards);
			    Command cmd2 = new Command(NetworkCommand.GOFISHREQ, param1, param2, param3);
			    System.out.print("New hand: ");
			    printHand();
			    System.out.println();
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
				System.out.println("\n\n" + fromName + " didn't have any cards. Go Fish!");
				askServerForOneCard();

				synchronized (serverResponse) {
				    try {
					serverResponse.wait();
				    } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				}

				synchronized (this.clientResponse) {
				    this.clientResponse.notify();
				}
			    } else {
				Card c = neighborCards.get(0);
				System.out.println("You recieved " + neighborCards.size() + " " + c.getRank() + " from "
					+ fromName + "!");
				Vector<Card> newCards = hand.get(c.getRank());
				newCards.addAll(neighborCards);
				hand.put(c.getRank(), newCards);
				checkForBooks();
				synchronized (this.clientResponse) {
				    this.clientResponse.notify();
				}
			    }
			} else {
			    System.out.println(param1 + " gave " + param2 + " a " + param3);
			    toSender.reset();
			    toSender.writeObject(input);
			}
		    } else if (commandType == NetworkCommand.ENDTURN) {
			String fromName = (String) param1;

			if (computerName.equals(fromName)) {
			    System.out.println("\n\nIt's your turn now!");
			    synchronized (this.yourTurnMonitor) {
				this.yourTurnMonitor.notifyAll();
			    }
			} else {
			    toSender.reset();
			    toSender.writeObject(input);
			}
		    }
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.out.println("A client has left the game.");
		System.exit(0);
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

	private Object lock;
	private Object serverResponse;

	public ListenForServerUpdates(Object lock, Object serverResponse) {
	    this.lock = lock;
	    this.serverResponse = serverResponse;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
	    try {
		while (true) {
		    Command input = (Command) inputFromServer.readObject();
		    NetworkCommand commandType = input.getCommand();
		    Object param1 = input.getParam1();
		    Object param2 = input.getParam2();
		    Object param3 = input.getParam3();

		    if (commandType == NetworkCommand.INDEX) {
			initializeHand((Vector<Card>) param1);
			System.out.println("Welcome to Go Fish, " + computerName + "!");
			System.out.println("Here is your hand: ");
			printHand();
			/*
			 * synchronized(this.lock) { this.lock.notify();
			 * System.out.println("After lock.notify()"); }
			 */
		    } else if (commandType == NetworkCommand.GAMEOVER) {
			if (computerName.equals(param1))
			    System.out.println("\n\nCongratulations!!!! You won GoFish!");
			else
			    System.out.println("Player " + (String) param1 + " won the game!");
			sc.close();
			System.exit(0);
		    } else if (commandType == NetworkCommand.FIVECARDS) {
			numCards = (int) param1;
			initializeHand((Vector<Card>) param2);
			// check if there's a book
			checkForBooks();
			isHandEmpty();
			System.out.println("You received " + numCards + " cards from the server.");
			System.out.print("New hand: ");
			printHand();
			System.out.println();

			synchronized (this.serverResponse) {
			    this.serverResponse.notify();
			}
		    } else if (commandType == NetworkCommand.ONECARD) {
			numCards += 1;
			addOneCard((Card) param1);
			checkForBooks();
			isHandEmpty();
			synchronized (this.serverResponse) {
			    this.serverResponse.notify();
			}
			// check if there's a book

			System.out.println("\nYou received: " + (Card) param1 + " from the deck!");
			System.out.print("New hand: ");
			printHand();
			System.out.println();
		    } else if (commandType == NetworkCommand.OUTOFCARDS) {
			System.out.println("There are no more cards remaining in the deck!");
			synchronized (serverResponse) {
			    serverResponse.notify();
			}
			deckEmpty = true;
		    }
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.out.println("The dealer has left the game.");
		System.exit(0);
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

    public static void isHandEmpty() {
	for (Rank r : Rank.values()) {
	    if (hand.get(r).size() != 0) {
		handEmpty = false;
		return;
	    }
	}
	handEmpty = true;
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

    public static void printHand() {
	Vector<Card> printCards = new Vector<Card>();

	for (Rank r : Rank.values()) {
	    printCards.addAll(hand.get(r));
	}
	if (printCards.size() == 0)
	    System.out.println("You have no cards");
	else
	    System.out.println("\n" + printCards);
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
		hand.put(r, new Vector<Card>()); // reset the cards to size 0
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
