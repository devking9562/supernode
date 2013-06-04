package com.bitsofproof.supernode.api;

import java.util.Collection;

import com.bitsofproof.supernode.common.BloomFilter;
import com.bitsofproof.supernode.common.BloomFilter.UpdateMode;

public interface ServerConnector
{
	/**
	 * returns nounce while doing a full roundtrip to the server
	 * 
	 * @param nonce
	 * @return
	 * @throws BCSAPIException
	 */
	public long ping (long nonce) throws BCSAPIException;

	/**
	 * sets the alert listener for the connections
	 * 
	 * @param listener
	 * @throws BCSAPIException
	 */
	public void addAlertListener (AlertListener listener) throws BCSAPIException;

	public void removeAlertListener (AlertListener listener);

	/**
	 * Are we talking to production?
	 * 
	 * @return
	 * @throws BCSAPIException
	 */
	public boolean isProduction () throws BCSAPIException;

	/**
	 * get block header for the hash
	 * 
	 * @param hash
	 * @return block header or null if hash is unknown
	 * @throws BCSAPIException
	 */
	public Block getBlockHeader (String hash) throws BCSAPIException;

	/**
	 * get block for the hash
	 * 
	 * @param hash
	 * @return block or null if hash is unknown
	 * @throws BCSAPIException
	 */
	public Block getBlock (String hash) throws BCSAPIException;

	/**
	 * get the transaction identified by the hash on the trunk
	 * 
	 * @param hash
	 * @return transaction or null if no transaction with that hash on the trunk
	 * @throws BCSAPIException
	 */
	public Transaction getTransaction (String hash) throws BCSAPIException;

	/**
	 * send a signed transaction
	 * 
	 * @param transaction
	 * @throws BCSAPIException
	 */
	public void sendTransaction (Transaction transaction) throws BCSAPIException;

	/**
	 * send a mined block
	 * 
	 * @param block
	 * @throws BCSAPIException
	 */
	public void sendBlock (Block block) throws BCSAPIException;

	/**
	 * Register a transactions listener
	 * 
	 * @param listener
	 *            will be called for every validated transaction
	 * @throws BCSAPIException
	 */
	public void registerTransactionListener (TransactionListener listener) throws BCSAPIException;

	/**
	 * remove a listener for validated transactions
	 * 
	 * @param listener
	 */
	public void removeTransactionListener (TransactionListener listener);

	/**
	 * Register a block listener
	 * 
	 * @param listener
	 *            will be called for every validated new block
	 * @throws BCSAPIException
	 */
	public void registerTrunkListener (TrunkListener listener) throws BCSAPIException;

	/**
	 * remove a trunk listener previously registered
	 * 
	 * @param listener
	 */
	public void removeTrunkListener (TrunkListener listener);

	/**
	 * Match transactions using and address or outpoint in match. This returns only exact matches but limited in result set.
	 * 
	 * @param match
	 * @param listener
	 * @throws BCSAPIException
	 */
	public void scanTransactions (Collection<byte[]> match, UpdateMode mode, long after, TransactionListener listener) throws BCSAPIException;

	/**
	 * scan transactions matching the filter
	 * 
	 * @param filter
	 * @param after
	 * @param listener
	 * @throws BCSAPIException
	 */
	public void scanTransactions (BloomFilter filter, TransactionListener listener) throws BCSAPIException;

	/**
	 * register listener with a Bloom filter
	 * 
	 * @param filter
	 * @param listener
	 * @throws BCSAPIException
	 */
	public void registerFilteredListener (BloomFilter filter, TransactionListener listener) throws BCSAPIException;

	/**
	 * remove the listener of a Bloom filter
	 * 
	 * @param filter
	 * @param listener
	 */
	public void removeFilteredListener (BloomFilter filter, TransactionListener listener);
}
