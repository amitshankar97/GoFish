
public class Command {
    private NetworkCommand type;
    private Object param1;
    private Object param2;
    private Object param3;
    
    public Command(NetworkCommand type, Object param1, Object param2, Object param3) {
	this.type = type;
	this.param1 = param1;
	this.param2 = param2;
	this.param3 = param3;
    }
    
    public NetworkCommand getCommand() {
	return this.type;
    }
    
    public Object getParam1() {
	return this.param1;
    }

    public Object getParam2() {
	return this.param2;
    }

    public Object getParam3() {
	return this.param3;
    }
}
