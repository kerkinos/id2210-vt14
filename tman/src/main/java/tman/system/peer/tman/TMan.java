package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;

import java.util.LinkedList;

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
    private LinkedList<PeerDescriptor> tmanPartnersByRes;
    private LinkedList<PeerDescriptor> tmanPartnersByCpus;
    private LinkedList<PeerDescriptor> tmanPartnersByMem;
    private LinkedList<PeerDescriptor> cyclonPartners;
    
    private DescriptorBuffer myBufferRes;
    private DescriptorBuffer myBufferCpu;
    private DescriptorBuffer myBufferMem;
    
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
    	cyclonPartners = new LinkedList<PeerDescriptor>();
        tmanPartnersByRes = new LinkedList<PeerDescriptor>();
        tmanPartnersByCpus = new LinkedList<PeerDescriptor>();
        tmanPartnersByMem = new LinkedList<PeerDescriptor>();
      
        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handletmanPartnersResponseRes, networkPort);
        subscribe(handletmanPartnersResponseCpu, networkPort);
        subscribe(handletmanPartnersResponseMem, networkPort);
        subscribe(handletmanPartnersRequestRes, networkPort);
        subscribe(handletmanPartnersRequestCpu, networkPort);
        subscribe(handletmanPartnersRequestMem, networkPort);       
    }

    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
        	
//        	System.out.println("TMan is initialized!!");
            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            period = tmanConfiguration.getPeriod();
            r = new Random(tmanConfiguration.getSeed());
            availableResources = init.getAvailableResources();
            selfDescriptor = new PeerDescriptor(self, availableResources);

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);

        }
    };

    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {
            Snapshot.updateTManPartnersByRes(new PeerDescriptor(self,  availableResources), tmanPartnersByRes);
            Snapshot.updateTManPartnersByCpu(new PeerDescriptor(self,  availableResources), tmanPartnersByCpus);
            Snapshot.updateTManPartnersByMem(new PeerDescriptor(self,  availableResources), tmanPartnersByMem);
            

            trigger(new TManSample(tmanPartnersByRes, tmanPartnersByCpus, tmanPartnersByMem), tmanPort);
        }
    };

    
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            cyclonPartners = event.getSample();
            

            // merge cyclonPartners into tmanPartnersByRes
            tmanPartnersByRes.clear();
            tmanPartnersByCpus.clear();
            tmanPartnersByMem.clear();
            
            if(!cyclonPartners.isEmpty()) {
                tmanPartnersByRes.addAll(cyclonPartners);
                tmanPartnersByCpus.addAll(cyclonPartners);
                tmanPartnersByMem.addAll(cyclonPartners);
                
                
//                System.out.println(self.getId() + " In TMan : tmanPartnersByRes before -> " + tmanPartnersByRes);
//                for(PeerDescriptor pd : tmanPartnersByRes) {
//                	System.out.println(pd.getAddress().getId() + " " + pd.getAv().getNumFreeCpus() * pd.getAv().getFreeMemInMbs());
//                }
                // we rank our view according to our Descriptor
                Collections.sort(tmanPartnersByRes, new ComparatorByResources(new PeerDescriptor(self, availableResources)));
                Collections.sort(tmanPartnersByCpus, new ComparatorByNumCpu(new PeerDescriptor(self, availableResources)));
                Collections.sort(tmanPartnersByMem, new ComparatorByNumMem(new PeerDescriptor(self, availableResources)));

//                System.out.println(self.getId() + " In TMan : tmanPartnersByRes after sort -> " + tmanPartnersByRes);
//                for(PeerDescriptor pd : tmanPartnersByRes) {
//                	System.out.println(pd.getAddress().getId() + " " + pd.getAv().getNumFreeCpus() * pd.getAv().getFreeMemInMbs());
//                }
                
                // p <- selectPeer(psi, rank(myDescriptor, view))
                PeerDescriptor selectedPeerByRes, selectedPeerByCpu, selectedPeerByMem;
                selectedPeerByRes = selectPeer(tmanPartnersByRes.size() / 3, tmanPartnersByRes);
                selectedPeerByCpu = selectPeer(tmanPartnersByCpus.size() / 3, tmanPartnersByCpus);
                selectedPeerByMem = selectPeer(tmanPartnersByMem.size() / 3, tmanPartnersByMem);

  
                // buffer <- merge(view, {myDescriptor})
                if(!tmanPartnersByRes.contains(selfDescriptor)) {
                	tmanPartnersByRes.add(selfDescriptor);
                }
                
                if(!tmanPartnersByCpus.contains(selfDescriptor)) {
                	tmanPartnersByCpus.add(selfDescriptor);
                }
                
                if(!tmanPartnersByMem.contains(selfDescriptor)) {
                	tmanPartnersByMem.add(selfDescriptor);
                }
                
                // buffer <- rank(p, buffer)
                Collections.sort(tmanPartnersByRes, new ComparatorByResources(new PeerDescriptor(selectedPeerByRes.getAddress(), selectedPeerByRes.getAv())));
                Collections.sort(tmanPartnersByCpus, new ComparatorByNumCpu(new PeerDescriptor(selectedPeerByCpu.getAddress(), selectedPeerByCpu.getAv())));
                Collections.sort(tmanPartnersByMem, new ComparatorByNumMem(new PeerDescriptor(selectedPeerByMem.getAddress(), selectedPeerByMem.getAv())));

//                System.out.println(self.getId() + " In TMan : tmanPartnersByRes after sort#2 -> " + tmanPartnersByRes);
//                for(PeerDescriptor pd : tmanPartnersByRes) {
//                	System.out.println(pd.getAddress().getId() + " " + pd.getAv().getNumFreeCpus() * pd.getAv().getFreeMemInMbs());
//                }
                
    	        myBufferRes = new DescriptorBuffer(self, tmanPartnersByRes);
    	        myBufferCpu = new DescriptorBuffer(self, tmanPartnersByCpus);
    	        myBufferMem = new DescriptorBuffer(self, tmanPartnersByMem);
    	        
    	        //check null selectedPeerByRes
    	        // send first m entries of buffer to p (here we sent the entire buffer)
    	        trigger(new ExchangeMsg.RequestRes(UUID.randomUUID(), myBufferRes, self, selectedPeerByRes.getAddress()), networkPort);
    	        trigger(new ExchangeMsg.RequestCpu(UUID.randomUUID(), myBufferCpu, self, selectedPeerByCpu.getAddress()), networkPort);
    	        trigger(new ExchangeMsg.RequestMem(UUID.randomUUID(), myBufferMem, self, selectedPeerByMem.getAddress()), networkPort);
            }
            else {
//            	System.out.println("empty sample");
            	return;
            }
            
           
        }
    };
    

    /**
     * The PASSIVE thread receives the buffer from the active thread in this handler (receive bufferq from q)
     * Then it merges itself with its view (buffer <- merge(view, {myDescriptor}))
     * Then it ranks its buffer according to the active's thread Descriptor (buffer <-rank(q, buffer))
     * Then it sends its Descriptor buffer to the active thread (send first m entries of buffer to q)
     * And then it merges its view with the received buffer (view <- merge(bufferq, view))
     */
    Handler<ExchangeMsg.RequestRes> handletmanPartnersRequestRes = new Handler<ExchangeMsg.RequestRes>() {
        @Override
        public void handle(ExchangeMsg.RequestRes event) {
        	LinkedList<PeerDescriptor> receivedView = event.getRandomBuffer().getDescriptors();

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
            

            myBufferRes = new DescriptorBuffer(self, tmanPartnersByRes);
            trigger(new ExchangeMsg.ResponseRes(UUID.randomUUID(), myBufferRes, self, event.getSource()), networkPort);
            tmanPartnersByRes.addAll(receivedView);
            // remove the duplicates after merging my view with the received buffer
            HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByRes);
            tmanPartnersByRes.clear();
            tmanPartnersByRes.addAll(hs);
        }
    };
    
    /**
     * PASSIVE thread : does the same as described above
     */ 
    Handler<ExchangeMsg.RequestMem> handletmanPartnersRequestMem = new Handler<ExchangeMsg.RequestMem>() {
        @Override
        public void handle(ExchangeMsg.RequestMem event) {
        	LinkedList<PeerDescriptor> receivedView = event.getRandomBuffer().getDescriptors();
        	PeerDescriptor from = null;
        	for(PeerDescriptor pd : event.getRandomBuffer().getDescriptors()) {
        		if(pd.getAddress() == event.getSource()) {
        			from = pd;
        			break;
        		}
        	}
        	if(!tmanPartnersByMem.contains(selfDescriptor)) {
        		tmanPartnersByMem.add(selfDescriptor);
        	}

            Collections.sort(tmanPartnersByMem, new ComparatorByNumMem(new PeerDescriptor(event.getSource(),
            						from.getAv())));

            myBufferMem = new DescriptorBuffer(self, tmanPartnersByMem);
            trigger(new ExchangeMsg.ResponseMem(UUID.randomUUID(), myBufferMem, self, event.getSource()), networkPort);
            tmanPartnersByMem.addAll(receivedView);
            // remove the duplicates after merging my view with the received buffer
            HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByMem);
            tmanPartnersByMem.clear();
            tmanPartnersByMem.addAll(hs);
        }
    };
    
    /**
     * PASSIVE thread : does the same as described above
     */ 
    Handler<ExchangeMsg.RequestCpu> handletmanPartnersRequestCpu = new Handler<ExchangeMsg.RequestCpu>() {
        @Override
        public void handle(ExchangeMsg.RequestCpu event) {
        	LinkedList<PeerDescriptor> receivedView = event.getRandomBuffer().getDescriptors();
        	PeerDescriptor from = null;
        	for(PeerDescriptor pd : event.getRandomBuffer().getDescriptors()) {
        		if(pd.getAddress() == event.getSource()) {
        			from = pd;
        			break;
        		}
        	}
        	if(!tmanPartnersByCpus.contains(selfDescriptor)) {
        		tmanPartnersByCpus.add(selfDescriptor);
        	}

            Collections.sort(tmanPartnersByCpus, new ComparatorByNumCpu(new PeerDescriptor(event.getSource(),
            						from.getAv())));

            myBufferCpu = new DescriptorBuffer(self, tmanPartnersByCpus);
            trigger(new ExchangeMsg.ResponseCpu(UUID.randomUUID(), myBufferCpu, self, event.getSource()), networkPort);
            tmanPartnersByCpus.addAll(receivedView);
            // remove the duplicates after merging my view with the received buffer
            HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByCpus);
            tmanPartnersByCpus.clear();
            tmanPartnersByCpus.addAll(hs);
        }
    };

    
    /** The ACTIVE thread receives the bufferp from p (receive bufferp from p)
     * and then merges its view with the received buffer (view <- merge(bufferp, view)
     * 
     */
    Handler<ExchangeMsg.ResponseRes> handletmanPartnersResponseRes = new Handler<ExchangeMsg.ResponseRes>() {
        @Override
        public void handle(ExchangeMsg.ResponseRes event) {
        	LinkedList<PeerDescriptor> receivedView = event.getSelectedBuffer().getDescriptors();

            tmanPartnersByRes.addAll(receivedView);
            // remove the duplicates after merging my view with the received buffer
        	HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByRes);
            tmanPartnersByRes.clear();
            tmanPartnersByRes.addAll(hs);
        }
    };
    
    /**
     * ACTIVE thread : does the same as described above
     */
    Handler<ExchangeMsg.ResponseCpu> handletmanPartnersResponseCpu = new Handler<ExchangeMsg.ResponseCpu>() {
        @Override
        public void handle(ExchangeMsg.ResponseCpu event) {
        	LinkedList<PeerDescriptor> receivedView = event.getSelectedBuffer().getDescriptors();
            tmanPartnersByCpus.addAll(receivedView);
            // remove the duplicates after merging my view with the received buffer
        	HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByCpus);
            tmanPartnersByCpus.clear();
            tmanPartnersByCpus.addAll(hs);
        }
    };
    
    /**
     * ACTIVE thread : does the same as described above
     */
    Handler<ExchangeMsg.ResponseMem> handletmanPartnersResponseMem = new Handler<ExchangeMsg.ResponseMem>() {
        @Override
        public void handle(ExchangeMsg.ResponseMem event) {
        	LinkedList<PeerDescriptor> receivedView = event.getSelectedBuffer().getDescriptors();
            tmanPartnersByMem.addAll(receivedView);
            // remove the duplicates after merging my view with the received buffer
        	HashSet<PeerDescriptor> hs = new HashSet<PeerDescriptor>();
            hs.addAll(tmanPartnersByMem);
            tmanPartnersByMem.clear();
            tmanPartnersByMem.addAll(hs);
        }
    };
    
    public void PrintMsg(String msg) {
    	logger.info("Peer " + self.getId() + " :" + msg);
    }
    
    
    /**
     * @param psi
     * @param view
     * @return PeerDescriptor
     * Selects the peer with who I will exchange views ( p <- selectedPeer(psi, rank(myDescriptor, view))) 
     */
    public PeerDescriptor selectPeer(int psi, LinkedList<PeerDescriptor> view) {
    	if(view.size() == 0) {
    		return null;
    	}
    	else if(view.size() == 1) {
    		return view.get(0);
    	}
    	else if(psi==0){
    		return view.get(0);
    	}
    	else if(psi>view.size()){
    		return view.get(r.nextInt(view.size()));
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
