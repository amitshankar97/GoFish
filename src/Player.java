import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import javafx.concurrent.Task;

public class Player {
    private static int index;
    private static int numCards;
    
    private static Vector<Card> hand;

    // Connection variables
    private static Socket socketFromServer;
    private static ObjectOutputStream outputToServer;
    private static ObjectInputStream  inputFromServer;

    private ServerSocket toNeighbor;
    private static Vector<Socket> neighbors;

    private static final String Address = "localhost";
    
    
    public static void main(String args[]) {
	makeConnection();
    }


    private static void makeConnection() {
	// Our server is on our computer, but make sure to use the same port.
	try {
	    socketFromServer = new Socket(Address, 10495);
	    outputToServer = new ObjectOutputStream(socketFromServer.getOutputStream());
	    inputFromServer = new ObjectInputStream(socketFromServer.getInputStream());

	    // SeverListener will have a while(true) loop
	    ListenForServerUpdates listener = new ListenForServerUpdates();
	    // TODO 6: Start a new Thread that reads from the server
	    // Note: Need setDaemon when started with a JavaFX App, or it crashes.
	    Thread thread = new Thread(listener);
	    thread.setDaemon(true);
	    thread.start();
	} catch (IOException e) {
	}
    }




    private static class ListenForServerUpdates extends Task<Object> {

	@Override
	public void run() {
	    try {
		while(true) {
		    System.out.println("before reading object");
		    Command input = (Command) inputFromServer.readObject();
		    System.out.println("after reading object");
		    NetworkCommand commandType = input.getCommand();
		    Object param1 = input.getParam1();
		    Object param2 = input.getParam2();
		    Object param3 = input.getParam3();

		    if(commandType == NetworkCommand.WELCOME) {
			System.out.println("Waiting on other players....");
		    }
		    if(commandType == NetworkCommand.INDEX) {
			index = (int) param1;
			hand = (Vector<Card>) param2; // initialize the deck here
			neighbors = (Vector<Socket>) param3; // store the other players
			
			System.out.println("Welcome to Go Fish! You are Player " + index);
		    }
		    else if(commandType == NetworkCommand.GAMEOVER) {
			if(index == (int) param1)
			    System.out.println("Congratulations!!!! You won!");
			else
			    System.out.println("Player " + (int) param1 + " won the game!");
		    }
		    else if(commandType == NetworkCommand.FIVECARDS) {
			numCards = (int) param1;
			hand = (Vector<Card>) param2;
			
			System.out.println("You received " + numCards + " cards: ");
			System.out.println(hand);
		    }
		    else if(commandType == NetworkCommand.ONECARD) {
			numCards += 1;
			hand.add((Card) param1);
			
			System.out.println("You received: " + (Card) param1);
			System.out.println("New hand: \n" + hand);
		    }
		}
	    } catch(IOException ioe) {}
	    catch(ClassNotFoundException cnfe) {}
	    System.out.println("CLOSED FOR BUSINESS");
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
