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

import java.io.Serializable;

import com.bitsofproof.supernode.common.Hash;
import com.bitsofproof.supernode.common.WireFormat;
import com.google.protobuf.ByteString;

public class TransactionInput implements Serializable, Cloneable
{
	private static final long serialVersionUID = -7019826355856117874L;

	private String sourceHash;
	private long ix;
	private long sequence = 0xFFFFFFFFL;
	private byte[] script;

	public String getSourceHash ()
	{
		return sourceHash;
	}

	public void setSourceHash (String sourceHash)
	{
		this.sourceHash = sourceHash;
	}

	public long getIx ()
	{
		return ix;
	}

	public void setIx (long ix)
	{
		this.ix = ix;
	}

	public long getSequence ()
	{
		return sequence;
	}

	public void setSequence (long sequence)
	{
		this.sequence = sequence;
	}

	public byte[] getScript ()
	{
		if ( script != null )
		{
			byte[] copy = new byte[script.length];
			System.arraycopy (script, 0, copy, 0, script.length);
			return copy;
		}
		return null;
	}

	public void setScript (byte[] script)
	{
		if ( script != null )
		{
			this.script = new byte[script.length];
			System.arraycopy (script, 0, this.script, 0, script.length);
		}
		else
		{
			this.script = null;
		}
	}

	public void toWire (WireFormat.Writer writer)
	{
		if ( sourceHash != null && !sourceHash.equals (Hash.ZERO_HASH.toString ()) )
		{
			writer.writeHash (new Hash (sourceHash));
			writer.writeUint32 (ix);
		}
		else
		{
			writer.writeBytes (Hash.ZERO_HASH.toByteArray ());
			writer.writeUint32 (-1);
		}
		writer.writeVarBytes (script);
		writer.writeUint32 (sequence);
	}

	public static TransactionInput fromWire (WireFormat.Reader reader)
	{
		TransactionInput i = new TransactionInput ();

		i.sourceHash = reader.readHash ().toString ();
		i.ix = reader.readUint32 ();
		i.script = reader.readVarBytes ();
		i.sequence = reader.readUint32 ();

		return i;
	}

	@Override
	public TransactionInput clone () throws CloneNotSupportedException
	{
		TransactionInput i = (TransactionInput) super.clone ();

		i.sourceHash = sourceHash;
		i.ix = ix;
		i.sequence = sequence;
		if ( script != null )
		{
			i.script = new byte[script.length];
			System.arraycopy (script, 0, i.script, 0, script.length);
		}

		return i;
	}

	public BCSAPIMessage.TransactionInput toProtobuf ()
	{
		BCSAPIMessage.TransactionInput.Builder builder = BCSAPIMessage.TransactionInput.newBuilder ();
		builder.setScript (ByteString.copyFrom (script));
		builder.setSequence ((int) sequence);
		builder.setSource (ByteString.copyFrom (new Hash (sourceHash).toByteArray ()));
		builder.setSourceix ((int) ix);
		return builder.build ();
	}

	public static TransactionInput fromProtobuf (BCSAPIMessage.TransactionInput pi)
	{
		TransactionInput input = new TransactionInput ();
		input.setIx (pi.getSourceix ());
		input.setScript (pi.getScript ().toByteArray ());
		input.setSequence (pi.getSequence ());
		input.setSourceHash (new Hash (pi.getSource ().toByteArray ()).toString ());
		return input;
	}
}
