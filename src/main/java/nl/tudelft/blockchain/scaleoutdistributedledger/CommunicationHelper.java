package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ValidationException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Helper class for communication.
 */
public final class CommunicationHelper {
	private CommunicationHelper() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param proof         - the proof provided with the transaction
	 * @param localStore	- the localstore of the node
	 * @return               true if the transaction was accepted, false otherwise
	 */
	public static boolean receiveTransaction(Proof proof, LocalStore localStore) {
		Log.log(Level.FINE, "Received transaction: " + proof.getTransaction());
		
		if (proof.getTransaction().getReceiver().getId() != localStore.getOwnNode().getId()) {
			Log.log(Level.WARNING, "Received a transaction that isn't for us: " + proof.getTransaction());
			return false;
		}
		
		try {
			localStore.getVerification().validateNewMessage(proof, localStore);
		} catch (ValidationException ex) {
			Log.log(Level.WARNING, "Received an invalid transaction/proof " + proof.getTransaction() + ": " + ex.getMessage());
			return false;
		}

		Log.log(Level.INFO, "Received and validated transaction: " + proof.getTransaction());
		Log.log(Level.FINE, "Transaction " + proof.getTransaction() + " is valid, applying updates...");
		proof.applyUpdates(localStore);
		try {
			TrackerHelper.registerTransaction(proof);
		} catch (IOException e) {
			Log.log(Level.WARNING, "Transaction registration failed", e);
		}

		if (proof.getTransaction().getAmount() > 0) {
			localStore.addUnspentTransaction(proof.getTransaction());
		}

		return true;
	}
}
