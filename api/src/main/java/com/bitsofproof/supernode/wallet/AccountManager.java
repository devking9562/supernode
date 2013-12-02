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
package com.bitsofproof.supernode.wallet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bitsofproof.supernode.api.Address;
import com.bitsofproof.supernode.api.BCSAPI;
import com.bitsofproof.supernode.api.BCSAPIException;
import com.bitsofproof.supernode.api.Transaction;
import com.bitsofproof.supernode.api.TransactionListener;
import com.bitsofproof.supernode.api.TransactionOutput;
import com.bitsofproof.supernode.common.Key;
import com.bitsofproof.supernode.common.ValidationException;

public interface AccountManager extends TransactionListener
{
	public interface UTXO
	{

		public void add (TransactionOutput out);

		public Collection<TransactionOutput> getUTXO ();

		public TransactionOutput get (String tx, long ix);

		public TransactionOutput remove (String tx, long ix);

		public long getTotal ();

	}

	public void sync (BCSAPI api) throws BCSAPIException;

	public void syncHistory (BCSAPI api) throws BCSAPIException;

	public long getCreated ();

	public Key getNextKey () throws ValidationException;

	public Address getNextAddress () throws ValidationException;

	public Key getKeyForAddress (Address address);

	public Set<Address> getAddresses ();

	public Transaction pay (Address receiver, long amount, long fee, boolean senderPaysFee) throws ValidationException;

	public Transaction pay (Address receiver, long amount, boolean senderPaysFee) throws ValidationException;

	public Transaction pay (List<Address> receiver, List<Long> amount, long fee, boolean senderPaysFee) throws ValidationException;

	public Transaction pay (List<Address> receiver, List<Long> amounts, boolean senderPaysFee) throws ValidationException;

	public long getBalance ();

	public long getConfirmed ();

	public long getSending ();

	public long getReceiving ();

	public long getChange ();

	public Collection<TransactionOutput> getConfirmedOutputs ();

	public Collection<TransactionOutput> getSendingOutputs ();

	public Collection<TransactionOutput> getReceivingOutputs ();

	public Collection<TransactionOutput> getChangeOutputs ();

	public List<Transaction> getTransactions ();

	public void addAccountListener (AccountListener listener);

	public void removeAccountListener (AccountListener listener);
}