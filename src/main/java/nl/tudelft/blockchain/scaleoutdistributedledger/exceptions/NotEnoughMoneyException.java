package nl.tudelft.blockchain.scaleoutdistributedledger.exceptions;

/**
 * Exception for indicating that the user doesn't have enough money.
 */
public class NotEnoughMoneyException extends RuntimeException {
	
	private static final long serialVersionUID = -5115214404574584898L;
	
	/**
	 * Exception without message.
	 */
	public NotEnoughMoneyException() {
		super();
	}
	
}
