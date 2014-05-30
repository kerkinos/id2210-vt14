package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{
                
		StochasticProcess startPeers = new StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			raise(100, Operations.peerJoin(), 
                                uniform(1, Integer.MAX_VALUE), 
                                constant(8), constant(12000)
                             );
		}};
        
		// these requests will be served by cyclonPartners using probing
		StochasticProcess requestResources1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(2500, Operations.requestResources(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2000),
                                constant(1000*60*1) // 1 minute
                                );
		}};
		
		// these requests will be served by tmanPartners
		StochasticProcess requestResources2 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(2500, Operations.requestResources(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2000),
                                constant(1000*60*1) // 1 minute
                                );
		}};
		
		// here we raise batchRequests asking two machines. They will be served cyclonPartners (after probing)
		StochasticProcess requestBatchResources1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1250, Operations.allocateResourcesManyMachines(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2000), constant(2),
                                constant(1000*60*1) // 1 minute
                                );
		}};
		
		// here we raise batchRequests asking two machines. They will be served by tmanPartners
		StochasticProcess requestBatchResources2 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1250, Operations.allocateResourcesManyMachines(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2000), constant(2),
                                constant(1000*60*1) // 1 minute
                                );
		}};
                
                // TODO - not used yet
		StochasticProcess failPeersProcess = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.peerFail, 
                                uniform(0, Integer.MAX_VALUE));
		}};
                
		StochasticProcess generateStats1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		
		StochasticProcess generateStats2 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		
		StochasticProcess generateStats3 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		
		StochasticProcess generateStats4 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		
		StochasticProcess terminateScenario = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminatePeers);
		}};
		
		
		startPeers.start();
		requestResources1.startAfterTerminationOf(2000, startPeers);
		generateStats1.startAfterTerminationOf(1000*100, requestResources1);
		requestResources2.startAfterTerminationOf(1000*100, generateStats1);
		generateStats2.startAfterTerminationOf(1000*100, requestResources2);
		//terminateScenario.startAfterTerminationOf(1000*100, generateStats2);
		requestBatchResources1.startAfterTerminationOf(1000*1000, generateStats2);
		generateStats3.startAfterTerminationOf(1000*100, requestBatchResources1);
		requestBatchResources2.startAfterTerminationOf(1000*1000, generateStats3);
		generateStats4.startAfterTerminationOf(1000*100, requestBatchResources2);
		terminateScenario.startAfterTerminationOf(1000*100, generateStats4);
        
                
	}};

	// -------------------------------------------------------------------
	public Scenario1() {
		super(scenario);
	}
}
