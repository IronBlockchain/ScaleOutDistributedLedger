package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

import java.util.List;

/**
 * Block class.
 */
public class Block {

    @Getter
    final int number;

    @Getter
    Block previousBlock;

    @Getter
    final Node owner;

    @Getter
    final List<Transaction> transactions;

    /**
     * Constructor.
     * @param number - the number of this block.
     * @param owner - the owner of this block.
     * @param transactions - a list of transactions of this block.
     */
    public Block(int number, Node owner, List<Transaction> transactions) {
        this.number = number;
        this.owner = owner;
        this.transactions = transactions;
        this.previousBlock = null;
    }

    /**
     * Constructor.
     * @param number - the number of this block.
     * @param previousBlock - reference to the previous block in the chain of this block.
     * @param owner - the owner of this block.
     * @param transactions - a list of transactions of this block.
     */
    public Block(int number, Block previousBlock, Node owner, List<Transaction> transactions) {
        this.number = number;
        this.previousBlock = previousBlock;
        this.owner = owner;
        this.transactions = transactions;
    }
}
