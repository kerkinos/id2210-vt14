package tman.simulator.snapshot;

import java.util.ArrayList;

import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.address.Address;


public class PeerInfo {
	private ArrayList<PeerDescriptor> tmanPartnersByRes;
	private ArrayList<PeerDescriptor> tmanPartnersByCpu;
	private ArrayList<PeerDescriptor> tmanPartnersByMem;
	private ArrayList<Address> cyclonPartners;


	public PeerInfo() {
		this.tmanPartnersByRes = new ArrayList<PeerDescriptor>();
		this.tmanPartnersByCpu = new ArrayList<PeerDescriptor>();
		this.tmanPartnersByMem = new ArrayList<PeerDescriptor>();
		this.cyclonPartners = new ArrayList<Address>();
	}


	public void updateTManPartnersByRes(ArrayList<PeerDescriptor> partners) {
		this.tmanPartnersByRes = partners;
	}
	
	public void updateTManPartnersByCpu(ArrayList<PeerDescriptor> partners) {
		this.tmanPartnersByCpu = partners;
	}
	
	public void updateTManPartnersByMem(ArrayList<PeerDescriptor> partners) {
		this.tmanPartnersByMem = partners;
	}

	public void updateCyclonPartners(ArrayList<Address> partners) {
		this.cyclonPartners = partners;
	}


	public ArrayList<PeerDescriptor> getTManPartnersByRes() {
		return this.tmanPartnersByRes;
	}
	
	public ArrayList<PeerDescriptor> getTManPartnersByCpu() {
		return this.tmanPartnersByCpu;
	}
	
	public ArrayList<PeerDescriptor> getTManPartnersByMem() {
		return this.tmanPartnersByMem;
	}


	public ArrayList<Address> getCyclonPartners() {
		return this.cyclonPartners;
	}
}
