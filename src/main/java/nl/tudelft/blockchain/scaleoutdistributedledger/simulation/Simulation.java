package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.TrackerHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StartTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StopTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionPatternMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Class for simulations.
 */
public class Simulation {
	@Getter
	private ITransactionPattern transactionPattern;
	
	@Getter
	private SimulationState state;
	
	@Getter
	private Map<Integer, Node> nodes;
	
	private Application[] localApplications;
	private final SocketClient socketClient;
	
	/**
	 * Creates a new simulation.
	 */
	public Simulation() {
		this.socketClient = new SocketClient();
		this.state = SimulationState.STOPPED;
	}
	
	/**
	 * @param pattern - the new transaction pattern
	 * @throws NullPointerException - if pattern is null.
	 */
	public void setTransactionPattern(ITransactionPattern pattern) {
		if (pattern == null) throw new NullPointerException("Pattern must not be null!");
		this.transactionPattern = pattern;
	}
	
	/**
	 * Runs the given amount of nodes locally, in the current JVM process.
	 * @param amount - the number of nodes to run locally
	 * @throws IllegalStateException - if the state is not STOPPED.
	 */
	public void runNodesLocally(int amount) {
		checkState(SimulationState.STOPPED, "start local nodes");
		
		localApplications = new Application[amount];
		for (int i = 0; i < amount; i++) {
			int basePort = Application.NODE_PORT + i * 4;
			Application app = new Application(true);
			try {
				app.init(basePort, basePort + 3);
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Unable to initialize local node " + i + " on port " + basePort + "!", ex);
			}
			localApplications[i] = app;
		}
	}
	
	/**
	 * Stops all nodes that are running locally.
	 * @throws IllegalStateException - if the state is not STOPPED.
	 */
	public void stopLocalNodes() {
		checkState(SimulationState.STOPPED, "stop local nodes");
		if (localApplications == null) return;
		
		for (Application app : localApplications) {
			app.stop();
		}
	}
	
	/**
	 * Initializes the simulation.
	 * 
	 * This method first gets the node list from the tracker and then sends the transaction pattern
	 * to all nodes.
	 * @throws IllegalStateException - if the state is not STOPPED.
	 * @throws NullPointerException  - if the transaction pattern has not been set.
	 */
	public void initialize() {
		checkState(SimulationState.STOPPED, "initialize");
		if (transactionPattern == null) throw new NullPointerException("TransactionPattern must not be null!");
		
		getNodeListFromTracker();
		if (nodes.isEmpty()) {
			Log.log(Level.INFO, "[Simulation] No nodes found. Stopping simulation.");
			return;
		} else {
			Log.log(Level.INFO, "[Simulation] Tracker reported " + nodes.size() + " nodes.");
		}
		
		//Broadcast distributed transaction pattern
		if (transactionPattern.getSimulationMode() == SimulationMode.DISTRIBUTED) {
			broadcastMessage(new TransactionPatternMessage(transactionPattern));
		}
		
		Log.log(Level.INFO, "[Simulation] Initialized");
		state = SimulationState.INITIALIZED;
	}
	
	/**
	 * Starts the simulation.
	 * 
	 * This method sends a "Start transacting" message to all nodes.
	 * @throws IllegalStateException - if the state is not INITIALIZED.
	 */
	public void start() {
		checkState(SimulationState.INITIALIZED, "start");
		
		if (transactionPattern.getSimulationMode() == SimulationMode.DISTRIBUTED) {
			broadcastMessage(new StartTransactingMessage());
		} else if (transactionPattern.getSimulationMode() == SimulationMode.DIRECTED) {
			Log.log(Level.INFO, "[Simulation] Starting directed simulation...");
			//TODO Directed simulation
		}
		
		Log.log(Level.INFO, "[Simulation] Running");
		state = SimulationState.RUNNING;
	}
	
	/**
	 * Stops the simulation.
	 * @throws IllegalStateException - if the state is not RUNNING.
	 */
	public void stop() {
		checkState(SimulationState.RUNNING, "stop");
		
		broadcastMessage(new StopTransactingMessage());
		Log.log(Level.INFO, "[Simulation] Stopped");
		state = SimulationState.STOPPED;
	}
	
	/**
	 * Cleans up the simulation.
	 * @throws IllegalStateException - if the state is RUNNING.
	 */
	public void cleanup() {
		if (state == SimulationState.RUNNING) throw new IllegalStateException("Cannot cleanup while still running!");
		
		this.nodes = null;
		state = SimulationState.STOPPED;
	}
	
	/**
	 * Checks if the current state is the expected state.
	 * @param expected - the expected state
	 * @param msg      - the message
	 * @throws IllegalStateException - If the current state is not the expected state.
	 */
	protected void checkState(SimulationState expected, String msg) {
		if (state != expected) {
			throw new IllegalStateException("You can only " + msg + " when the simulation is in the " + expected.name() + " state.");
		}
	}
	
	/**
	 * Gets the node list from the tracker.
	 */
	protected void getNodeListFromTracker() {
		nodes = new HashMap<>();
		try {
			TrackerHelper.updateNodes(nodes);
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "[Simulation] Unable to get list of nodes from tracker!", ex);
		}
	}
	
	/**
	 * Sends the given message to all nodes.
	 * @param msg - the message to send
	 */
	protected void broadcastMessage(Message msg) {
		Log.log(Level.INFO, "[Simulation] Sending " + msg + " to all nodes...");
		
		for (Node node : nodes.values()) {
			try {
				if (!socketClient.sendMessage(node, msg)) {
					Log.log(Level.SEVERE,
							"Failed to send message " + msg + " to node " + node.getId() +
							" at " + node.getAddress() + ":" + node.getPort());
				}
			} catch (Exception ex) {
				Log.log(Level.SEVERE,
						"[Simulation] Failed to send message " + msg + " to node " + node.getId() +
						" at " + node.getAddress() + ":" + node.getPort(), ex);
			}
		}
	}
}
