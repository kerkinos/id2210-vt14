package tman.system.peer.tman;

import java.util.ArrayList;



import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class TManSample extends Event {
	ArrayList<PeerDescriptor> partnersByRes = new ArrayList<PeerDescriptor>();
	ArrayList<PeerDescriptor> partnersByCpu = new ArrayList<PeerDescriptor>();
	ArrayList<PeerDescriptor> partnersByMem = new ArrayList<PeerDescriptor>();

	public TManSample(ArrayList<PeerDescriptor> partnersByRes, ArrayList<PeerDescriptor> partnersByCpu,
			ArrayList<PeerDescriptor> partnersByMem) {
		this.partnersByRes= partnersByRes;
		this.partnersByCpu = partnersByCpu;
		this.partnersByMem = partnersByMem;
	}
        
	public ArrayList<PeerDescriptor> getPartnersByRes() {
		return partnersByRes;
	}

	public ArrayList<PeerDescriptor> getPartnersByCpu() {
		return partnersByCpu;
	}

	public ArrayList<PeerDescriptor> getPartnersByMem() {
		return partnersByMem;
	}

}
