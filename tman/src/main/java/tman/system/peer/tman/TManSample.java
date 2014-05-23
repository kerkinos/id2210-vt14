package tman.system.peer.tman;

import java.util.ArrayList;



import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class TManSample extends Event {
	ArrayList<PeerDescriptor> partners = new ArrayList<PeerDescriptor>();


	public TManSample(ArrayList<PeerDescriptor> partners) {
		this.partners = partners;
	}
        
	public TManSample() {
	}


	public ArrayList<PeerDescriptor> getSample() {
		return this.partners;
	}
}
