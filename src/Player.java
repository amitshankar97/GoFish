import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Player {
    private int index;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream  input;
    
    public Player(int num) {
	this.index = num;
    }
    
    
    
    
}
