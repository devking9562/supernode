/*
 * Copyright 2012 Tamas Blummer tamas@bitsofproof.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitsofproof.supernode.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bitsofproof.supernode.core.AddressConverter;
import com.bitsofproof.supernode.core.Chain;
import com.bitsofproof.supernode.core.Difficulty;
import com.bitsofproof.supernode.core.Hash;
import com.bitsofproof.supernode.core.Script;
import com.bitsofproof.supernode.core.Script.Opcode;
import com.bitsofproof.supernode.core.ValidationException;
import com.mysema.query.jpa.impl.JPAQuery;

@Component ("jpaBlockStore")
class JpaBlockStore implements BlockStore
{
	private static final Logger log = LoggerFactory.getLogger (JpaBlockStore.class);

	private static final long MAX_BLOCK_SIGOPS = 20000;

	// a block can not branch from earlier than this
	private static final int MAXBRANCH = 100;

	@Autowired
	private Chain chain;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock ();

	private CachedHead currentHead = null;
	private final Map<String, CachedHead> heads = new HashMap<String, CachedHead> ();
	private final Map<String, CachedBlock> cachedBlocks = new HashMap<String, CachedBlock> ();
	private final Map<Long, CachedHead> cachedHeads = new HashMap<Long, CachedHead> ();

	private final ExecutorService inputProcessor = Executors.newFixedThreadPool (Runtime.getRuntime ().availableProcessors ());
	private final ExecutorService transactionsProcessor = Executors.newFixedThreadPool (Runtime.getRuntime ().availableProcessors ());

	private static class CachedHead
	{
		private CachedBlock last;
		private double chainWork;
		private long height;
		private CachedHead previous;
		private final Set<CachedBlock> blocks = new HashSet<CachedBlock> ();

		public double getChainWork ()
		{
			return chainWork;
		}

		public long getHeight ()
		{
			return height;
		}

		public void setChainWork (double chainWork)
		{
			this.chainWork = chainWork;
		}

		public void setHeight (long height)
		{
			this.height = height;
		}

		public Set<CachedBlock> getBlocks ()
		{
			return blocks;
		}

		public CachedHead getPrevious ()
		{
			return previous;
		}

		public void setPrevious (CachedHead previous)
		{
			this.previous = previous;
		}

		public CachedBlock getLast ()
		{
			return last;
		}

		public void setLast (CachedBlock last)
		{
			this.last = last;
		}

	}

	private static class CachedBlock
	{
		public CachedBlock (String hash, Long id, CachedBlock previous, long time)
		{
			this.hash = hash;
			this.id = id;
			this.previous = previous;
			this.time = time;
		}

		private final String hash;
		private final Long id;
		private final CachedBlock previous;
		private final long time;

		public Long getId ()
		{
			return id;
		}

		public CachedBlock getPrevious ()
		{
			return previous;
		}

		public long getTime ()
		{
			return time;
		}

		public String getHash ()
		{
			return hash;
		}

		@Override
		public int hashCode ()
		{
			return hash.hashCode ();
		}
	}

	@Transactional (propagation = Propagation.REQUIRED)
	@Override
	public void cache ()
	{
		try
		{
			lock.writeLock ().lock ();

			log.trace ("filling chain cache with heads");
			QHead head = QHead.head;
			JPAQuery q = new JPAQuery (entityManager);
			for ( Head h : q.from (head).list (head) )
			{
				CachedHead sh = new CachedHead ();
				sh.setChainWork (h.getChainWork ());
				sh.setHeight (h.getHeight ());
				heads.put (h.getLeaf (), sh);
				if ( h.getPrevious () != null )
				{
					sh.setPrevious (cachedHeads.get (h.getId ()));
				}
				cachedHeads.put (h.getId (), sh);
				if ( currentHead == null || currentHead.getChainWork () < sh.getChainWork () )
				{
					currentHead = sh;
				}
			}

			log.trace ("filling chain cache with stored blocks");
			QBlk block = QBlk.blk;
			q = new JPAQuery (entityManager);
			for ( Blk b : q.from (block).list (block) )
			{
				CachedBlock cb = null;
				if ( b.getPrevious () != null )
				{
					cb = new CachedBlock (b.getHash (), b.getId (), cachedBlocks.get (b.getPrevious ().getHash ()), b.getCreateTime ());
				}
				else
				{
					cb = new CachedBlock (b.getHash (), b.getId (), null, b.getCreateTime ());
				}
				cachedBlocks.put (b.getHash (), cb);
				CachedHead h = cachedHeads.get (b.getHead ().getId ());
				h.getBlocks ().add (cb);
				h.setLast (cb);
			}
		}
		finally
		{
			lock.writeLock ().unlock ();
		}
	}

	@Override
	public Blk getGenesisBlock ()
	{
		try
		{
			lock.writeLock ().lock ();

			QBlk block = QBlk.blk;
			JPAQuery q = new JPAQuery (entityManager);
			return q.from (block).orderBy (block.id.asc ()).limit (1).uniqueResult (block);
		}
		finally
		{
			lock.writeLock ().unlock ();
		}
	}

	@Override
	public boolean isStoredBlock (String hash)
	{
		try
		{
			lock.readLock ().lock ();

			return cachedBlocks.get (hash) != null;
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	@Override
	public long getChainHeight ()
	{
		try
		{
			lock.readLock ().lock ();

			return currentHead.getHeight ();
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	@Override
	public List<String> getInventory (List<String> locator, String last, int limit)
	{
		try
		{
			lock.readLock ().lock ();

			List<String> inventory = new LinkedList<String> ();
			CachedBlock curr = currentHead.getLast ();
			CachedBlock prev = curr.getPrevious ();
			if ( !last.equals (Hash.ZERO_HASH.toString ()) )
			{
				while ( prev != null && !curr.equals (last) )
				{
					curr = prev;
					prev = curr.getPrevious ();
				}
			}
			do
			{
				if ( locator.contains (curr) )
				{
					break;
				}
				inventory.add (0, curr.getHash ());
				if ( inventory.size () > limit )
				{
					inventory.remove (limit);
				}
				curr = prev;
				if ( prev != null )
				{
					prev = curr.getPrevious ();
				}
			} while ( curr != null );
			return inventory;
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	@Override
	public List<String> getLocator ()
	{
		try
		{
			lock.readLock ().lock ();

			List<String> locator = new ArrayList<String> ();
			CachedBlock curr = currentHead.getLast ();
			locator.add (curr.getHash ());
			CachedBlock prev = curr.getPrevious ();
			for ( int i = 0, step = 1; prev != null; ++i )
			{
				for ( int j = 0; prev != null && j < step; ++j )
				{
					curr = prev;
					prev = curr.getPrevious ();
				}
				locator.add (curr.getHash ());
				if ( i >= 10 )
				{
					step *= 2;
				}
			}
			return locator;
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	@Override
	public List<TxOut> getUnspentOutput (List<String> addresses)
	{
		List<TxOut> unspent = new ArrayList<TxOut> ();
		try
		{
			lock.readLock ().lock ();

			QTxOut txout = QTxOut.txOut;
			QOwner owner = QOwner.owner;
			JPAQuery query = new JPAQuery (entityManager);

			for ( TxOut out : query.from (txout).join (txout.owners, owner).where (owner.address.in (addresses)).list (txout) )
			{
				CachedBlock blockOfOut = cachedBlocks.get (out.getTransaction ().getBlock ().getHash ());
				if ( isBlockOnBranch (blockOfOut, currentHead) )
				{
					unspent.add (out);
				}
			}
			if ( !unspent.isEmpty () )
			{
				QTxIn txin = QTxIn.txIn;
				query = new JPAQuery (entityManager);
				for ( TxIn in : query.from (txin).where (txin.source.in (unspent)).list (txin) )
				{
					CachedBlock blockOfIn = cachedBlocks.get (in.getTransaction ().getBlock ().getHash ());
					if ( isBlockOnBranch (blockOfIn, currentHead) )
					{
						unspent.remove (in.getSource ());
					}
				}
			}
			return unspent;
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	private static class TransactionContext
	{
		Blk block;
		BigInteger blkSumInput = BigInteger.ZERO;
		BigInteger blkSumOutput = BigInteger.ZERO;
		int nsigs = 0;
		boolean coinbase = true;
		Map<String, ArrayList<TxOut>> transactionsOutputCache = new HashMap<String, ArrayList<TxOut>> ();
		Map<String, HashMap<Integer, TxOut>> resolvedInputs = new HashMap<String, HashMap<Integer, TxOut>> ();
	}

	@Transactional (propagation = Propagation.REQUIRED, rollbackFor = { Exception.class })
	@Override
	public void storeBlock (Blk b) throws ValidationException
	{
		try
		{
			lock.writeLock ().lock ();

			lockedStoreBlock (b);
		}
		catch ( ValidationException e )
		{
			throw e;
		}
		catch ( Exception e )
		{
			throw new ValidationException ("OTHER exception " + b.toWireDump (), e);
		}
		finally
		{
			lock.writeLock ().unlock ();
		}
	}

	private void lockedStoreBlock (Blk b) throws ValidationException
	{
		CachedBlock cached = cachedBlocks.get (b.getHash ());
		if ( cached != null )
		{
			return;
		}

		// find previous block
		CachedBlock cachedPrevious = cachedBlocks.get (b.getPreviousHash ());
		if ( cachedPrevious != null )
		{
			Blk prev = null;
			prev = entityManager.find (Blk.class, (cachedPrevious).getId ());
			b.setPrevious (prev);

			if ( b.getCreateTime () > (System.currentTimeMillis () / 1000) * 2 * 60 * 60 )
			{
				throw new ValidationException ("Future generation attempt " + b.getHash ());
			}

			boolean branching = false;
			Head head;
			if ( prev.getHead ().getLeaf ().equals (prev.getHash ()) )
			{
				// continuing
				head = prev.getHead ();

				head.setLeaf (b.getHash ());
				head.setHeight (head.getHeight () + 1);
				head.setChainWork (prev.getChainWork () + Difficulty.getDifficulty (b.getDifficultyTarget ()));
				head = entityManager.merge (head);
			}
			else
			{
				// branching
				branching = true;
				// check if branch is not too far back
				CachedBlock c = currentHead.getLast ();
				CachedBlock p = c.getPrevious ();
				int i = 0;
				while ( p != null && !p.getHash ().equals (prev.getHash ()) )
				{
					c = p;
					p = c.getPrevious ();
					if ( ++i == MAXBRANCH )
					{
						throw new ValidationException ("attempt to branch too far back in past " + b.toWireDump ());
					}
				}

				head = new Head ();
				head.setTrunk (prev.getHash ());
				head.setHeight (prev.getHeight ());
				head.setChainWork (prev.getChainWork ());
				head.setPrevious (prev.getHead ());

				head.setLeaf (b.getHash ());
				head.setHeight (head.getHeight () + 1);
				head.setChainWork (prev.getChainWork () + Difficulty.getDifficulty (b.getDifficultyTarget ()));
				entityManager.persist (head);
			}
			b.setHead (head);
			b.setHeight (head.getHeight ());
			b.setChainWork (head.getChainWork ());

			if ( b.getHeight () >= chain.getDifficultyReviewBlocks () && b.getHeight () % chain.getDifficultyReviewBlocks () == 0 )
			{
				CachedBlock c = null;
				CachedBlock p = cachedPrevious;
				for ( int i = 0; i < chain.getDifficultyReviewBlocks () - 1; ++i )
				{
					c = p;
					p = c.getPrevious ();
				}

				long next = Difficulty.getNextTarget (prev.getCreateTime () - p.getTime (), prev.getDifficultyTarget (), chain.getTargetBlockTime ());
				if ( chain.isProduction () && next != b.getDifficultyTarget () )
				{
					throw new ValidationException ("Difficulty does not match expectation " + b.getHash () + " " + b.toWireDump ());
				}
			}
			else
			{
				if ( chain.isProduction () && b.getDifficultyTarget () != prev.getDifficultyTarget () )
				{
					throw new ValidationException ("Illegal attempt to change difficulty " + b.getHash ());
				}
			}
			if ( chain.isProduction () && new Hash (b.getHash ()).toBigInteger ().compareTo (Difficulty.getTarget (b.getDifficultyTarget ())) > 0 )
			{
				throw new ValidationException ("Insufficuent proof of work for current difficulty " + b.getHash () + " " + b.toWireDump ());
			}

			b.parseTransactions ();

			if ( b.getTransactions ().isEmpty () )
			{
				throw new ValidationException ("Block must have transactions " + b.getHash () + " " + b.toWireDump ());
			}

			b.checkMerkleRoot ();

			final TransactionContext tcontext = new TransactionContext ();
			tcontext.block = b;

			boolean skip = true;
			log.trace ("resolving inputs for block " + b.getHash ());
			for ( Tx t : b.getTransactions () )
			{
				ArrayList<TxOut> outs = new ArrayList<TxOut> ();
				for ( int i = 0; i < t.getOutputs ().size (); ++i )
				{
					outs.add (t.getOutputs ().get (i));
				}
				tcontext.transactionsOutputCache.put (t.getHash (), outs);
				if ( skip ) // skip coinbase
				{
					skip = false;
				}
				else
				{
					resolveInputs (tcontext, t);
				}
			}

			log.trace ("validating block " + b.getHash ());
			List<Callable<TransactionValidationException>> callables = new ArrayList<Callable<TransactionValidationException>> ();
			for ( final Tx t : b.getTransactions () )
			{
				t.setBlock (b);
				if ( tcontext.coinbase )
				{
					try
					{
						validateTransaction (tcontext, t);
					}
					catch ( TransactionValidationException e )
					{
						throw new ValidationException (e.getMessage () + " " + t.toWireDump (), e);
					}
				}
				else
				{
					callables.add (new Callable<TransactionValidationException> ()
					{
						@Override
						public TransactionValidationException call ()
						{
							try
							{
								validateTransaction (tcontext, t);
							}
							catch ( TransactionValidationException e )
							{
								return e;
							}
							catch ( Exception e )
							{
								return new TransactionValidationException (e, t);
							}
							return null;
						}
					});
				}
			}
			try
			{
				for ( Future<TransactionValidationException> e : transactionsProcessor.invokeAll (callables) )
				{
					try
					{
						if ( e.get () != null )
						{
							Tx t = e.get ().getTx ();
							int in = e.get ().getIn ();
							if ( in >= 0 )
							{
								int out = (int) t.getInputs ().get (in).getSource ().getIx ();
								throw new ValidationException (e.get ().getMessage () + " " + in + "-" + out + " " + t.toWireDump () + " "
										+ t.getInputs ().get (in).getSource ().getTransaction ().toWireDump (), e.get ());
							}
							throw new ValidationException (e.get ().getMessage () + " " + e.get ().getTx ().toWireDump (), e.get ());
						}
					}
					catch ( ExecutionException e1 )
					{
						throw new ValidationException ("corrupted transaction processor", e1);
					}
				}
			}
			catch ( InterruptedException e1 )
			{
				throw new ValidationException ("interrupted", e1);
			}

			// block reward could actually be less... as in 0000000000004c78956f8643262f3622acf22486b120421f893c0553702ba7b5
			if ( tcontext.blkSumOutput.subtract (tcontext.blkSumInput).longValue () > ((50L * Tx.COIN) >> (b.getHeight () / 210000L)) )
			{
				throw new ValidationException ("Invalid block reward " + b.getHash () + " " + b.toWireDump ());
			}

			log.trace ("storing block " + b.getHash ());
			entityManager.persist (b);

			// modify transient caches only after persistent changes
			CachedBlock m = new CachedBlock (b.getHash (), b.getId (), cachedBlocks.get (b.getPrevious ().getHash ()), b.getCreateTime ());
			cachedBlocks.put (b.getHash (), m);

			CachedHead usingHead = currentHead;
			if ( branching )
			{
				heads.put (b.getHash (), usingHead = new CachedHead ());
				cachedHeads.put (head.getId (), usingHead);
			}
			usingHead.setLast (m);
			usingHead.setChainWork (head.getChainWork ());
			usingHead.setHeight (head.getHeight ());
			usingHead.getBlocks ().add (m);

			if ( head.getChainWork () > currentHead.getChainWork () )
			{
				currentHead = usingHead;
			}

			log.trace ("stored block " + b.getHeight () + " " + b.getHash ());
		}
	}

	private boolean isBlockOnBranch (CachedBlock block, CachedHead branch)
	{
		if ( branch.getBlocks ().contains (block) )
		{
			return true;
		}
		if ( branch.getPrevious () == null )
		{
			return false;
		}
		return isBlockOnBranch (block, branch.getPrevious ());
	}

	private void resolveInputs (TransactionContext tcontext, Tx t) throws ValidationException
	{
		HashMap<Integer, TxOut> resolved;
		if ( (resolved = tcontext.resolvedInputs.get (t.getHash ())) == null )
		{
			resolved = new HashMap<Integer, TxOut> ();
			tcontext.resolvedInputs.put (t.getHash (), resolved);
		}

		Set<String> inputtx = new HashSet<String> ();
		for ( final TxIn i : t.getInputs () )
		{
			ArrayList<TxOut> outs;
			if ( (outs = tcontext.transactionsOutputCache.get (i.getSourceHash ())) != null )
			{
				if ( i.getIx () >= outs.size () )
				{
					throw new ValidationException ("Transaction refers to output number not available " + t.toWireDump ());
				}
			}
			else
			{
				inputtx.add (i.getSourceHash ());
			}
		}

		if ( !inputtx.isEmpty () )
		{
			QTx tx = QTx.tx;
			QTxOut txout = QTxOut.txOut;
			JPAQuery query = new JPAQuery (entityManager);

			// Unfortunatelly tx hash is not unique (not even on a branch) ordering ensures earlier will be overwritten
			for ( TxOut out : query.from (tx).join (tx.outputs, txout).where (tx.hash.in (inputtx)).orderBy (tx.id.asc (), txout.ix.asc ()).list (txout) )
			{
				CachedBlock blockOfOut = cachedBlocks.get (out.getTransaction ().getBlock ().getHash ());
				if ( isBlockOnBranch (blockOfOut, currentHead) )
				{
					ArrayList<TxOut> cached = tcontext.transactionsOutputCache.get (out.getTransaction ().getHash ());
					if ( cached == null || cached.size () > out.getIx () ) // replace if more than one tx
					{
						cached = new ArrayList<TxOut> ();
						tcontext.transactionsOutputCache.put (out.getTransaction ().getHash (), cached);
					}
					cached.add (out);
				}
			}
		}
		// check for double spending
		Set<TxOut> outs = new HashSet<TxOut> ();
		int nr = 0;
		for ( final TxIn i : t.getInputs () )
		{
			ArrayList<TxOut> cached;
			TxOut transactionOutput = null;
			if ( (cached = tcontext.transactionsOutputCache.get (i.getSourceHash ())) == null )
			{
				throw new ValidationException ("Transaction refers to unknown source " + i.getSourceHash () + " " + t.toWireDump ());
			}
			transactionOutput = cached.get ((int) i.getIx ());
			if ( transactionOutput == null )
			{
				throw new ValidationException ("Transaction refers to unknown input " + i.getSourceHash () + " [" + i.getIx () + "] " + t.toWireDump ());
			}
			if ( transactionOutput.getId () != null )
			{
				// double spend within same block will be caught by the sum in/out
				outs.add (transactionOutput);
			}
			resolved.put (nr++, transactionOutput);
		}

		if ( !outs.isEmpty () )
		{
			// find previously spending blocks
			List<String> dsblocks = new ArrayList<String> ();

			QTx tx = QTx.tx;
			QBlk blk = QBlk.blk;
			QTxIn txin = QTxIn.txIn;
			JPAQuery query = new JPAQuery (entityManager);
			dsblocks = query.from (tx).join (tx.block, blk).join (tx.inputs, txin).where (txin.source.in (outs)).list (blk.hash);
			for ( String dsbh : dsblocks )
			{
				if ( isBlockOnBranch (cachedBlocks.get (dsbh), currentHead) )
				{
					throw new ValidationException ("Double spend attempt " + t.toWireDump ());
				}
			}
		}

		if ( tcontext.block != null )
		{
			// check coinbase spending
			for ( int j = 0; j < nr; ++j )
			{
				TxOut transactionOutput = resolved.get (j);
				if ( transactionOutput.getTransaction ().getInputs ().get (0).getSource () == null && transactionOutput.getTransaction ().getBlock () != null
						&& transactionOutput.getTransaction ().getBlock ().getHeight () > currentHead.getHeight () - 100 )
				{
					throw new ValidationException ("coinbase spent too early " + t.toWireDump ());
				}
			}
		}
	}

	private void validateTransaction (final TransactionContext tcontext, final Tx t) throws TransactionValidationException
	{
		if ( tcontext.block != null && tcontext.coinbase )
		{
			if ( t.getInputs ().size () != 1 || !t.getInputs ().get (0).getSourceHash ().equals (Hash.ZERO_HASH.toString ())
					|| (chain.isProduction () && t.getInputs ().get (0).getSequence () != 0xFFFFFFFFL) )
			{
				throw new TransactionValidationException ("first transaction must be coinbase ", t);
			}
			if ( t.getInputs ().get (0).getScript ().length > 100 || t.getInputs ().get (0).getScript ().length < 2 )
			{
				throw new TransactionValidationException ("coinbase scriptsig must be in 2-100 ", t);
			}
			tcontext.coinbase = false;
			for ( TxOut o : t.getOutputs () )
			{
				try
				{
					if ( chain.isProduction () && !Script.isStandard (o.getScript ()) )
					{
						throw new TransactionValidationException ("Nonstandard script rejected", t);
					}
					tcontext.blkSumOutput = tcontext.blkSumOutput.add (BigInteger.valueOf (o.getValue ()));
					tcontext.nsigs += Script.sigOpCount (o.getScript ());
				}
				catch ( ValidationException e )
				{
					throw new TransactionValidationException (e, t);
				}
			}
			if ( tcontext.nsigs > MAX_BLOCK_SIGOPS )
			{
				throw new TransactionValidationException ("too many signatures in this block ", t);
			}
			for ( TxOut out : t.getOutputs () )
			{
				addOwners (out);
			}
		}
		else
		{
			if ( t.getInputs ().size () == 1 && t.getInputs ().get (0).getSourceHash ().equals (Hash.ZERO_HASH.toString ()) )
			{
				throw new TransactionValidationException ("coinbase only first in a block", t);
			}
			if ( t.getOutputs ().isEmpty () )
			{
				throw new TransactionValidationException ("Transaction must have outputs ", t);
			}
			if ( t.getInputs ().isEmpty () )
			{
				throw new TransactionValidationException ("Transaction must have inputs ", t);
			}

			long sumOut = 0;
			for ( TxOut o : t.getOutputs () )
			{
				if ( o.getScript ().length > 520 )
				{
					if ( tcontext.block != null && tcontext.block.getHeight () < 80000 )
					{
						log.trace ("Old DoD at [" + tcontext.block.getHeight () + "]" + tcontext.block.getHash ());
					}
					else
					{
						throw new TransactionValidationException ("script too long ", t);
					}
				}
				if ( chain.isProduction () )
				{
					try
					{
						if ( tcontext.block.getHeight () > 80000 && !Script.isStandard (o.getScript ()) )
						{
							throw new TransactionValidationException ("Nonstandard script rejected", t);
						}
					}
					catch ( ValidationException e )
					{
						throw new TransactionValidationException (e, t);
					}
				}
				if ( tcontext.block != null )
				{
					try
					{
						tcontext.nsigs += Script.sigOpCount (o.getScript ());
					}
					catch ( ValidationException e )
					{
						throw new TransactionValidationException (e, t);
					}
					if ( tcontext.nsigs > MAX_BLOCK_SIGOPS )
					{
						throw new TransactionValidationException ("too many signatures in this block ", t);
					}
				}
				if ( o.getValue () < 0 || o.getValue () > Tx.MAX_MONEY )
				{
					throw new TransactionValidationException ("Transaction output not in money range ", t);
				}
				tcontext.blkSumOutput = tcontext.blkSumOutput.add (BigInteger.valueOf (o.getValue ()));
				sumOut += o.getValue ();
				if ( sumOut < 0 || sumOut > Tx.MAX_MONEY )
				{
					throw new TransactionValidationException ("Transaction output not in money range ", t);
				}
				if ( tcontext.block != null )
				{
					addOwners (o);
				}
			}

			long sumIn = 0;
			int inNumber = 0;
			List<Callable<TransactionValidationException>> callables = new ArrayList<Callable<TransactionValidationException>> ();
			HashMap<Integer, TxOut> resolved = tcontext.resolvedInputs.get (t.getHash ());
			final Set<String> signatureCache = new HashSet<String> ();
			for ( final TxIn i : t.getInputs () )
			{
				if ( i.getScript ().length > 520 )
				{
					if ( tcontext.block == null || tcontext.block.getHeight () > 80000 )
					{
						throw new TransactionValidationException ("script too long ", t);
					}
				}

				i.setSource (resolved.get (inNumber));
				sumIn += i.getSource ().getValue ();

				final int nr = inNumber;
				callables.add (new Callable<TransactionValidationException> ()
				{
					@Override
					public TransactionValidationException call () throws Exception
					{
						try
						{
							if ( !new Script (t, nr, signatureCache).evaluate () )
							{
								return new TransactionValidationException ("The transaction script does not evaluate to true in input", t, nr);
							}

							synchronized ( tcontext )
							{
								tcontext.blkSumInput = tcontext.blkSumInput.add (BigInteger.valueOf (i.getSource ().getValue ()));
							}
						}
						catch ( Exception e )
						{
							return new TransactionValidationException (e, t, nr);
						}
						return null;
					}
				});
				++inNumber;
			}
			List<Future<TransactionValidationException>> results;
			try
			{
				results = inputProcessor.invokeAll (callables);
			}
			catch ( InterruptedException e1 )
			{
				throw new TransactionValidationException (e1, t);
			}
			for ( Future<TransactionValidationException> r : results )
			{
				TransactionValidationException ex;
				try
				{
					ex = r.get ();
				}
				catch ( InterruptedException e )
				{
					throw new TransactionValidationException (e, t);
				}
				catch ( ExecutionException e )
				{
					throw new TransactionValidationException (e, t);
				}
				if ( ex != null )
				{
					throw ex;
				}
			}
			if ( sumOut > sumIn )
			{
				throw new TransactionValidationException ("Transaction value out more than in", t);
			}
		}
	}

	private void addOwners (TxOut out) throws TransactionValidationException
	{
		List<Owner> owners = new ArrayList<Owner> ();
		parseOwners (out.getScript (), out, owners);
		out.setOwners (owners);
	}

	private void parseOwners (byte[] script, TxOut out, List<Owner> owners) throws TransactionValidationException
	{
		List<Script.Token> parsed;
		try
		{
			parsed = Script.parse (out.getScript ());
			if ( parsed.size () == 2 && parsed.get (0).data != null && parsed.get (1).op == Opcode.OP_CHECKSIG )
			{
				// pay to key
				Owner o = new Owner ();
				o.setAddress (AddressConverter.toSatoshiStyle (Hash.keyHash (parsed.get (0).data), false, chain));
				o.setOutpoint (out);
				owners.add (o);
				out.setVotes (1L);
			}
			if ( parsed.size () == 5 && parsed.get (0).op == Opcode.OP_DUP && parsed.get (1).op == Opcode.OP_HASH160 && parsed.get (2).data != null
					&& parsed.get (3).op == Opcode.OP_EQUALVERIFY && parsed.get (4).op == Opcode.OP_CHECKSIG )
			{
				// pay to address
				Owner o = new Owner ();
				o.setAddress (AddressConverter.toSatoshiStyle (parsed.get (2).data, false, chain));
				o.setOutpoint (out);
				owners.add (o);
				out.setVotes (1L);
			}
			if ( parsed.size () == 3 && parsed.get (0).op == Opcode.OP_HASH160 && parsed.get (1).data != null && parsed.get (1).data.length == 20
					&& parsed.get (2).op == Opcode.OP_EQUAL )
			{
				// pay to script
			}
			for ( int i = 0; i < parsed.size (); ++i )
			{
				if ( parsed.get (i).op == Opcode.OP_CHECKMULTISIG || parsed.get (i).op == Opcode.OP_CHECKMULTISIGVERIFY )
				{
					if ( parsed.get (i - 1).data != null ) // happens only on testnet
					{
						int nkeys = Script.intValue (parsed.get (i - 1).data);
						for ( int j = 0; j < nkeys; ++j )
						{
							Owner o = new Owner ();
							o.setAddress (AddressConverter.toSatoshiStyle (Hash.keyHash (parsed.get (i - j - 2).data), true, chain));
							o.setOutpoint (out);
							owners.add (o);
						}
						out.setVotes ((long) Script.intValue (parsed.get (i - nkeys - 2).data));
						return;
					}
				}
			}
		}
		catch ( ValidationException e )
		{
			throw new TransactionValidationException (e, out.getTransaction ());
		}
	}

	@Override
	public String getHeadHash ()
	{
		try
		{
			lock.readLock ().lock ();

			return currentHead.getLast ().getHash ();
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	@Override
	public boolean isEmpty ()
	{
		try
		{
			lock.readLock ().lock ();

			QHead head = QHead.head;
			JPAQuery q = new JPAQuery (entityManager);
			return q.from (head).list (head).isEmpty ();
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}

	@Transactional (propagation = Propagation.REQUIRED, rollbackFor = { Exception.class })
	@Override
	public void resetStore (Chain chain) throws TransactionValidationException
	{
		Blk genesis = chain.getGenesis ();
		addOwners (genesis.getTransactions ().get (0).getOutputs ().get (0));
		Head h = new Head ();
		h.setLeaf (genesis.getHash ());
		h.setHeight (0);
		h.setChainWork (Difficulty.getDifficulty (genesis.getDifficultyTarget ()));
		entityManager.persist (h);
		genesis.setHead (h);
		entityManager.persist (genesis);
	}

	@Transactional (propagation = Propagation.MANDATORY)
	@Override
	public Blk getBlock (String hash)
	{

		CachedBlock cached = null;
		try
		{
			lock.readLock ().lock ();
			cached = cachedBlocks.get (hash);
			if ( cached == null )
			{
				return null;
			}
		}
		finally
		{
			lock.readLock ().unlock ();
		}
		return entityManager.find (Blk.class, cached.getId ());
	}

	@Transactional (propagation = Propagation.REQUIRED)
	@Override
	public void validateTransaction (Tx t) throws ValidationException
	{
		try
		{
			lock.readLock ().lock ();

			TransactionContext tcontext = new TransactionContext ();
			tcontext.block = null;
			tcontext.transactionsOutputCache = new HashMap<String, ArrayList<TxOut>> ();
			tcontext.coinbase = false;
			tcontext.nsigs = 0;
			resolveInputs (tcontext, t);
			validateTransaction (tcontext, t);
		}
		finally
		{
			lock.readLock ().unlock ();
		}
	}
}
