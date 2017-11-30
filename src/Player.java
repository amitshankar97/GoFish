import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import javafx.concurrent.Task;

public class Player {
    private static int index;
    private static int numCards;

    private static HashMap<Rank, Vector<Card>> hand;
    private static Vector<Card> books;

    // Connection variables
    private static Socket socketFromServer;
    private static ObjectOutputStream outputToServer;
    private static ObjectInputStream inputFromServer;

    private static ServerSocket fromNeighbor;
    private static Vector<Socket> neighbors;
    private static ArrayList<ObjectOutputStream> neighborOutputStreams;
    private static ArrayList<ObjectInputStream> neighborInputStreams;

    private static final String Address = "localhost";

    private static Scanner sc = new Scanner(System.in);

    public static void main(String args[]) throws IOException {
	initializeBooksAndHand();
	makeServerConnection();
	makePlayerConnections();

	System.out.println("Welcome to Go Fish! Here are the possible commands:");
	System.out.println("1) gofish\n2) quit");

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
		    System.out.print("Please enter the player you would like to request for a card: ");
		    pNumber = sc.nextInt();
		    if (pNumber == index)
			System.out.println("Sorry! You cannot select yourself");
		    else if (pNumber > 4 || pNumber < 0)
			System.out.println("Invalid player number.");
		    else
			break;
		}

		while (true) {
		    System.out.print("Please enter the card you would like to request: ");
		    printOptions();
		    cardNumber = sc.nextInt();

		    if (isValid(cardNumber)) {
			askPlayer(pNumber, cardNumber);
		    } else {
			System.out.println("Sorry! That is not a valid choice.");
		    }
		}

	    } else {
		System.out.println("Sorry! Didn't recognize the command.");
		continue;
	    }

	    try {
		outputToServer.writeObject(cmd);
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    System.out.println("Hi client, you wrote: " + userString);
	    if (userString.toLowerCase().equals("quit")) {
		System.out.println("You entered the magic word");
		break;
	    } else {
		System.out.print("Enter a message: ");
	    }

	}

	sc.close();

    }

    private void initializeBooksAndHand() {
	// initialize the hand
	hand = new HashMap<Rank, Vector<Card>>();
	for(Rank r: Rank.values()) {
	    hand.put(r, new Vector<Card>());
	}
	
	// initialize the books
	books = new Vector<Card>();
    }

    private void makePlayerConnections() throws IOException {
	boolean listen = index % 2 == 0;
	int connectionsMade = 0;

	while (true) {
	    if (listen) {
		fromNeighbor = new ServerSocket(10495 + index);
		System.out.println("Player " + index + " listening on port " + (10495 + index));
		while (true) {
		    Socket socket = fromNeighbor.accept();
		    neighbors.add(socket);
		    System.out.println("Player " + neighbors.size() + " connected.");
		    connectionsMade++;

		    // Connections finished, connect the last 2 even players
		    if (connectionsMade == 2) {
			if (index == 2) {
			    while (true) {
				Socket lastSocket = fromNeighbor.accept();
				neighbors.add(lastSocket);

				neighborOutputStreams.add((ObjectOutputStream) lastSocket.getOutputStream());
				neighborInputStreams.add((ObjectInputStream) lastSocket.getInputStream());

				System.out.println("Player 4 connected");
			    }
			} else {
			    socket = new Socket(Address, 10497);
			    neighborOutputStreams.add((ObjectOutputStream) socket.getOutputStream());
			    neighborInputStreams.add((ObjectInputStream) socket.getInputStream());

			    System.out.println("Player " + index + " listening on port " + (10495 + index));
			}
			connectionsMade++;
			break;
		    }
		}
	    } else {
		int playerToConnect = 2;
		while (playerToConnect <= 4) {
		    Socket socket = new Socket(Address, 10495 + playerToConnect);
		    outputToServer = new ObjectOutputStream(socket.getOutputStream());
		    inputFromServer = new ObjectInputStream(socket.getInputStream());
		    playerToConnect += 2;
		}
	    }
	}
    }

    private void askPlayer(int pNumber, int cardNumber) {
	Command cmd = new Command(NetworkCommand.GOFISHREQ, pNumber, cardNumber);
	for (int i = 0; i < neighbors.size(); i++) {
	    if (i == index)
		continue;
	}
    }

    private boolean isValid(int cardNumber) {
	// TODO Auto-generated method stub
	return false;
    }

    private void printOptions() {
	// TODO Auto-generated method stub

    }

    private void makeServerConnection() {
	// Our server is on our computer, but make sure to use the same port.
	try {
	    socketFromServer = new Socket(Address, 10495);
	    outputToServer = new ObjectOutputStream(socketFromServer.getOutputStream());
	    inputFromServer = new ObjectInputStream(socketFromServer.getInputStream());

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

    private class ListenForClientUpdates extends Task<Object> {

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

		    if (commandType == NetworkCommand.GOFISHREQ) {
			int fromIndex = (int) param1;
			int toIndex = (int) param2;
			Rank cardRank = (Rank) param3;

			if(toIndex == index) {
			    if()
			}

		    } else if (commandType == NetworkCommand.GAMEOVER) {
			if (index == (int) param1)
			    System.out.println("Congratulations!!!! You won!");
			else
			    System.out.println("Player " + (int) param1 + " won the game!");
		    } else if (commandType == NetworkCommand.FIVECARDS) {
			numCards = (int) param1;

			System.out.println("You received " + numCards + " cards: ");
			System.out.println(hand);
		    } else if (commandType == NetworkCommand.ONECARD) {
			numCards += 1;
			hand.add((Card) param1);

			System.out.println("You received: " + (Card) param1);
			System.out.println("New hand: \n" + hand);
		    }
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    } catch (ClassNotFoundException cnfe) {
		cnfe.printStackTrace();
	    }
	    System.out.println("CLOSED FOR BUSINESS");
	}

	@Override
	protected Object call() throws Exception {
	    // Not using this call, but we need to override it to compile
	    return null;
	}
    }

    private class ListenForServerUpdates extends Task<Object> {

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

		    if (commandType == NetworkCommand.WELCOME) {
			System.out.println("Waiting on other players....");
		    }
		    if (commandType == NetworkCommand.INDEX) {
			index = (int) param1;
			initializeHand((Vector<Card>) param2);
			System.out.println("Welcome to Go Fish! You are Player " + index);
		    } else if (commandType == NetworkCommand.GAMEOVER) {
			if (index == (int) param1)
			    System.out.println("Congratulations!!!! You won!");
			else
			    System.out.println("Player " + (int) param1 + " won the game!");
		    } else if (commandType == NetworkCommand.FIVECARDS) {
			numCards = (int) param1;
			initializeHand((Vector<Card>) param2);
			// check if there's a book
			System.out.println("You received " + numCards + " cards: ");
			System.out.println(hand);
		    } else if (commandType == NetworkCommand.ONECARD) {
			numCards += 1;
			addOneCard((Card) param1);
			// check if there's a book
			System.out.println("You received: " + (Card) param1);
			System.out.println("New hand: \n" + hand);
		    }
		}
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    } catch (ClassNotFoundException cnfe) {
		cnfe.printStackTrace();
	    }
	    System.out.println("CLOSED FOR BUSINESS");
	}

	private void addOneCard(Card c) {
	    Vector<Card> cards = hand.get(c.getRank());
	    cards.add(c);
	    hand.put(c.getRank(), cards);
	    if(cards.size() == 4)
		handleBook(c.getRank());
	}

	private void handleBook(Rank rank) {
	    
	}

	private void initializeHand(Vector<Card> cards) {
	    for(Card c: cards) {
		Vector<Card> handVector = hand.get(c.getRank());
		handVector.add(c);
		hand.put(c.getRank(), handVector);
		if(handVector.size() == 4)
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

}
