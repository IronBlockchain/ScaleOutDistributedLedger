package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Block message for netty.
 */
public class BlockMessage {
	
	@Getter
	private final int number;

	@Getter
	private final int previousBlockNumber;
	
	@Getter
	private final BlockMessage previousBlock;
	
	@Getter
	private final int ownerId;

	@Getter
	private final List<TransactionMessage> transactions;

	@Getter
	private final Sha256Hash hash;

	//private transient BlockAbstract blockAbstract;
	//private transient Boolean hasAbstract;
	
	/**
	 * Constructor.
	 * @param block - original block
	 * @param usePreviousBlockNumber - check whether to use or not the previous block number
	 */
	public BlockMessage(Block block, boolean usePreviousBlockNumber) {
		this.number = block.getNumber();
		this.previousBlockNumber = block.getPreviousBlock().getNumber();
		if (usePreviousBlockNumber) {
			this.previousBlock = null;
		} else {
			this.previousBlock = new BlockMessage(block.getPreviousBlock());
		}
		this.ownerId = block.getOwner().getId();
		this.transactions = new ArrayList<>();
		for (Transaction transaction : block.getTransactions()) {
			this.transactions.add(new TransactionMessage(transaction));
		}
		this.hash = block.getHash();
	}
	
	/**
	 * Constructor.
	 * @param block - original block
	 */
	public BlockMessage(Block block) {
		this(block, false);
	}
	
}
