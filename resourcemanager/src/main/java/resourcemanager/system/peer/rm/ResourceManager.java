package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 *
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    Positive<RmPort> indexPort = requires(RmPort.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<Timer> timerPort = requires(Timer.class);
    Negative<Web> webPort = provides(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = requires(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = requires(TManSamplePort.class);
    ArrayList<Address> neighbours = new ArrayList<Address>();
    private Address self;
    private RmConfiguration configuration;
    Random random;
    private AvailableResources availableResources;
    
    // requestsQueue where we put incoming requests for resources
    private ArrayList<RequestResources.Request> requestsQueue;
    
    Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
        @Override
        public int compare(PeerDescriptor t, PeerDescriptor t1) {
            if (t.getAge() > t1.getAge()) {
                return 1;
            } else {
                return -1;
            }
        }
    };

	
    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
        subscribe(handleUpdateTimeout, timerPort);
        subscribe(handleResourceAllocationRequest, networkPort);
        subscribe(handleResourceAllocationResponse, networkPort);
        subscribe(handleTManSample, tmanPort);
    }
	
    Handler<RmInit> handleInit = new Handler<RmInit>() {
        @Override
        public void handle(RmInit init) {
            self = init.getSelf();
            configuration = init.getConfiguration();
            random = new Random(init.getConfiguration().getSeed());
            availableResources = init.getAvailableResources();
            long period = configuration.getPeriod();
            availableResources = init.getAvailableResources();
            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateTimeout(rst));
            
            requestsQueue = new ArrayList<RequestResources.Request>();
            		
            trigger(rst, timerPort);


        }
    };


    Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            // pick a random neighbour to ask for index updates from. 
            // You can change this policy if you want to.
            // Maybe a gradient neighbour who is closer to the leader?
            if (neighbours.isEmpty()) {
                return;
            }
            Address dest = neighbours.get(random.nextInt(neighbours.size()));


        }
    };


    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {
            // TODO
        	System.out.println(self.getId() + " : Received Request for " + event.getNumCpus() + " cpus and " + event.getAmountMemInMb() 
        			+ " memInMb" + " from peer with id " + event.getSource().getId());
        	System.out.println("I have " + availableResources.getNumFreeCpus() + " cpus and " + availableResources.getFreeMemInMbs());
        	
        	
        	//if the resources are available send a response event
        	boolean success = availableResources.isAvailable(event.getNumCpus(), event.getAmountMemInMb());
        	if( success ) {
        		availableResources.allocate(event.getNumCpus(), event.getAmountMemInMb());
        		RequestResources.Response response = new RequestResources.Response(self, event.getSource(), success);
        		trigger(response, networkPort);
        	}
        	//otherwise put the request in the queue
        	else {
        		requestsQueue.add(event);
        	}
        }
    };
    
    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {
            // TODO 
        }
    };
    
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            System.out.println("id " + self.getId() + " received samples: " + event.getSample().size());
            
            
            // receive a new list of neighbours
            neighbours.clear();
            neighbours.addAll(event.getSample());
            
            if(event.getSample().size() > 0) {
	            System.out.print("My neigbours are [ ");
	            for(Address peer : neighbours){
	            	System.out.print(peer.getId() + " ");
	            }
	            System.out.print("]\n\n");
            }

        }
    };
	
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {
            
            //System.out.println("Allocate resources: " + event.getId() + " " + event.getNumCpus() + " + " + event.getMemoryInMbs());
            // TODO: Ask for resources from neighbours
            // by sending a ResourceRequest
        	
        	
        	System.out.println(self.getId() + " Request resource id: " + event.getId());
        	//currently the probe number is equal to the size of the neighbours
            for (Address dest : neighbours) {
            	RequestResources.Request req = new RequestResources.Request(self, dest,
                        event.getNumCpus(), event.getMemoryInMbs());
                        trigger(req, networkPort);
            }
            
        }
    };
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            // TODO: 
        }
    };

}