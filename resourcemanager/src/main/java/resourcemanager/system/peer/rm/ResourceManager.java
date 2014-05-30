package resourcemanager.system.peer.rm;


import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.AllocateResourcesManyMachines;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.LinkedList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import simulator.snapshot.Snapshot;
import system.peer.RmPort;
import tman.system.peer.tman.ComparatorByNumCpu;
import tman.system.peer.tman.ComparatorByNumMem;
import tman.system.peer.tman.ComparatorByResources;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;


/**
 * The ResourceManager is the component that receives the requests for resources.
 * In our system every node is a Resource Manager and manages its own set of Cpus and Memory.
 * 
 */
public final class ResourceManager extends ComponentDefinition {

	public static boolean isFlag() {
		return flag;
	}

	public static void setFlag(boolean flag) {
		ResourceManager.flag = flag;
	}

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
	static int requestedNumMachines = 1;
	
	private Address self;
	private RmConfiguration configuration;
	private Random random;
	private AvailableResources availableResources;
	private static final int MAX_NUM_NODES = 4;
	PeerDescriptor selfDescriptor;
	
	//true = cyclon
	public static boolean flag = true;

	// queue where we put incoming requests for resources in case we dont have available resources
	private Queue<Allocate> queue = new LinkedList<Allocate>();
	// Map where we put request_id as a key and RequestResources object as a value
	private Map<Long, RequestResources> requestResourcesMap = new HashMap<Long, RequestResources>();

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
		subscribe(handleBatchRequest, indexPort);
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
			selfDescriptor = new PeerDescriptor(self, availableResources);
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

	/**
	 * Triggered when a peer receives a request. He answers back with a boolean value and its queue size.
	 */
	Handler<Request> handleResourceAllocationRequest = new Handler<Request>() {
		@Override
		public void handle(Request event) {

			// send a response event with a boolean value
			boolean success = availableResources.isAvailable(
					event.getNumCpus(), event.getAmountMemInMb());
			Response response = new Response(self, event.getSource(), success,
					queue.size(), event.getReqid(), event.getStartTime());
			trigger(response, networkPort);

		}
	};

	/**
	 * Triggered when we receive a response from a peer.
	 * we wait until we receive responses from all the peers we have sent a request for resources
	 * and then we choose the best peer among them to assign the request
	 */
	Handler<Response> handleResourceAllocationResponse = new Handler<Response>() {
		@Override
		public void handle(Response event) {
			// System.out.println(self + " Got response from " +
			// event.getSource().getId());
			RequestResources rr = requestResourcesMap.get(event.getReqid());
			//Response best = rr.findBestResponse(event);
			rr.collectResponses(event);
			rr.pendingResponses --;
			requestResourcesMap.put(event.getReqid(), rr);
			if (rr.pendingResponses == 0) {
				rr.sortResponses();
				for (int i = 0 ; i < requestedNumMachines; i++) {
					Allocate al = new Allocate(self, rr.getResponses().get(i).getSource(),
									rr.getNumCpus(), rr.getAmountMem(), rr.getTime(), event.getReqid(),event.getStartTime());
					trigger(al, networkPort);
				}
			}
		}
	};

	/**
	 * Triggered when a peer receives an allocation request for resources.
	 * If it has the available resources it allocates them
	 * otherwise it puts the request in a queue.
	 */
	Handler<Allocate> handleAllocateResources = new Handler<Allocate>() {

		@Override
		public void handle(Allocate event) {
			boolean success = availableResources.isAvailable(event.getNumCpus(),event.getAmountMem());
			if (!success) {
				if(!queue.contains(event)) {
					queue.add(event);
					selfDescriptor.setQueueSize(queue.size());
					availableResources.setQueueSize(queue.size());
				}		
			} 
			else {
				long timeToFindResources = System.currentTimeMillis() - event.getStartTime();
				// for every request_id, we hold the time we did to find available resources for it in a map
				Snapshot.addTime(event.getReqid(), timeToFindResources);
				
				availableResources.allocate(event.getNumCpus(),event.getAmountMem());	
				selfDescriptor.setAv(availableResources);
				if(!queue.isEmpty()) {
					queue.remove();
					selfDescriptor.setQueueSize(queue.size());
					availableResources.setQueueSize(queue.size());
				}
				
				ScheduleTimeout st = new ScheduleTimeout(event.getTime());
				st.setTimeoutEvent(new JobDone(st, event.getNumCpus(), event.getAmountMem()));
				trigger(st, timerPort);
				
			}
		}
	};

	/**
	 * Triggered when the job has finished and we are ready to release the Resources we allocated for that job
	 * If we have outstanding jobs in the queue we serve the next one
	 */
	Handler<JobDone> handleJobDone = new Handler<JobDone>() {

		@Override
		public void handle(JobDone arg0) {
			
			availableResources.release(arg0.getNumCpus(), arg0.getAmountMem());
			selfDescriptor.setAv(availableResources);
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
	
	/**
	 * This handler is triggered when there is a request for a batch request.
	 * 
	 */
	Handler<AllocateResourcesManyMachines> handleBatchRequest = new Handler<AllocateResourcesManyMachines>() {
		
		@Override
		public void handle(AllocateResourcesManyMachines event) {
			long startTime = System.currentTimeMillis();
			
			event.setStartTime(startTime);
			setRequestedNumMachines(event.getNumMachines());
			
			// we check if this request is only for cpu, memory, or both
			// and then we choose our tmanParters respectively
			if( (event.getMemoryInMbs() * event.getNumCpus()) != 0 ) {
				tmanPartners = tmanPartnersByRes;
			}
			else if(event.getMemoryInMbs() == 0) {
				tmanPartners = tmanPartnersByCpu;
			}
			else if(event.getNumCpus() == 0) {
				tmanPartners = tmanPartnersByMem;
			}

			if(flag) {
				if(cyclonPartners.size() >= event.getNumMachines() && cyclonPartners.size() <= MAX_NUM_NODES) {
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
				else if(cyclonPartners.size() >= event.getNumMachines() && cyclonPartners.size() > MAX_NUM_NODES){
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
				else {
					return;
				}
			}
			else {
				if (tmanPartners.size() >= event.getNumMachines()) {
					for (int i = 0 ; i < requestedNumMachines; i++) {
						int index = random.nextInt(tmanPartners.size()/2);
						PeerDescriptor dest = tmanPartners.get(index);
						Allocate al = new Allocate(self, dest.getAddress(), event.getNumCpus(), 
								event.getMemoryInMbs(), event.getTimeToHoldResource(), event.getId(),event.getStartTime());
						trigger(al, networkPort);
						tmanPartners.remove(index);
					}
				}
			}
		}
	};

	Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
		@Override
		public void handle(RequestResource event) {
			
			event.setStartTime(System.currentTimeMillis());
			
			// we check if this request is only for cpu, memory, or both
			// and then we choose our tmanParters respectively
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
		if (cyclonPartners.size() <= MAX_NUM_NODES && cyclonPartners.size()>0) {
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
		else if (cyclonPartners.size() > MAX_NUM_NODES && cyclonPartners.size()>0) {
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
		if (tmanPartners.size() != 0) {
//			Collections.sort(tmanPartners, new ComparatorQueueSizeRM());
			int index = random.nextInt(tmanPartners.size()/2);
//			int index = 0;
			PeerDescriptor dest = tmanPartners.get(index);
			Allocate al = new Allocate(self, dest.getAddress(), event.getNumCpus(),
							event.getMemoryInMbs(), event.getTimeToHoldResource(), event.getId(), event.getStartTime());
			trigger(al, networkPort);
//			tmanPartners.remove(index);
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
		}
	};

	public static int getRequestedNumMachines() {
		return requestedNumMachines;
	}

	public void setRequestedNumMachines(int requestedNumMachines) {
		this.requestedNumMachines = requestedNumMachines;
	}

}
