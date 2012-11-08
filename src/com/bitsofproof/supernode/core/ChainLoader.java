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
package com.bitsofproof.supernode.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitsofproof.supernode.messages.BitcoinMessageListener;
import com.bitsofproof.supernode.messages.BlockMessage;
import com.bitsofproof.supernode.messages.GetBlocksMessage;
import com.bitsofproof.supernode.messages.GetDataMessage;
import com.bitsofproof.supernode.messages.InvMessage;
import com.bitsofproof.supernode.model.Blk;
import com.bitsofproof.supernode.model.BlockStore;

public class ChainLoader
{
	private static final Logger log = LoggerFactory.getLogger (ChainLoader.class);

	private int timeout = 30;

	private final Map<BitcoinPeer, TreeSet<KnownBlock>> knownInventory = new HashMap<BitcoinPeer, TreeSet<KnownBlock>> ();
	private final Map<BitcoinPeer, HashSet<String>> requests = new HashMap<BitcoinPeer, HashSet<String>> ();
	private final Map<BitcoinPeer, Long> busy = new HashMap<BitcoinPeer, Long> ();
	private final Map<String, Blk> pending = Collections.synchronizedMap (new HashMap<String, Blk> ());

	private class KnownBlock
	{
		private int nr;
		private String hash;
	}

	public boolean isBehind ()
	{
		synchronized ( knownInventory )
		{
			return !busy.isEmpty ();
		}
	}

	public ChainLoader (final BitcoinNetwork network)
	{
		final BlockStore store = network.getStore ();

		network.addListener ("block", new BitcoinMessageListener<BlockMessage> ()
		{
			@Override
			public void process (BlockMessage m, BitcoinPeer peer)
			{
				Blk block = m.getBlock ();
				log.trace ("received block " + block.getHash () + " from " + peer.getAddress ());
				if ( store.isStoredBlock (block.getPreviousHash ()) )
				{
					try
					{
						store.storeBlock (block);
						while ( pending.containsKey (block.getHash ()) )
						{
							block = pending.get (block.getHash ());
							store.storeBlock (block);
						}
					}
					catch ( ValidationException e )
					{
						log.trace ("Rejecting block " + block.getHash () + " from " + peer.getAddress (), e);
					}
				}
				else
				{
					pending.put (block.getPreviousHash (), block);
				}
				HashSet<String> peerRequests;
				synchronized ( knownInventory )
				{
					peerRequests = requests.get (peer);
					if ( peerRequests == null )
					{
						return;
					}
					peerRequests.remove (block.getHash ());
				}
				if ( peerRequests.isEmpty () )
				{
					Long job = busy.get (peer);
					if ( job != null )
					{
						network.cancelJob (job);
						busy.remove (peer);
					}
					if ( peer.getHeight () > store.getChainHeight () )
					{
						getBlockInventory (network, store, peer);
					}
				}
			}
		});

		network.addListener ("inv", new BitcoinMessageListener<InvMessage> ()
		{
			@Override
			public void process (InvMessage m, BitcoinPeer peer)
			{
				if ( !m.getBlockHashes ().isEmpty () )
				{
					log.trace ("received inventory of " + m.getBlockHashes ().size () + " blocks from " + peer.getAddress ());
					synchronized ( knownInventory )
					{
						Set<KnownBlock> k = knownInventory.get (peer);
						if ( k == null )
						{
							return; // disconnected peer
						}
						int n = k.size ();
						for ( byte[] h : m.getBlockHashes () )
						{
							String hash = new Hash (h).toString ();
							if ( !store.isStoredBlock (hash) )
							{
								KnownBlock kn = new KnownBlock ();
								kn.nr = n++;
								kn.hash = hash;
								k.add (kn);
							}
						}
					}
					getBlocks (network, peer);
				}
			}
		});

		network.addPeerListener (new BitcoinPeerListener ()
		{

			@Override
			public void remove (final BitcoinPeer peer)
			{
				synchronized ( knownInventory )
				{
					knownInventory.remove (peer);
					requests.remove (peer);
					busy.remove (peer);
				}
			}

			@Override
			public void add (BitcoinPeer peer)
			{
				synchronized ( knownInventory )
				{
					knownInventory.notify ();
					requests.put (peer, new HashSet<String> ());
					knownInventory.put (peer, new TreeSet<KnownBlock> (incomingOrder));
				}
				if ( peer.getHeight () > store.getChainHeight () )
				{
					getBlockInventory (network, store, peer);
				}
			}
		});
	}

	public int getTimeout ()
	{
		return timeout;
	}

	public void setTimeout (int inventoryTimeout)
	{
		this.timeout = inventoryTimeout;
	}

	private void getBlocks (final BitcoinNetwork network, final BitcoinPeer peer)
	{
		GetDataMessage gdm = (GetDataMessage) peer.createMessage ("getdata");
		synchronized ( knownInventory )
		{
			if ( busy.containsKey (peer) )
			{
				return;
			}
			for ( KnownBlock b : knownInventory.get (peer) )
			{
				boolean askedElswhere = false;
				for ( Set<String> ps : requests.values () )
				{
					if ( ps.contains (b.hash) )
					{
						askedElswhere = true;
						break;
					}
				}
				if ( !askedElswhere )
				{
					gdm.getBlocks ().add (new Hash (b.hash).toByteArray ());
					requests.get (peer).add (b.hash);
				}
			}
			knownInventory.get (peer).clear ();
		}
		if ( gdm.getBlocks ().size () > 0 )
		{
			peer.send (gdm);
			log.trace ("asking for " + gdm.getBlocks ().size () + " blocks from " + peer.getAddress ());
			busy.put (peer, network.scheduleJob (new Runnable ()
			{
				@Override
				public void run ()
				{
					log.trace ("Peer did not answer to getblock requests " + peer.getAddress ());
					peer.disconnect ();
				}
			}, timeout, TimeUnit.SECONDS));
		}
	}

	private void getBlockInventory (final BitcoinNetwork network, final BlockStore store, final BitcoinPeer peer)
	{
		GetBlocksMessage gbm = (GetBlocksMessage) peer.createMessage ("getblocks");
		for ( String h : store.getLocator () )
		{
			gbm.getHashes ().add (new Hash (h).toByteArray ());
		}
		gbm.setLastHash (Hash.ZERO_HASH.toByteArray ());
		if ( !gbm.getHashes ().isEmpty () )
		{
			log.trace ("Sending inventory request to " + peer.getAddress ());
			peer.send (gbm);
		}
	}

	private static final Comparator<KnownBlock> incomingOrder = new Comparator<KnownBlock> ()
	{
		@Override
		public int compare (KnownBlock arg0, KnownBlock arg1)
		{
			int diff = arg0.nr - arg1.nr;
			if ( diff != 0 )
			{
				return diff;
			}
			else
			{
				return arg0.equals (arg1) ? 0 : arg0.hashCode () - arg1.hashCode ();
			}
		}
	};
}
