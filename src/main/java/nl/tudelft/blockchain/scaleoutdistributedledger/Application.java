package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Class to run a node.
 */
public class Application {

	public static final String NODE_ADDRESS = "localhost";
	public static final int NODE_PORT = 8007;
	
	private LocalStore localStore;

	/**
	 * Creates a new application.
	 * @throws IOException - error while registering nodes
	 */
	public Application() throws IOException {
		this.setupNode();
	}

	/**
	 * Called when we receive a new transaction.
	 * @param transaction - the transaction
	 * @param proof       - the proof
	 */
	public synchronized void receiveTransaction(Transaction transaction, Proof proof) {
		if (CommunicationHelper.receiveTransaction(localStore.getVerification(), transaction, proof)) {
			if (transaction.getAmount() > 0) {
				localStore.getUnspent().add(transaction);
			}
		}
	}
	
	/**
	 * Send a transaction to the receiver of the transaction.
	 * An abstract of the block containing the transaction (or a block after it) must already be
	 * committed to the main chain.
	 * @param transaction - the transaction to send
	 */
	public void sendTransaction(Transaction transaction) {
		CommunicationHelper.sendTransaction(transaction);
	}
	
	/**
	 * Setup your own node.
	 * Register to the tracker and setup the local store.
	 * @throws java.io.IOException - error while registering node
	 */
	private void setupNode() throws IOException {
		// Create and register node
		RSAKey key = new RSAKey();
		Node ownNode = TrackerHelper.registerNode(key.getPublicKey(), "localhost", 80);
		ownNode.setPrivateKey(key.getPrivateKey());
		
		// Setup local store
		localStore = new LocalStore(ownNode);
	}
}
