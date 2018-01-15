package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;

import static org.junit.Assert.assertTrue;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private HashMap<Integer, Node> nodeList;
	
	private Block genesisBlock;
	
	private OwnNode sender;
	
	private Node receiver;
	
	private LocalStore localStore;
	
	private Transaction transaction;
	
	private TransactionMessage transactionMessage;
	
	private Proof proof;
	
	private ProofMessage proofMessage;
	
	private Block block;
	
	private BlockMessage blockMessage;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		// Initialize node list
		this.nodeList = new HashMap<>();
		this.nodeList.put(0, new OwnNode(0));
		for (int i = 1; i < 10; i++) {
			this.nodeList.put(i, new Node(i));
		}
		// Encode: transactions, proofs and blocks
		// Setup sender and block
		this.sender = (OwnNode) this.nodeList.get(0);
		this.block = new Block(1234, sender, new ArrayList<>());
		List<Block> blockList = new ArrayList<>();
		blockList.add(this.block);
		blockList.add(new Block(1235, this.sender, new ArrayList<>()));
		this.sender.getChain().update(blockList);
		this.receiver = this.nodeList.get(1);
		// Generate genesis block (10 nodes, 1000 coins)
		this.genesisBlock = TendermintHelper.generateGenesisBlock(10, 1000, nodeList);
		// Setup LocalStore
		this.localStore = new LocalStore(this.sender, null, genesisBlock, false, new HashMap<>());
		this.localStore.getNodes().put(this.receiver.getId(), this.receiver);
		// Add Transaction
		this.transaction = new Transaction(44, this.sender, this.receiver, 100, 20, new HashSet<>());
		this.block.getTransactions().add(this.transaction);
		// Encode Block into BlockMessage
		this.blockMessage = new BlockMessage(this.block);
		// Encode Transaction into TransactionMessage
		this.transactionMessage = new TransactionMessage(this.transaction);
		// Add Proof
		this.proof = new Proof(this.transaction);
		// Encode Proof into ProofMessage
		this.proofMessage = new ProofMessage(this.proof);
	}
	
	/**
	 * Test the serialization of a genesis {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testBlockGenesis_Valid() throws IOException {
		// Encode genesis block
		BlockMessage newBlockMessage = new BlockMessage(this.genesisBlock);
		// Check
		assertTrue(newBlockMessage.getNumber() == this.genesisBlock.getNumber());
		assertTrue(newBlockMessage.getOwnerId() == Transaction.GENESIS_SENDER);
		assertTrue(newBlockMessage.getTransactions().size() == this.nodeList.size());
	}
	
	/**
	 * Test the serialization of {@link Transaction}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testTransaction_Valid() throws IOException {
		// Decode transaction object
		Transaction newTransaction = new Transaction(this.transactionMessage, this.localStore);
		// Check primitives
		assertTrue(newTransaction.getNumber() == this.transaction.getNumber());
		assertTrue(newTransaction.getAmount() == this.transaction.getAmount());
		assertTrue(newTransaction.getReceiver() == this.transaction.getReceiver());
		assertTrue(newTransaction.getBlockNumber().equals(this.transaction.getBlockNumber()));
		// Check references
		assertTrue(newTransaction.getSender().equals(this.transaction.getSender()));
		assertTrue(newTransaction.getReceiver().equals(this.transaction.getReceiver()));
		assertTrue(newTransaction.getHash().equals(this.transaction.getHash()));
		// Check sets
		assertTrue(newTransaction.getSource().equals(this.transaction.getSource()));
	}
	
	/**
	 * Test the serialization of {@link Proof}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testProof_Valid() throws IOException {
		// Decode transaction object
		Proof newProof = new Proof(this.proofMessage, this.localStore);
		// Check references
		assertTrue(newProof.getTransaction().equals(this.proof.getTransaction()));
		assertTrue(newProof.getChainUpdates().equals(this.proof.getChainUpdates()));
	}
	
	/**
	 * Test the serialization of NOT genesis {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testBlock_Valid() throws IOException {
		// Decode transaction object
		Block newBlock = new Block(this.blockMessage, this.localStore);
		// Check primitives
		assertTrue(newBlock.getNumber() == this.block.getNumber());
		// Check references
		if (newBlock.getPreviousBlock() != null) {
			assertTrue(newBlock.getPreviousBlock().equals(this.block.getPreviousBlock()));
		}
		assertTrue(newBlock.getOwner().equals(this.block.getOwner()));
		assertTrue(newBlock.getHash().equals(this.block.getHash()));
		// Check list
		assertTrue(newBlock.getTransactions().equals(this.block.getTransactions()));
	}
	
	/**
	 * Test the serialization of {@link Transaction}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testTransaction_WithMetaknowledge() throws IOException {
		// Setup new receiver
		Node newReceiver = this.nodeList.get(2);
		newReceiver.getMetaKnowledge().put(this.sender, 1234);
		this.localStore.getNodes().put(newReceiver.getId(), newReceiver);
		
		// Create new transaction
		HashSet<Transaction> listSources = new HashSet<>();
		listSources.add(this.transaction);
		Transaction newTransaction = new Transaction(88, this.sender, newReceiver, 200, 40, listSources);
		this.sender.getChain().getBlocks().get(1).getTransactions().add(newTransaction);
		
		// Encode
		TransactionMessage newTransactionMessage = new TransactionMessage(newTransaction);
		// Check the correctness of the encoding
		assertTrue(newTransactionMessage.getKnownSource().contains(new SimpleEntry<>(this.sender.getId(), this.transaction.getNumber())));
		assertTrue(newTransactionMessage.getNewSource().isEmpty());
		// Decode transaction object
		Transaction decodedTransaction = new Transaction(newTransactionMessage, this.localStore);
		
		// newReceiver gets the sources correctly
		assertTrue(decodedTransaction.getSource().size() == 1);
		Transaction sourceTransaction = decodedTransaction.getSource().iterator().next();
		assertTrue(sourceTransaction.getNumber() == this.transaction.getNumber());
		assertTrue(sourceTransaction.getAmount() == this.transaction.getAmount());
		assertTrue(sourceTransaction.getRemainder() == this.transaction.getRemainder());
		assertTrue(sourceTransaction.getHash().equals(this.transaction.getHash()));
		assertTrue(sourceTransaction.getBlockNumber().equals(this.transaction.getBlockNumber()));
	}
	
}
