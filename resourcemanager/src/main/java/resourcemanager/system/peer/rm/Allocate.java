package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;



public class Allocate extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6196336565685562525L;
	private final int numCpus;
	private final int amountMem;
	private final int time;
	
	public Allocate(Address source, Address destination, int numCpus, int amountMem, int time) {
		super(source, destination);
		this.numCpus = numCpus;
		this.amountMem = amountMem;
		this.time = time;
	}

	public int getNumCpus() {
		return numCpus;
	}

	public int getAmountMem() {
		return amountMem;
	}

	public int getTime() {
		return time;
	}  	
	
}

