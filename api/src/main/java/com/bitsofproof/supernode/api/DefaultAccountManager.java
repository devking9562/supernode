/*
 * Copyright 2013 bits of proof zrt.
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

class DefaultAccountManager // implements TransactionListener, TrunkListener, AccountManager
{
	// private static final Logger log = LoggerFactory.getLogger (ClientBusAdaptor.class);
	//
	// private static final long DUST = 10000;
	//
	// private BCSAPI api;
	// private final LocalUTXO utxo = new LocalUTXO ();
	// private final HashMap<String, Long> colorBalances = new HashMap<String, Long> ();
	// private long balance = 0;
	// private BloomFilter filter;
	// private final List<Posting> postings = new ArrayList<Posting> ();
	// private final Set<String> walletAddresses = new HashSet<String> ();
	// private final List<AccountListener> accountListener = Collections.synchronizedList (new ArrayList<AccountListener> ());
	// private final Set<String> received = Collections.synchronizedSet (new HashSet<String> ());
	// private final Wallet wallet;
	//
	// public void setApi (BCSAPI api)
	// {
	// this.api = api;
	// }
	//
	// @Override
	// public void track (List<String> addresses)
	// {
	// try
	// {
	// walletAddresses.addAll (addresses);
	//
	// AccountStatement s = api.getAccountStatement (walletAddresses, 0);
	// if ( s != null )
	// {
	// if ( s.getOpening () != null )
	// {
	// for ( TransactionOutput o : s.getOpening () )
	// {
	// if ( o.getColor () == null )
	// {
	// balance += o.getValue ();
	// }
	// else
	// {
	// Long b = colorBalances.get (o.getColor ());
	// if ( b == null )
	// {
	// colorBalances.put (o.getColor (), o.getValue ());
	// }
	// else
	// {
	// colorBalances.put (o.getColor (), b.longValue () + o.getValue ());
	// }
	// }
	// utxo.add (o);
	// }
	// }
	// if ( s.getPosting () != null )
	// {
	// postings.addAll (s.getPosting ());
	// for ( Posting p1 : s.getPosting () )
	// {
	// if ( p1.getSpent () == null )
	// {
	// TransactionOutput o = p1.getOutput ();
	// if ( o.getColor () == null )
	// {
	// balance += o.getValue ();
	// }
	// else
	// {
	// Long b = colorBalances.get (o.getColor ());
	// if ( b == null )
	// {
	// colorBalances.put (o.getColor (), o.getValue ());
	// }
	// else
	// {
	// colorBalances.put (o.getColor (), b.longValue () + o.getValue ());
	// }
	// }
	// utxo.add (p1.getOutput ());
	// }
	// else
	// {
	// TransactionOutput o = p1.getOutput ();
	// if ( o.getColor () == null )
	// {
	// balance -= o.getValue ();
	// }
	// else
	// {
	// Long b = colorBalances.get (o.getColor ());
	// if ( b == null )
	// {
	// colorBalances.put (o.getColor (), -o.getValue ());
	// }
	// else
	// {
	// colorBalances.put (o.getColor (), b.longValue () - o.getValue ());
	// }
	// }
	// utxo.remove (p1.getOutput ());
	// }
	// }
	// }
	// if ( s.getUnconfirmedReceive () != null )
	// {
	// for ( Transaction t : s.getUnconfirmedReceive () )
	// {
	// updateWithTransaction (t, null);
	// }
	// }
	// if ( s.getUnconfirmedSpend () != null )
	// {
	// for ( Transaction t : s.getUnconfirmedReceive () )
	// {
	// updateWithTransaction (t, null);
	// }
	// }
	// }
	//
	// filter = BloomFilter.createOptimalFilter (addresses.size (), 0.01, UpdateMode.all);
	// for ( String a : walletAddresses )
	// {
	// filter.addAddress (a, 0x0); // TODO: fix addres flag
	// }
	// utxo.addOutpoints (filter);
	// api.registerFilteredListener (filter, this);
	// api.registerTrunkListener (this);
	// }
	// catch ( ValidationException e )
	// {
	// log.error ("Error extracting wallet adresses", e);
	// }
	// catch ( BCSAPIException e )
	// {
	// log.error ("Error talking to server", e);
	// }
	//
	// }
	//
	// @Override
	// public Transaction pay (String receiver, long amount, long fee) throws ValidationException, BCSAPIException
	// {
	// synchronized ( utxo )
	// {
	// List<TransactionSource> sources = new ArrayList<TransactionSource> ();
	// List<TransactionSink> sinks = new ArrayList<TransactionSink> ();
	//
	// List<TransactionOutput> sufficient = utxo.getSufficientSources (amount, fee, null);
	// if ( sufficient == null )
	// {
	// throw new ValidationException ("Insufficient funds to pay " + (amount + fee));
	// }
	// long in = 0;
	// for ( TransactionOutput o : sufficient )
	// {
	// sources.add (new TransactionSource (o, wallet.getKeyForAddress (o.getAddresses ().get (0))));
	// in += o.getValue ();
	// }
	// TransactionSink target = new TransactionSink (AddressConverter.fromSatoshiStyle (receiver, wallet.getAddressFlag ()), amount);
	// if ( (in - amount - fee) > 0 )
	// {
	// TransactionSink change = new TransactionSink (wallet.getRandomKey ().getAddress (), in - amount - fee);
	// if ( new SecureRandom ().nextBoolean () )
	// {
	// sinks.add (target);
	// sinks.add (change);
	// }
	// else
	// {
	// sinks.add (change);
	// sinks.add (target);
	// }
	// }
	// else
	// {
	// sinks.add (target);
	// }
	// return Transaction.createSpend (sources, sinks, fee);
	// }
	// }
	//
	// @Override
	// public Transaction split (long[] amounts, long fee) throws ValidationException, BCSAPIException
	// {
	// synchronized ( utxo )
	// {
	// List<TransactionSource> sources = new ArrayList<TransactionSource> ();
	// List<TransactionSink> sinks = new ArrayList<TransactionSink> ();
	//
	// long amount = 0;
	// for ( long a : amounts )
	// {
	// amount += a;
	// }
	// List<TransactionOutput> sufficient = utxo.getSufficientSources (amount, fee, null);
	// if ( sufficient == null )
	// {
	// throw new ValidationException ("Insufficient funds to pay " + (amount + fee));
	// }
	// long in = 0;
	// for ( TransactionOutput o : sufficient )
	// {
	// sources.add (new TransactionSource (o, wallet.getKeyForAddress (o.getAddresses ().get (0))));
	// in += o.getValue ();
	// }
	// for ( long a : amounts )
	// {
	// TransactionSink target = new TransactionSink (wallet.getRandomKey ().getAddress (), a);
	// sinks.add (target);
	// }
	// if ( (in - amount - fee) > 0 )
	// {
	// TransactionSink change = new TransactionSink (wallet.getRandomKey ().getAddress (), in - amount - fee);
	// sinks.add (change);
	// }
	// Collections.shuffle (sinks);
	// return Transaction.createSpend (sources, sinks, fee);
	// }
	// }
	//
	// @Override
	// public Transaction transfer (String receiver, long units, long fee, Color color) throws ValidationException, BCSAPIException
	// {
	// synchronized ( utxo )
	// {
	// List<TransactionSource> sources = new ArrayList<TransactionSource> ();
	// List<TransactionSink> sinks = new ArrayList<TransactionSink> ();
	//
	// long amount = units * color.getUnit ();
	// List<TransactionOutput> sufficient = utxo.getSufficientSources (amount, fee, color.getTransaction ());
	// if ( sufficient == null )
	// {
	// throw new ValidationException ("Insufficient holdings to transfer " + units + " of " + color.getTransaction ());
	// }
	// long in = 0;
	// long colorIn = 0;
	// for ( TransactionOutput o : sufficient )
	// {
	// sources.add (new TransactionSource (o, wallet.getKeyForAddress (o.getAddresses ().get (0))));
	// in += o.getValue ();
	// if ( color != null && o.getColor ().equals (color) )
	// {
	// colorIn += o.getValue ();
	// }
	// }
	// TransactionSink target = new TransactionSink (AddressConverter.fromSatoshiStyle (receiver, wallet.getAddressFlag ()), amount);
	// if ( colorIn > amount )
	// {
	// TransactionSink colorChange = new TransactionSink (wallet.getRandomKey ().getAddress (), colorIn - amount);
	// if ( new SecureRandom ().nextBoolean () )
	// {
	// sinks.add (target);
	// sinks.add (colorChange);
	// }
	// else
	// {
	// sinks.add (colorChange);
	// sinks.add (target);
	// }
	// }
	// else
	// {
	// sinks.add (target);
	// }
	// sinks.add (new TransactionSink (wallet.getRandomKey ().getAddress (), in - amount - fee));
	// return Transaction.createSpend (sources, sinks, fee);
	// }
	// }
	//
	// @Override
	// public Transaction createColorGenesis (long quantity, long unitSize, long fee) throws ValidationException, BCSAPIException
	// {
	// synchronized ( utxo )
	// {
	// List<TransactionSource> sources = new ArrayList<TransactionSource> ();
	// List<TransactionSink> sinks = new ArrayList<TransactionSink> ();
	//
	// List<TransactionOutput> sufficient = utxo.getSufficientSources (quantity * unitSize, fee, null);
	// if ( sufficient == null )
	// {
	// throw new ValidationException ("Insufficient funds to issue color ");
	// }
	// long in = 0;
	// for ( TransactionOutput o : sufficient )
	// {
	// sources.add (new TransactionSource (o, wallet.getKeyForAddress (o.getAddresses ().get (0))));
	// in += o.getValue ();
	// }
	// Key issuerKey = wallet.getRandomKey ();
	// TransactionSink target = new TransactionSink (issuerKey.getAddress (), quantity * unitSize);
	// TransactionSink change = new TransactionSink (wallet.getRandomKey ().getAddress (), in - quantity * unitSize - fee);
	// sinks.add (target);
	// sinks.add (change);
	// return Transaction.createSpend (sources, sinks, fee);
	// }
	// }
	//
	// @Override
	// public Transaction cashIn (String serialized, String passpharse, long fee) throws ValidationException, BCSAPIException
	// {
	// synchronized ( utxo )
	// {
	// KeyFormatter formatter = new KeyFormatter (passpharse, wallet.getAddressFlag ());
	// Key key = formatter.parseSerializedKey (serialized);
	// String inAddress = AddressConverter.toSatoshiStyle (key.getAddress (), wallet.getAddressFlag ());
	// List<TransactionOutput> available = utxo.getAddressSources (inAddress);
	// if ( available.size () > 0 )
	// {
	// long sum = 0;
	// List<TransactionSource> sources = new ArrayList<TransactionSource> ();
	// for ( TransactionOutput o : available )
	// {
	// sources.add (new TransactionSource (o, key));
	// sum += o.getValue ();
	// }
	// List<TransactionSink> sinks = new ArrayList<TransactionSink> ();
	// // make it look like a spend
	// long a = Math.max ((((sum - fee) - Math.abs ((new SecureRandom ().nextLong () % (sum - fee)))) / DUST) * DUST, DUST);
	// long b = (sum - fee) - a;
	// sinks.add (new TransactionSink (wallet.getRandomKey ().getAddress (), a));
	// sinks.add (new TransactionSink (wallet.getRandomKey ().getAddress (), b));
	// return Transaction.createSpend (sources, sinks, fee);
	// }
	// throw new ValidationException ("No input available on this key");
	// }
	// }
	//
	// @Override
	// public void process (Transaction t)
	// {
	// if ( updateWithTransaction (t, null) )
	// {
	// notifyListener ();
	// }
	// }
	//
	// private boolean updateWithTransaction (Transaction t, Block b)
	// {
	// boolean notify = false;
	//
	// synchronized ( utxo )
	// {
	// for ( TransactionInput input : t.getInputs () )
	// {
	// TransactionOutput output = utxo.get (input.getSourceHash (), input.getIx ());
	// if ( output != null )
	// {
	// balance -= output.getValue ();
	// Posting p = new Posting ();
	// p.setOutput (output);
	// if ( b != null )
	// {
	// p.setBlock (b.getHash ());
	// p.setTimestamp (b.getCreateTime ());
	// }
	// else
	// {
	// p.setTimestamp (System.currentTimeMillis () / 1000);
	// }
	// p.setSpent (t.getHash ());
	// postings.add (p);
	// utxo.remove (input.getSourceHash (), input.getIx ());
	// notify = true;
	// }
	// }
	// if ( !received.contains (t.getHash ()) )
	// {
	// for ( TransactionOutput output : t.getOutputs () )
	// {
	// output.parseOwners (wallet.getAddressFlag (), 0x05);
	// for ( String address : output.getAddresses () )
	// {
	// if ( walletAddresses.contains (address) )
	// {
	// Posting p = new Posting ();
	// p.setOutput (output);
	// if ( b != null )
	// {
	// p.setBlock (b.getHash ());
	// p.setTimestamp (b.getCreateTime ());
	// }
	// else
	// {
	// p.setTimestamp (System.currentTimeMillis () / 1000);
	// }
	// postings.add (p);
	// utxo.add (output);
	// balance += output.getValue ();
	// notify = true;
	// }
	// }
	// }
	// received.add (t.getHash ());
	// }
	// }
	// return notify;
	// }
	//
	// @Override
	// public long getBalance ()
	// {
	// synchronized ( utxo )
	// {
	// return balance;
	// }
	// }
	//
	// @Override
	// public long getBalance (Color color)
	// {
	// synchronized ( utxo )
	// {
	// Long balance = colorBalances.get (color.getFungibleName ());
	// if ( balance != null )
	// {
	// return balance.longValue () / color.getUnit ();
	// }
	// }
	// return 0;
	// }
	//
	// @Override
	// public List<String> getColors ()
	// {
	// List<String> cols = new ArrayList<String> ();
	// cols.addAll (colorBalances.keySet ());
	// return cols;
	// }
	//
	// @Override
	// public void addAccountListener (AccountListener listener)
	// {
	// accountListener.add (listener);
	// }
	//
	// @Override
	// public void removeAccountListener (AccountListener listener)
	// {
	// accountListener.remove (listener);
	// }
	//
	// private void notifyListener ()
	// {
	// synchronized ( accountListener )
	// {
	// for ( AccountListener l : accountListener )
	// {
	// try
	// {
	// l.accountChanged (this);
	// }
	// catch ( Exception e )
	// {
	// }
	// }
	// }
	// }
	//
	// @Override
	// public void trunkUpdate (List<Block> removed, List<Block> added)
	// {
	// boolean notify = false;
	// synchronized ( utxo )
	// {
	// if ( added != null )
	// {
	// for ( Block block : added )
	// {
	// for ( Transaction t : block.getTransactions () )
	// {
	// notify |= updateWithTransaction (t, block);
	// }
	// }
	// }
	// }
	// if ( notify )
	// {
	// notifyListener ();
	// }
	// }
}
