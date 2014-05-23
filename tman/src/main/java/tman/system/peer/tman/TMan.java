package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;

import java.util.ArrayList;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.DescriptorBuffer;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TMan.class);

    Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    private long period;
    private Address self;
    private PeerDescriptor selfDescriptor;
    private ArrayList<PeerDescriptor> tmanPartnersByRes;
    private ArrayList<PeerDescriptor> tmanPartnersByCpus;
    private ArrayList<PeerDescriptor> tmanPartnersByMem;
    private ArrayList<PeerDescriptor> cyclonPartners;
    
    private DescriptorBuffer myBuffer;
    
    private TManConfiguration tmanConfiguration;
    private Random r;
    private AvailableResources availableResources;
    


    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

        public TManSchedule(ScheduleTimeout request) {
            super(request);
        }
    }

    public TMan() {
    	cyclonPartners = new ArrayList<PeerDescriptor>();
        tmanPartnersByRes = new ArrayList<PeerDescriptor>();
        tmanPartnersByCpus = new ArrayList<PeerDescriptor>();
        tmanPartnersByMem = new ArrayList<PeerDescriptor>();
        
        selfDescriptor = new PeerDescriptor(self, availableResources);
        
        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handletmanPartnersResponse, networkPort);
        subscribe(handletmanPartnersRequest, networkPort);
    }

    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
        	
        	System.out.println("TMan is initialized!!");
            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            period = tmanConfiguration.getPeriod();
            r = new Random(tmanConfiguration.getSeed());
            availableResources = init.getAvailableResources();
            
            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);

        }
    };

    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {
            Snapshot.updateTManPartners(new PeerDescriptor(self,  availableResources), tmanPartnersByRes);
            

            PeerDescriptor selectedPeer;
            selectedPeer = selectPeer(tmanPartnersByRes.size() / 2, tmanPartnersByRes);
            if(!tmanPartnersByRes.contains(selfDescriptor)) {
            	tmanPartnersByRes.add(selfDescriptor);
            }
            Collections.sort(tmanPartnersByRes, new ComparatorByResources(new PeerDescriptor(selectedPeer.getAddress(), selectedPeer.getAv())));

            myBuffer = new DescriptorBuffer(self, tmanPartnersByRes);
            //check null selectedPeer
            trigger(new ExchangeMsg.Request(UUID.randomUUID(), myBuffer, self, selectedPeer.getAddress()), networkPort);
            
            // Publish sample to connected components
            trigger(new TManSample(tmanPartnersByRes), tmanPort);
        }
    };

    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            cyclonPartners = event.getSample();

            // merge cyclonPartners into tmanPartnersByRes
            tmanPartnersByRes.clear();
            
            if(!cyclonPartners.isEmpty()) {
                tmanPartnersByRes.addAll(cyclonPartners);
                tmanPartnersByCpus.addAll(cyclonPartners);
                tmanPartnersByMem.addAll(cyclonPartners);
                
                System.out.println("In TMan : cyclonPartners -> " + cyclonPartners);
                System.out.println("In TMan : tmanPartnersByRes -> " + tmanPartnersByRes);
                for(PeerDescriptor pd : tmanPartnersByRes) {
                	System.out.println(pd.getAv().getNumFreeCpus() + " " + pd.getAv().getFreeMemInMbs());
                }
                Collections.sort(tmanPartnersByRes, new ComparatorByResources(new PeerDescriptor(self, availableResources)));
                Collections.sort(tmanPartnersByCpus, new ComparatorByNumCpu(new PeerDescriptor(self, availableResources)));
                Collections.sort(tmanPartnersByMem, new ComparatorByNumMem(new PeerDescriptor(self, availableResources)));

                System.out.println("In TMan : tmanPartnersByRes -> " + tmanPartnersByRes);
                for(PeerDescriptor pd : tmanPartnersByRes) {
                	System.out.println(pd.getAv().getNumFreeCpus() + pd.getAv().getFreeMemInMbs());
                }


            }
            else {
            	System.out.println("empty sample");
            }
        }
    };

    Handler<ExchangeMsg.Request> handletmanPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
        	ArrayList<PeerDescriptor> receivedView = event.getRandomBuffer().getDescriptors();
        	PeerDescriptor from = null;
        	for(PeerDescriptor pd : event.getRandomBuffer().getDescriptors()) {
        		if(pd.getAddress() == event.getSource()) {
        			from = pd;
        			break;
        		}
        	}
        	if(!tmanPartnersByRes.contains(selfDescriptor)) {
        		tmanPartnersByRes.add(selfDescriptor);
        	}
            Collections.sort(tmanPartnersByRes, new ComparatorByResources(new PeerDescriptor(event.getSource(),
            						from.getAv())));
            myBuffer = new DescriptorBuffer(self, tmanPartnersByRes);
            trigger(new ExchangeMsg.Response(UUID.randomUUID(), myBuffer, self, event.getSource()), networkPort);
            tmanPartnersByRes.addAll(receivedView);
            HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByRes);
            tmanPartnersByRes.clear();
            tmanPartnersByRes.addAll(hs);
        }
    };

    Handler<ExchangeMsg.Response> handletmanPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {
        	ArrayList<PeerDescriptor> receivedView = event.getSelectedBuffer().getDescriptors();
            tmanPartnersByRes.addAll(receivedView);
        	HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByRes);
            tmanPartnersByRes.clear();
            tmanPartnersByRes.addAll(hs);
        }
    };
    
    public void PrintMsg(String msg) {
    	logger.info("Peer " + self.getId() + " :" + msg);
    }
    
    public PeerDescriptor selectPeer(int psi, ArrayList<PeerDescriptor> view) {
    	if(view.size() == 0) {
    		return null;
    	}
    	else if(view.size() == 1) {
    		return view.get(0);
    	}
    	else {
    		return view.get(r.nextInt(psi));
    	}
    }

    // TODO - if you call this method with a list of entries, it will
    // return a single node, weighted towards the 'best' node (as defined by
    // ComparatorById) with the temperature controlling the weighting.
    // A temperature of '1.0' will be greedy and always return the best node.
    // A temperature of '0.000001' will return a random node.
    // A temperature of '0.0' will throw a divide by zero exception :)
    // Reference:
    // http://webdocs.cs.ualberta.ca/~sutton/book/2/node4.html
    public Address getSoftMaxAddress(List<Address> entries) {
        Collections.sort(entries, new ComparatorById(self));

        double rnd = r.nextDouble();
        double total = 0.0d;
        double[] values = new double[entries.size()];
        int j = entries.size() + 1;
        for (int i = 0; i < entries.size(); i++) {
            // get inverse of values - lowest have highest value.
            double val = j;
            j--;
            values[i] = Math.exp(val / tmanConfiguration.getTemperature());
            total += values[i];
        }

        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                values[i] += values[i - 1];
            }
            // normalise the probability for this entry
            double normalisedUtility = values[i] / total;
            if (normalisedUtility >= rnd) {
                return entries.get(i);
            }
        }
        return entries.get(entries.size() - 1);
    }

}
