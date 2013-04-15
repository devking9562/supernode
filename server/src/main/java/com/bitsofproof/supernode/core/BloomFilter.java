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
package com.bitsofproof.supernode.core;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.util.Arrays;

import com.bitsofproof.supernode.api.ByteUtils;
import com.bitsofproof.supernode.api.WireFormat;

public class BloomFilter
{
	public static enum UpdateMode
	{
		none, all, keys
	}

	private final BigInteger filter;
	private final int mod;
	private final long hashFunctions;
	private final long tweak;
	private final UpdateMode update;

	private static final int MAX_FILTER_SIZE = 36000;
	private static final int MAX_HASH_FUNCS = 50;

	public static BloomFilter createOptimalFilter (int n, double probFalsePositive, UpdateMode update)
	{
		long tweak = Math.abs (new SecureRandom ().nextInt ());
		// http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives
		double ln2 = Math.log (2.0);
		int mod = Math.max (1, (int) Math.min ((-n * Math.log (probFalsePositive) / (ln2 * ln2)) / 8.0, MAX_FILTER_SIZE));
		long hashFunctions = Math.max (1, Math.min ((int) (mod * 8.0 / n * ln2), MAX_HASH_FUNCS));
		return new BloomFilter (new byte[mod], hashFunctions, tweak, update);
	}

	public BloomFilter (byte[] data, long hashFunctions, long tweak, UpdateMode update)
	{
		byte[] tmp = Arrays.copyOf (data, Math.min (data.length, MAX_FILTER_SIZE));
		mod = data.length * 8;
		this.filter = new BigInteger (1, ByteUtils.reverse (tmp));
		this.hashFunctions = Math.min (hashFunctions, MAX_HASH_FUNCS);
		this.tweak = tweak;
		this.update = update;
	}

	public void add (byte[] data)
	{
		for ( int i = 0; i < hashFunctions; ++i )
		{
			filter.setBit (murmurhash3bit (i, data));
		}
	}

	public boolean contains (byte[] data)
	{
		for ( int i = 0; i < hashFunctions; ++i )
		{
			if ( !filter.testBit (murmurhash3bit (i, data)) )
			{
				return false;
			}
		}
		return true;
	}

	private int murmurhash3bit (int hashNum, byte[] data)
	{
		return (int) ((murmurhash3 (data, 0, data.length, (int) (hashNum * 0xFBA4C795L + tweak)) & 0xFFFFFFFFL) % mod);
	}

	public void toWire (WireFormat.Writer writer)
	{
		byte[] data = filter.toByteArray ();
		ByteUtils.reverse (data);
		writer.writeVarInt (mod);
		writer.writeBytes (data);
		if ( data.length < mod )
		{
			byte[] fill = new byte[mod - data.length];
			writer.writeBytes (fill);
		}
		writer.writeUint32 (hashFunctions);
		writer.writeUint32 (tweak);
		writer.writeUint32 (update.ordinal ());
	}

	public static BloomFilter fromWire (WireFormat.Reader reader)
	{
		byte[] data = reader.readVarBytes ();
		long hashFunctions = reader.readUint32 ();
		long tweak = reader.readUint32 ();
		long update = reader.readUint32 ();
		return new BloomFilter (data, hashFunctions, tweak, UpdateMode.values ()[(int) update]);
	}

	/*
	 * This code is public domain.
	 * 
	 * The MurmurHash3 algorithm was created by Austin Appleby and put into the public domain. See http://code.google.com/p/smhasher/
	 * 
	 * This java port was authored by Yonik Seeley and was placed into the public domain per
	 * https://github.com/yonik/java_util/blob/master/src/util/hash/MurmurHash3.java.
	 */
	private static int murmurhash3 (byte[] data, int offset, int len, int seed)
	{
		int c1 = 0xcc9e2d51;
		int c2 = 0x1b873593;

		int h1 = seed;
		int roundedEnd = offset + (len & 0xfffffffc); // round down to 4 byte block

		for ( int i = offset; i < roundedEnd; i += 4 )
		{
			// little endian load order
			int k1 = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
			k1 *= c1;
			k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
			k1 *= c2;

			h1 ^= k1;
			h1 = (h1 << 13) | (h1 >>> 19); // ROTL32(h1,13);
			h1 = h1 * 5 + 0xe6546b64;
		}

		// tail
		int k1 = 0;

		switch ( len & 0x03 )
		{
			case 3:
				k1 = (data[roundedEnd + 2] & 0xff) << 16;
				// fallthrough
			case 2:
				k1 |= (data[roundedEnd + 1] & 0xff) << 8;
				// fallthrough
			case 1:
				k1 |= data[roundedEnd] & 0xff;
				k1 *= c1;
				k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
				k1 *= c2;
				h1 ^= k1;
			default:
		}

		// finalization
		h1 ^= len;

		// fmix(h1);
		h1 ^= h1 >>> 16;
		h1 *= 0x85ebca6b;
		h1 ^= h1 >>> 13;
		h1 *= 0xc2b2ae35;
		h1 ^= h1 >>> 16;

		return h1;
	}

	public BigInteger getFilter ()
	{
		return filter;
	}

	public long getHashFunctions ()
	{
		return hashFunctions;
	}

	public long getTweak ()
	{
		return tweak;
	}

	public UpdateMode getUpdateMode ()
	{
		return update;
	}
}
