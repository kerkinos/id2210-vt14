package resourcemanager.system.peer.rm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.ComparatorByNumCpu;
import tman.system.peer.tman.ComparatorByNumMem;
import tman.system.peer.tman.ComparatorByResources;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

/**
 * Should have some comments here.
 * 
 */
public final class ResourceManager extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceManager.class);
	Positive<RmPort> indexPort = requires(RmPort.class);
	Positive<Network> networkPort = requires(Network.class);
	Positive<Timer> timerPort = requires(Timer.class);
	Negative<Web> webPort = provides(Web.class);
	Positive<CyclonSamplePort> cyclonSamplePort = requires(CyclonSamplePort.class);
	Positive<TManSamplePort> tmanPort = requires(TManSamplePort.class);
	LinkedList<PeerDescriptor> cyclonPartners = new LinkedList<PeerDescriptor>();
	LinkedList<PeerDescriptor> tmanPartners = new LinkedList<PeerDescriptor>();
	LinkedList<PeerDescriptor> tmanPartnersByRes = new LinkedList<PeerDescriptor>();
	LinkedList<PeerDescriptor> tmanPartnersByCpu = new LinkedList<PeerDescriptor>();
	LinkedList<PeerDescriptor> tmanPartnersByMem = new LinkedList<PeerDescriptor>();
	
	int requestedNumCpus;
	int requestedNumMem;
	
	private Address self;
	private RmConfiguration configuration;
	private Random random;
	private AvailableResources availableResources;
	private static final int MAX_NUM_NODES = 4;
	
	//true = cyclon
	public static boolean flag = false;

	// queue where we put incoming requests for resources

	private Queue<Allocate> queue = new LinkedList<Allocate>();
	private Map<Long, RequestResources> requestResourcesMap = new HashMap<Long, RequestResources>();
	private static Map<Long, Long> timePerRequest = new HashMap<Long, Long>();

	Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
		@Override
		public int compare(PeerDescriptor t, PeerDescriptor t1) {
			if (t.getAge() > t1.getAge()) {
				return 1;
			} 
			else {
				return -1;
			}
		}
	};

	public ResourceManager() {

		subscribe(handleInit, control);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleRequestResource, indexPort);
		subscribe(handleUpdateTimeout, timerPort);
		subscribe(handleJobDone, timerPort);
		subscribe(handleResourceAllocationRequest, networkPort);
		subscribe(handleResourceAllocationResponse, networkPort);
		subscribe(handleAllocateResources, networkPort);
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
			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new UpdateTimeout(rst));

			trigger(rst, timerPort);

		}
	};

	Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
		@Override
		public void handle(UpdateTimeout event) {

			// pick a random neighbour to ask for index updates from.
			// You can change this policy if you want to.
			// Maybe a gradient neighbour who is closer to the leader?
			
			//System.out.println(averageTime);

			if (cyclonPartners.isEmpty()) {
				return;
			}
			PeerDescriptor dest = cyclonPartners.get(random
					.nextInt(cyclonPartners.size()));

		}
	};

	Handler<Request> handleResourceAllocationRequest = new Handler<Request>() {
		@Override
		public void handle(Request event) {
			// System.out.println(self.getId() + " : Received Request for " +
			// event.getNumCpus() + " cpus and " + event.getAmountMemInMb()
			// + " memInMb" + " from peer with id " +
			// event.getSource().getId());
			// System.out.println("I have " +
			// availableResources.getNumFreeCpus() + " cpus and " +
			// availableResources.getFreeMemInMbs());

			// send a response event with a boolean value
			boolean success = availableResources.isAvailable(
					event.getNumCpus(), event.getAmountMemInMb());
			Response response = new Response(self, event.getSource(), success,
					queue.size(), event.getReqid(), event.getStartTime());
			trigger(response, networkPort);

		}
	};

	Handler<Response> handleResourceAllocationResponse = new Handler<Response>() {
		@Override
		public void handle(Response event) {
			// System.out.println(self + " Got response from " +
			// event.getSource().getId());
			//System.out.println("Got allocate event for requestId " + event.getReqid());
			RequestResources rr = requestResourcesMap.get(event.getReqid());
			Response best = rr.findBestResponse(event);
			rr.pendingResponses --;
			requestResourcesMap.put(event.getReqid(), rr);
			if (rr.pendingResponses == 0) {
				//System.out.println("HERE");
				Allocate al = new Allocate(self, best.getSource(),rr.getNumCpus(), rr.getAmountMem(), rr.getTime(), event.getReqid(),event.getStartTime());
				trigger(al, networkPort);
			}
		}
	};
	
	static long getAverageTime() {
		long sum = 0;
		for(Long l : timePerRequest.values()) {
			sum += l;
		}
		if(timePerRequest.size()==0){
			return 0;
		}
		else return sum / timePerRequest.size();
	}

	Handler<Allocate> handleAllocateResources = new Handler<Allocate>() {

		@Override
		public void handle(Allocate event) {
			boolean success = availableResources.isAvailable(event.getNumCpus(),event.getAmountMem());
			if (!success) {
				if(!queue.contains(event)) {
					queue.add(event);
				}		
			} 
			else {
				long timeToFindResources = System.currentTimeMillis() - event.getStartTime();
				availableResources.allocate(event.getNumCpus(),event.getAmountMem());				
				if(!queue.isEmpty()) {
					queue.remove();
				}
				
				ScheduleTimeout st = new ScheduleTimeout(event.getTime());
				st.setTimeoutEvent(new JobDone(st, event.getNumCpus(), event.getAmountMem()));
				trigger(st, timerPort);
				//save stats to a file
				try {
				    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("stats.txt", true)));
				    out.println(timeToFindResources);
				    out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	Handler<JobDone> handleJobDone = new Handler<JobDone>() {

		@Override
		public void handle(JobDone arg0) {
			
			availableResources.release(arg0.getNumCpus(), arg0.getAmountMem());
			if (!queue.isEmpty()) {
				Allocate al = queue.peek();
				trigger(al, networkPort);
				}
			}
	};

	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {

			// receive a new list of cyclonPartners
			cyclonPartners.clear();
			cyclonPartners.addAll(event.getSample());

		}
	};

	Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
		@Override
		public void handle(RequestResource event) {
			// TODO: Ask for resources from cyclonPartners
			// by sending a ResourceRequest
			
			event.setStartTime(System.currentTimeMillis());
			
			requestedNumCpus = event.getNumCpus();
			requestedNumMem = event.getMemoryInMbs();
			
			if( (event.getMemoryInMbs() * event.getNumCpus()) != 0 ) {
				tmanPartners = tmanPartnersByRes;
			}
			else if(event.getMemoryInMbs() == 0) {
				tmanPartners = tmanPartnersByCpu;
			}
			else if(event.getNumCpus() == 0) {
				tmanPartners = tmanPartnersByMem;
			}

			if (flag) {
				takeCyclonSample(event);
			}
			else {
				takeTmanSample(event);
			}
			
		}
	};
	
	void takeCyclonSample(RequestResource event) {
		if (cyclonPartners.size() <= MAX_NUM_NODES) {
			requestResourcesMap.put(event.getId(), new RequestResources(event.getNumCpus(), event
													.getMemoryInMbs(), event
													.getTimeToHoldResource(),
													cyclonPartners.size()));
			for (PeerDescriptor dest : cyclonPartners) {
				Request req = new Request(self, dest.getAddress(),
						event.getNumCpus(), event.getMemoryInMbs(),
						event.getId(), event.getStartTime());
				trigger(req, networkPort);
			}
		} 
		else {
			requestResourcesMap.put(event.getId(), new RequestResources(event.getNumCpus(), event
													.getMemoryInMbs(), event
													.getTimeToHoldResource(), MAX_NUM_NODES));
			for (int i = 0; i < MAX_NUM_NODES; i++) {
				int index = random.nextInt(cyclonPartners.size());
				PeerDescriptor dest = cyclonPartners.get(index);
				cyclonPartners.remove(index);
				Request req = new Request(self, dest.getAddress(),
						event.getNumCpus(), event.getMemoryInMbs(),
						event.getId(), event.getStartTime());
				trigger(req, networkPort);
			}

		}
	}
	
	void takeTmanSample(RequestResource event){
		if (tmanPartners.size() <= MAX_NUM_NODES) {
			requestResourcesMap.put(event.getId(), new RequestResources(event.getNumCpus(), event
													.getMemoryInMbs(), event
													.getTimeToHoldResource(),
													tmanPartners.size()));
			for (PeerDescriptor dest : tmanPartners) {
				Request req = new Request(self, dest.getAddress(),
						event.getNumCpus(), event.getMemoryInMbs(),
						event.getId(), event.getStartTime());
				trigger(req, networkPort);
			}
		} 
		else {
			requestResourcesMap.put(event.getId(), new RequestResources(event.getNumCpus(), event
													.getMemoryInMbs(), event
													.getTimeToHoldResource(), MAX_NUM_NODES));
			for (int i = 0; i < MAX_NUM_NODES; i++) {
//				int index = random.nextInt(tmanPartners.size());
				int index = 0;
				PeerDescriptor dest = tmanPartners.get(index);
				tmanPartners.remove(index);
				Request req = new Request(self, dest.getAddress(),
						event.getNumCpus(), event.getMemoryInMbs(),
						event.getId(), event.getStartTime());
				trigger(req, networkPort);
			}
		}
	}
	
	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
			
			tmanPartnersByRes.clear();
			tmanPartnersByCpu.clear();
			tmanPartnersByMem.clear();
			tmanPartnersByRes.addAll(event.getPartnersByRes());
			tmanPartnersByCpu.addAll(event.getPartnersByCpu());
			tmanPartnersByMem.addAll(event.getPartnersByMem());
            Collections.sort(tmanPartnersByRes, new ComparatorByResources(new PeerDescriptor(self, availableResources)));
            Collections.sort(tmanPartnersByCpu, new ComparatorByNumCpu(new PeerDescriptor(self, availableResources)));
            Collections.sort(tmanPartnersByMem, new ComparatorByNumMem(new PeerDescriptor(self, availableResources)));
//        	for(PeerDescriptor pd : tmanPartnersByRes) {
//            	System.out.println( "from " + self.getId() + " " + pd.getAv().getNumFreeCpus() + " RESM " + pd.getAv().getFreeMemInMbs());
//            }
		}
	};

}
