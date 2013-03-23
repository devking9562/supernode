/*
 * Copyright 2013 Tamas Blummer tamas@bitsofproof.com
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
package com.bitsofproof.supernode.api;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitsofproof.supernode.api.Transaction.TransactionSink;
import com.bitsofproof.supernode.api.Transaction.TransactionSource;

public class AccountManager implements WalletListener, TransactionListener, TrunkListener
{
	private static final Logger log = LoggerFactory.getLogger (ClientBusAdaptor.class);

	private class UTXO
	{
		private final Map<String, HashMap<Long, TransactionOutput>> utxo = new HashMap<String, HashMap<Long, TransactionOutput>> ();

		public void clear ()
		{
			utxo.clear ();
		}

		public void add (TransactionOutput out)
		{
			HashMap<Long, TransactionOutput> outs = utxo.get (out.getTransactionHash ());
			if ( outs == null )
			{
				outs = new HashMap<Long, TransactionOutput> ();
				utxo.put (out.getTransactionHash (), outs);
			}
			outs.put (out.getSelfIx (), out);
		}

		private TransactionOutput get (String tx, Long ix)
		{
			HashMap<Long, TransactionOutput> outs = utxo.get (tx);
			if ( outs != null )
			{
				return outs.get (ix);
			}
			return null;
		}

		private void remove (String tx, Long ix)
		{
			HashMap<Long, TransactionOutput> outs = utxo.get (tx);
			if ( outs != null )
			{
				outs.remove (ix);
				if ( outs.size () == 0 )
				{
					utxo.remove (tx);
				}
			}
		}

		private void remove (TransactionOutput out)
		{
			remove (out.getTransactionHash (), out.getSelfIx ());
		}

		private Collection<String> getTransactionHashes ()
		{
			return utxo.keySet ();
		}

		private List<TransactionOutput> getSufficientSources (long amount, long fee, String color)
		{
			List<TransactionOutput> result = new ArrayList<TransactionOutput> ();
			long sum = 0;
			for ( HashMap<Long, TransactionOutput> outs : utxo.values () )
			{
				for ( TransactionOutput o : outs.values () )
				{
					if ( o.getVotes () == 1 && o.getAddresses ().size () == 1 )
					{
						if ( color == null )
						{
							if ( o.getColors () == null )
							{
								sum += o.getValue ();
								result.add (o);
								if ( sum >= (amount + fee) )
								{
									return result;
								}
							}
						}
						else
						{
							if ( o.getColors ().contains (color) )
							{
								sum += o.getValue ();
								result.add (o);
								if ( sum >= amount )
								{
									if ( fee > 0 )
									{
										result.addAll (getSufficientSources (0, fee, null));
									}
									return result;
								}
							}
						}
					}
				}
			}
			return null;
		}

		private List<TransactionOutput> getAddressSources (String address)
		{
			List<TransactionOutput> result = new ArrayList<TransactionOutput> ();
			for ( HashMap<Long, TransactionOutput> outs : utxo.values () )
			{
				for ( TransactionOutput o : outs.values () )
				{
					if ( o.getVotes () == 1 && o.getAddresses ().size () == 1 && o.getAddresses ().get (0).equals (address) )
					{
						result.add (o);
					}
				}
			}
			return result;
		}

	}

	private BCSAPI api;
	private final UTXO utxo = new UTXO ();
	private long balance = 0;
	private Wallet wallet;
	private final List<Posting> postings = new ArrayList<Posting> ();
	private final Set<String> walletAddresses = new HashSet<String> ();
	private final List<AccountListener> accountListener = Collections.synchronizedList (new ArrayList<AccountListener> ());
	private final Set<String> received = Collections.synchronizedSet (new HashSet<String> ());

	public void setApi (BCSAPI api)
	{
		this.api = api;
	}

	public void track (Wallet wallet)
	{
		try
		{
			this.wallet = wallet;
			walletAddresses.addAll (wallet.getAddresses ());
			trackAddresses (walletAddresses);
			wallet.addListener (this);
			api.registerTrunkListener (this);
		}
		catch ( ValidationException e )
		{
			log.error ("Error extracting wallet adresses", e);
		}
		catch ( BCSAPIException e )
		{
			log.error ("Error talking to server", e);
		}

	}

	private AccountStatement trackAddresses (Collection<String> addresses) throws BCSAPIException
	{
		api.removeFilteredListener (walletAddresses, this);
		api.removeFilteredListener (utxo.getTransactionHashes (), this);
		balance = 0;
		postings.clear ();
		utxo.clear ();
		received.clear ();
		AccountStatement s = api.getAccountStatement (addresses, 0);
		if ( s != null )
		{
			if ( s.getOpening () != null )
			{
				for ( TransactionOutput o : s.getOpening () )
				{
					balance += o.getValue ();
					utxo.add (o);
				}
			}
			if ( s.getPosting () != null )
			{
				postings.addAll (s.getPosting ());
				for ( Posting p : s.getPosting () )
				{
					if ( p.getSpent () == null )
					{
						balance += p.getOutput ().getValue ();
						utxo.add (p.getOutput ());
					}
					else
					{
						balance -= p.getOutput ().getValue ();
						utxo.remove (p.getOutput ());
					}
				}
			}
			if ( s.getUnconfirmedReceive () != null )
			{
				for ( Transaction t : s.getUnconfirmedReceive () )
				{
					updateWithTransaction (t, null);
				}
			}
			if ( s.getUnconfirmedSpend () != null )
			{
				for ( Transaction t : s.getUnconfirmedReceive () )
				{
					updateWithTransaction (t, null);
				}
			}
			api.registerOutputListener (utxo.getTransactionHashes (), this);
		}
		api.registerAddressListener (addresses, this);
		return s;
	}

	public Transaction pay (String receiver, long amount, long fee) throws ValidationException, BCSAPIException
	{
		synchronized ( utxo )
		{
			List<TransactionSource> sources = new ArrayList<TransactionSource> ();
			List<TransactionSink> sinks = new ArrayList<TransactionSink> ();

			List<TransactionOutput> sufficient = utxo.getSufficientSources (amount, fee, null);
			if ( sufficient == null )
			{
				throw new ValidationException ("Insufficient funds to pay " + (amount + fee));
			}
			long in = 0;
			for ( TransactionOutput o : sufficient )
			{
				sources.add (new TransactionSource (o, wallet.getKeyForAddress (o.getAddresses ().get (0))));
				in += o.getValue ();
			}
			TransactionSink target = new TransactionSink (AddressConverter.fromSatoshiStyle (receiver, wallet.getAddressFlag ()), amount);
			TransactionSink change = new TransactionSink (wallet.generateNextKey ().getKey ().getAddress (), in - amount - fee);
			if ( new SecureRandom ().nextBoolean () )
			{
				sinks.add (target);
				sinks.add (change);
			}
			else
			{
				sinks.add (change);
				sinks.add (target);
			}
			return Transaction.createSpend (sources, sinks, fee);
		}
	}

	public Transaction transfer (String receiver, long units, long fee, Color color) throws ValidationException, BCSAPIException
	{
		synchronized ( utxo )
		{
			List<TransactionSource> sources = new ArrayList<TransactionSource> ();
			List<TransactionSink> sinks = new ArrayList<TransactionSink> ();

			long amount = units * color.getUnit ();
			List<TransactionOutput> sufficient = utxo.getSufficientSources (amount, fee, color.getHash ());
			if ( sufficient == null )
			{
				throw new ValidationException ("Insufficient holdings to transfer " + units + " of " + color.getHash ());
			}
			long in = 0;
			long colorIn = 0;
			for ( TransactionOutput o : sufficient )
			{
				sources.add (new TransactionSource (o, wallet.getKeyForAddress (o.getAddresses ().get (0))));
				in += o.getValue ();
				if ( color != null && o.getColors ().contains (color) )
				{
					colorIn += o.getValue ();
				}
			}
			TransactionSink target = new TransactionSink (AddressConverter.fromSatoshiStyle (receiver, wallet.getAddressFlag ()), amount);
			if ( colorIn > amount )
			{
				TransactionSink colorChange = new TransactionSink (wallet.generateNextKey ().getKey ().getAddress (), colorIn - amount);
				if ( new SecureRandom ().nextBoolean () )
				{
					sinks.add (target);
					sinks.add (colorChange);
				}
				else
				{
					sinks.add (colorChange);
					sinks.add (target);
				}
			}
			else
			{
				sinks.add (target);
			}
			sinks.add (new TransactionSink (wallet.generateNextKey ().getKey ().getAddress (), in - amount - fee));
			return Transaction.createSpend (sources, sinks, fee);
		}
	}

	public void issueColor (Color color) throws ValidationException, BCSAPIException
	{
		Key key = wallet.getKeyForAddress (AddressConverter.toSatoshiStyle (Hash.keyHash (color.getPubkey ()), wallet.getAddressFlag ()));
		if ( key == null )
		{
			throw new ValidationException ("Do not have key to issue color " + color.getHash ());
		}
		color.sign (key);
		api.issueColor (color);
	}

	public void importKey (String serialized, String passpharse) throws ValidationException
	{
		synchronized ( utxo )
		{
			KeyFormatter formatter = new KeyFormatter (passpharse, wallet.getAddressFlag ());
			Key key = formatter.parseSerializedKey (serialized);
			wallet.importKey (key);
		}
	}

	public Transaction cashIn (String serialized, String passpharse, long fee) throws ValidationException, BCSAPIException
	{
		synchronized ( utxo )
		{
			KeyFormatter formatter = new KeyFormatter (passpharse, wallet.getAddressFlag ());
			Key key = formatter.parseSerializedKey (serialized);
			wallet.importKey (key);
			String inAddress = AddressConverter.toSatoshiStyle (key.getAddress (), wallet.getAddressFlag ());
			List<TransactionOutput> available = utxo.getAddressSources (inAddress);
			if ( available.size () > 0 )
			{
				long sum = 0;
				List<TransactionSource> sources = new ArrayList<TransactionSource> ();
				for ( TransactionOutput o : available )
				{
					sources.add (new TransactionSource (o, key));
					sum += o.getValue ();
				}
				List<TransactionSink> sinks = new ArrayList<TransactionSink> ();
				sinks.add (new TransactionSink (wallet.generateNextKey ().getKey ().getAddress (), sum - fee));
				return Transaction.createSpend (sources, sinks, fee);
			}
			throw new ValidationException ("No input available on this key");
		}
	}

	@Override
	public void notifyNewKey (String address, Key key, boolean pristine)
	{
		if ( pristine )
		{
			walletAddresses.add (address);
			List<String> addresses = new ArrayList<String> ();
			addresses.add (address);
			try
			{
				api.registerAddressListener (addresses, this);
			}
			catch ( BCSAPIException e )
			{
				log.error ("Can not register listener for new key", e);
			}
		}
		else
		{
			boolean notify = false;
			synchronized ( utxo )
			{
				long oldBalance = balance;
				ArrayList<String> a = new ArrayList<String> ();
				a.add (address);
				try
				{
					api.removeFilteredListener (walletAddresses, this);
					api.removeFilteredListener (utxo.getTransactionHashes (), this);
					walletAddresses.add (address);
					trackAddresses (walletAddresses);
					notify = oldBalance != balance;
				}
				catch ( BCSAPIException e )
				{
					log.error ("Can not track new key " + address, e);
				}
			}
			if ( notify )
			{
				notifyListener ();
			}
		}
	}

	@Override
	public void process (Transaction t)
	{
		if ( updateWithTransaction (t, null) )
		{
			notifyListener ();
		}
	}

	public void process (Transaction t, Block b)
	{
		if ( updateWithTransaction (t, b) )
		{
			notifyListener ();
		}
	}

	private boolean updateWithTransaction (Transaction t, Block b)
	{
		boolean notify = false;

		synchronized ( utxo )
		{
			for ( TransactionInput input : t.getInputs () )
			{
				TransactionOutput output = utxo.get (input.getSourceHash (), input.getIx ());
				if ( output != null )
				{
					balance -= output.getValue ();
					Posting p = new Posting ();
					p.setOutput (output);
					if ( b != null )
					{
						p.setBlock (b.getHash ());
						p.setTimestamp (b.getCreateTime ());
					}
					else
					{
						p.setTimestamp (System.currentTimeMillis () / 1000);
					}
					p.setSpent (t.getHash ());
					postings.add (p);
					utxo.remove (input.getSourceHash (), input.getIx ());
					notify = true;
				}
			}
			if ( !received.contains (t.getHash ()) )
			{
				for ( TransactionOutput output : t.getOutputs () )
				{
					output.parseOwners (wallet.getAddressFlag (), wallet.getP2SHAddressFlag ());
					for ( String address : output.getAddresses () )
					{
						if ( walletAddresses.contains (address) )
						{
							Posting p = new Posting ();
							p.setOutput (output);
							if ( b != null )
							{
								p.setBlock (b.getHash ());
								p.setTimestamp (b.getCreateTime ());
							}
							else
							{
								p.setTimestamp (System.currentTimeMillis () / 1000);
							}
							postings.add (p);
							utxo.add (output);
							balance += output.getValue ();
							notify = true;
						}
					}
				}
				ArrayList<String> txs = new ArrayList<String> ();
				txs.add (t.getHash ());
				try
				{
					api.registerOutputListener (txs, this);
				}
				catch ( BCSAPIException e )
				{
				}
				received.add (t.getHash ());
			}
		}
		return notify;
	}

	public long getBalance ()
	{
		synchronized ( utxo )
		{
			return balance;
		}
	}

	public List<Posting> getPostings ()
	{
		synchronized ( utxo )
		{
			return postings;
		}
	}

	public void addAccountListener (AccountListener listener)
	{
		accountListener.add (listener);
	}

	public void removeAccountListener (AccountListener listener)
	{
		accountListener.remove (listener);
	}

	private void notifyListener ()
	{
		synchronized ( accountListener )
		{
			for ( AccountListener l : accountListener )
			{
				try
				{
					l.accountChanged (this);
				}
				catch ( Exception e )
				{
				}
			}
		}
	}

	@Override
	public void trunkUpdate (List<Block> removed, List<Block> added)
	{
		boolean notify = false;
		synchronized ( utxo )
		{
			if ( removed != null )
			{
				try
				{
					trackAddresses (walletAddresses);
					notify = true;
				}
				catch ( BCSAPIException e )
				{
					log.error ("Error reloading account after reorg", e);
				}
			}
			else if ( added != null )
			{
				for ( Block block : added )
				{
					for ( Transaction t : block.getTransactions () )
					{
						notify |= updateWithTransaction (t, block);
					}
				}
			}
		}
		if ( notify )
		{
			notifyListener ();
		}
	}
}
