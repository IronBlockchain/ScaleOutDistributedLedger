package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.io.ByteArrayOutputStream;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link Block}.
 */
public class BlockTest {
	
	private Node owner;
	
	private Block block;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		this.owner = new Node(24);
		this.block = new Block(1234, owner, new ArrayList<>());
	}
	
	/**
	 * Test for {@link Block#getHash()}.
	 */
	@Test
	public void testGetHash_Valid() {
		String hash = "334777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertTrue(this.block.getHash().toString().equals(hash));
	}
	
	/**
	 * Test for {@link Block#getHash()}.
	 */
	@Test
	public void testGetHash_Invalid() {
		String hash = "004777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertFalse(this.block.getHash().toString().equals(hash));
	}

	/**
	 * Test for {@link Block#getAbstract()}.
	 */
	@Test
	public void testGetAbstract_Valid() {
		RSAKey key = new RSAKey();
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(Utils.intToByteArray(this.block.getOwner().getId()));
			outputStream.write(Utils.intToByteArray(this.block.getNumber()));
			outputStream.write(this.block.getHash().getBytes());
			byte[] attrInBytes = outputStream.toByteArray();

			assertTrue(key.verify(attrInBytes, this.block.getAbstract(key).getSignature()));
		} catch (Exception ex) {
			fail();
		}
	}
	
	/**
	 * Test for {@link Block#getAbstract()}.
	 */
	@Test
	public void testGetAbstract_Invalid() {
		RSAKey key = new RSAKey();
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(Utils.intToByteArray(this.block.getOwner().getId() + 1));
			outputStream.write(Utils.intToByteArray(this.block.getNumber()));
			outputStream.write(this.block.getHash().getBytes());
			byte[] attrInBytes = outputStream.toByteArray();

			assertFalse(key.verify(attrInBytes, this.block.getAbstract(key).getSignature()));
		} catch (Exception ex) {
			fail();
		}
	}
	
}
