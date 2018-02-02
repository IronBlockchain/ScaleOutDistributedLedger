package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 * Test class for {@link Chain}.
 */
public class ChainTest {
	
	private Node node;
	
	private Chain chain;
	private LocalStore localstoreMock;

	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		this.node = mock(Node.class);
		this.localstoreMock = mock(LocalStore.class);
		this.chain = new Chain(node);
	}
	
	/**
	 * Test for {@link Chainw#update()}.
	 */
	@Test
	public void testUpdate_EmptyUpdate() {
		List<Block> updateList = new ArrayList<>();
		this.chain.update(updateList, localstoreMock);
		
		assertTrue(this.chain.getBlocks().isEmpty());
	}
	
	/**
	 * Test for {@link Chainw#update()}.
	 */
	@Test
	public void testUpdate_EmptyChain() {
		List<Block> updateList = new ArrayList<>();
		updateList.add(new Block(1, this.node, new ArrayList<>()));
		this.chain.update(updateList, localstoreMock);
		
		assertEquals(updateList, this.chain.getBlocks());
	}
	
	/**
	 * Test for {@link Chainw#update()}.
	 */
	@Test
	public void testUpdate_NotEmptyChain() {
		List<Block> updateList = new ArrayList<>();
		updateList.add(new Block(1, this.node, new ArrayList<>()));
		this.chain.update(updateList, localstoreMock);
		updateList.add(new Block(2, this.node, new ArrayList<>()));
		updateList.add(new Block(3, this.node, new ArrayList<>()));
		this.chain.update(updateList, localstoreMock);
		
		assertEquals(updateList, this.chain.getBlocks());
	}
	
}
