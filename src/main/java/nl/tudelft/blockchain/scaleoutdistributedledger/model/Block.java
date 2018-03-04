package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.Temp;
import nl.tudelft.blockchain.scaleoutdistributedledger.Temp2;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.SDLByteArrayOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Block class.
 */
public class Block {

	public static final int GENESIS_BLOCK_NUMBER = 0;
	
	@Getter
	private final int number;

	@Getter @Setter
	private Block previousBlock;
	
	@Getter @Setter
	private Block nextCommittedBlock;

	@Getter @Setter
	private Node owner;

	@Getter
	private final List<Transaction> transactions;

	// Custom getter
	private Sha256Hash hash;
	
	private transient boolean onMainChain;
	private transient boolean hasNoAbstract;
	private transient volatile boolean committed;
	private transient volatile boolean finalized;
	@Getter @Setter
	private transient int[] cachedRequirements;
	@Getter @Setter
	private transient boolean evaluating;

	/**
	 * Constructor for a (genesis) block.
	 * @param number - the number of this block.
	 * @param owner - the owner of this block.
	 * @param transactions - a list of transactions of this block.
	 */
	public Block(int number, Node owner, List<Transaction> transactions) {
		this.number = number;
		this.owner = owner;
		this.previousBlock = null;
		this.transactions = transactions;
		for (Transaction transaction : this.transactions) {
			transaction.setBlockNumber(number);
		}
	}
	
	/**
	 * Constructor for an empty block.
	 * @param previousBlock - reference to the previous block in the chain of this block.
	 * @param owner         - the owner
	 */
	public Block(Block previousBlock, Node owner) {
		this.number = previousBlock.getNumber() + 1;
		this.previousBlock = previousBlock;
		this.owner = owner;
		this.transactions = new ArrayList<>();
		
		//Our own blocks are guaranteed to have no abstract until we create the abstract.
		if (this.owner instanceof OwnNode) {
			this.hasNoAbstract = true;
		}
	}

	/**
	 * Gets the transaction with the correct number in this block.
	 * @param transactionNumber - the number of the transaction to get.
	 * @return - the transaction.
	 */
	public Transaction getTransaction(int transactionNumber) {
		for (Transaction transaction : this.transactions) {
			if (transaction.getNumber() == transactionNumber)
				return transaction;
		}
		throw new IllegalStateException("Invalid transaction number");
	}

	/**
	 * Adds the given transaction to this block and sets its block number.
	 * @param transaction - the transaction to add
	 * @throws IllegalStateException - If this block has already been committed.
	 */
	public synchronized void addTransaction(Transaction transaction) {
		if (finalized) {
			throw new IllegalStateException("You cannot add transactions to a block that is already committed.");
		}
		
		transactions.add(transaction);
		transaction.setBlockNumber(getNumber());
	}
	
	/**
	 * Finalizes this block.
	 * This means that no transactions can be added to it, and that requirements are calculated.
	 */
	public synchronized void finalizeBlock() {
		if (this.finalized) return;
		
		this.finalized = true;
	}
	
	/**
	 * Get hash of the block.
	 * @return Hash SHA256
	 */
	public Sha256Hash getHash() {
		if (this.hash == null) {
			this.hash = calculateHash();
		}
		
		return this.hash;
	}

	/**
	 * Calculate the abstract of the block.
	 * @return abstract of the block
	 * @throws IllegalStateException - something went wrong while signing the block
	 */
	public BlockAbstract calculateBlockAbstract() {
		if (!(this.owner instanceof OwnNode)) {
			throw new UnsupportedOperationException("You cannot calculate the block abstract of a block you do not own!");
		}

		// Convert attributes of abstract into an array of bytes, for the signature
		// Important to keep the order of writings
		byte[] attrInBytes;
		try (SDLByteArrayOutputStream stream = new SDLByteArrayOutputStream(2 + 4 + getHash().getBytes().length)) {
			stream.writeShort(this.owner.getId());
			stream.writeInt(this.number);
			stream.write(getHash().getBytes());
			attrInBytes = stream.getByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to write to outputstream", ex);
		}

		// Sign the attributes
		try {
			byte[] signature = ((OwnNode) this.owner).sign(attrInBytes);
			BlockAbstract blockAbstract = new BlockAbstract(this.owner.getId(), this.number, getHash(), signature);
			this.hasNoAbstract = false;
			return blockAbstract;
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to sign block abstract", ex);
		}
	}
	
	/**
	 * Commits this block to the main chain.
	 * @param localStore - the local store
	 */
	public synchronized void commit(LocalStore localStore) {
		if (this.committed) {
			throw new IllegalStateException("This block has already been committed!");
		}

		Log.log(Level.FINER, "Committing block " + getNumber(), getOwner().getId());
		
		//Commit to the main chain, and set the last committed block
		localStore.getMainChain().commitAbstract(calculateBlockAbstract());
		
		//Set next committed block
		nextCommittedBlock = this;
		Block prev = getPreviousBlock();
		while (prev != null && prev.nextCommittedBlock == null) {
			prev.nextCommittedBlock = this;
			prev.finalized = true;
			prev = prev.getPreviousBlock();
		}
		
		this.finalized = true;
		this.committed = true;
		
		Temp2.updateRequirementsForCommit(this);
		
		getOwner().getChain().setLastCommittedBlock(this);
		//Temp.fillInBlockRequirementsForCommit(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + (owner == null ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Block)) return false;

		Block other = (Block) obj;
		if (this.number != other.number) return false;
		if (this.owner == null) {
			if (other.owner != null) return false;
		} else if (!this.owner.equals(other.owner)) return false;

		//TODO We might not want to use equals for the previous block (as it will recurse further)
		if (this.previousBlock == null) {
			if (other.previousBlock != null) return false;
		} else if (!this.previousBlock.equals(other.previousBlock)) return false;

		return this.transactions.equals(other.transactions);
	}
	
	@Override
	public String toString() {
		return "Block<nr=" + number + ", owner=" + owner + ", transactions=" + transactions + ">";
	}

	/**
	 * Calculates the block hash.
	 * @return Hash SHA256
	 */
	private Sha256Hash calculateHash() {
		// Convert attributes of block into an array of bytes
		int prevBlockLength = this.previousBlock != null ? Sha256Hash.LENGTH : 0;
		try (SDLByteArrayOutputStream stream = new SDLByteArrayOutputStream(4 + prevBlockLength + 2 + this.transactions.size() * Sha256Hash.LENGTH)) {
			// Important to keep the order of writings
			stream.writeInt(this.number);
			
			byte[] prevBlockHash = (this.previousBlock != null) ? this.previousBlock.getHash().getBytes() : new byte[0];
			stream.write(prevBlockHash);
			if (this.owner != null) {
				stream.writeShort(this.owner.getId());
			} else {
				stream.writeShort(-1);
			}
			
			for (Transaction tx : this.transactions) {
				stream.write(tx.getHash().getBytes());
			}
			
			return new Sha256Hash(stream.getByteArray());
		} catch (IOException ex) {
			Log.log(Level.SEVERE, "Unable to calculate hash of block!", ex);
			return null;
		}
	}

	/**
	 * Creates a copy of this genesis block.
	 * @return - a deep copy of this block and transactions
	 * @throws UnsupportedOperationException - If this block is not a genesis block.
	 */
	public Block genesisCopy() {
		if (this.number != GENESIS_BLOCK_NUMBER) throw new UnsupportedOperationException("You can only copy genesis blocks");
		
		Block block = new Block(this.number, this.owner, new ArrayList<>());
		for (Transaction transaction : transactions) {
			block.addTransaction(transaction.genesisCopy());
		}
		
		//The genesis block is on the main chain, cannot be modified and is its own committed block
		block.onMainChain = true;
		block.finalized = true;
		block.nextCommittedBlock = block;
		return block;
	}
	
	/**
	 * Returns if an abstract of this block is present on the main chain.
	 * @param localStore - the local store
	 * @return - boolean identifying if an abstract of this block is on the main chain.
	 */
	public boolean isOnMainChain(LocalStore localStore) {
		if (this.number == GENESIS_BLOCK_NUMBER) return true;

		//Definitely has no abstract
		if (this.hasNoAbstract) return false;

		//We already determined before what the result should be
		if (this.onMainChain) return true;

		//It is present, so store it and return
		if (localStore.getMainChain().isPresent(this)) {
			this.onMainChain = true;
			this.nextCommittedBlock = this;
			return true;
		}

		//Not present (yet)
		return false;
	}
}
