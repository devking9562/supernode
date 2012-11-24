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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.bitsofproof.supernode.model.Blk;
import com.bitsofproof.supernode.model.Tx;

public class WireFormatTest
{

	@Test
	public void testUint16 ()
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream ();
		WireFormat.Writer writer = new WireFormat.Writer (bs);
		writer.writeUint16 (21845);

		WireFormat.Reader reader = new WireFormat.Reader (bs.toByteArray ());
		assertEquals (reader.readUint16 (), 21845l);
		assertTrue (reader.eof ());
	}

	@Test
	public void testUint32 ()
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream ();
		WireFormat.Writer writer = new WireFormat.Writer (bs);
		writer.writeUint32 (0xD9B4BEF9l);

		WireFormat.Reader reader = new WireFormat.Reader (bs.toByteArray ());
		assertEquals (reader.readUint32 (), 0xD9B4BEF9l);
		assertTrue (reader.eof ());
	}

	@Test
	public void testUint64 ()
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream ();
		WireFormat.Writer writer = new WireFormat.Writer (bs);
		long n = 286331153L * 286331153;
		writer.writeUint64 (n);

		WireFormat.Reader reader = new WireFormat.Reader (bs.toByteArray ());
		assertEquals (reader.readUint64 (), n);
		assertTrue (reader.eof ());
	}

	@Test
	public void testVarInt ()
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream ();
		WireFormat.Writer writer = new WireFormat.Writer (bs);
		writer.writeVarInt (286331153);
		writer.writeVarInt (1153);
		writer.writeVarInt (53);

		WireFormat.Reader reader = new WireFormat.Reader (bs.toByteArray ());
		assertEquals (reader.readVarInt (), 286331153);
		assertEquals (reader.readVarInt (), 1153);
		assertEquals (reader.readVarInt (), 53);
		assertTrue (reader.eof ());
	}

	@Test
	public void testReadHash ()
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream ();
		WireFormat.Writer writer = new WireFormat.Writer (bs);
		Hash h = new Hash (Hash.sha256 (new String ("Hello World !").getBytes ()));
		writer.writeHash (h);

		WireFormat.Reader reader = new WireFormat.Reader (bs.toByteArray ());
		assertEquals (reader.readHash ().toString (), h.toString ());
		assertTrue (reader.eof ());
	}

	@Test
	public void testString ()
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream ();
		WireFormat.Writer writer = new WireFormat.Writer (bs);

		writer.writeString ("Hello World !");

		WireFormat.Reader reader = new WireFormat.Reader (bs.toByteArray ());
		assertEquals (reader.readString (), "Hello World !");
		assertTrue (reader.eof ());
	}

	@Test
	public void testBlock ()
	{
		Blk b1 =
				Blk.fromWireDump ("01000000030a829633bc7bf1a7da1f62a5e07249dccb23d2a49f546ba100000000000000bc136fbaa332f13c46a89ae811aa622f3093608cf7d015276c3e64f9cf060c53801aab4e4b6d0b1a734bf22a1b01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff3807456c696769757302861b2c4d4d3d3d3da55adedb4c5856f7307e68d96f06f9f3849315ad0af015c15cf5f71d00d2340100000000000000ffffffff3501000000000000004341045976c2b7974f5b7b0dfa5a0b5f4b0828f78f88d4d6518dc0645253d6707355596364430b9647fbd15b9e264ad4c0939107fb8501ea89f91bd0857628dc16ce4bacc1585804000000001976a914cbf559b9d1f02508a448e28891395d8a143f26fb88acb2a75504000000001976a914996e7360841075378817a79a782d72c809484f5a88ac8e1c0204000000001976a9146df4d16e43f3f727f1968c3c09f1bfbba46fc5d288ac75b11e04000000001976a9148597d5d833a11186977e29001f651bbd4d63d0ee88ac0c7fd017000000001976a914b2606ae3ce0782268e916dc9e08eb13ba285e3b188ac89e50c04000000001976a914af47a490cd47b3fb1ae1800cc7fd1f2f1860903f88acf0e6a604000000001976a9148d5567ede2794f8b5e85c89ccca6cc2e3ae72a8888ac9da38d05000000001976a914a975dcbdef5c9360d831963bb3923411a495c0d788acf9452304000000001976a91439eb449e3cdf858e054533de91c543fff76d2ae088ac6c302f04000000001976a914af652c623045da48c22e117616d8305d05982b0088ac95caca05000000001976a914c5bc8e52b65d8031b8e3a77920fcd76cc0ebd69288acf09fc804000000001976a9149e10b49ac12dbb2ec969b78442c61e9043746adc88ac2dd7a104000000001976a914c2f21f1ef1bbb2c97080244895ba8a32546cf01d88ac031ec906000000001976a914776c25e07fa49e549944d24acef7a3a3d94818ca88ac6bd11504000000001976a9146f67318de52ca1d2082b9bf094d08b06492c435f88acfd37e804000000001976a914c4423f143d23b6c1a26b8f5aaeac3ccb197689e588ac4d813004000000001976a914cd5813b9e4b38eae5c99eeabd76d09d346bd9c2688ac67390805000000001976a91487d659973a1c75b11e338477d44c4501b66570a788ac3dfac304000000001976a914418e4d605cce8e34289830a21614d66145b4a1da88ac5a325f04000000001976a914180cbd0a36c0991a1d400be72d31d6f6e656957388acae4e0e04000000001976a9147de940f91c3b526d77d8f7589f11172ef47286a488ac9adfeb04000000001976a91404fc38108f998d1559bc80551d84c6630318698488ac8c546704000000001976a914dada56f3d1eacb8ef36d00d4eb5bd06449759ac988ac45aea004000000001976a9142a8c470a2382d80fb56cdf60782590ff4787becd88ac8770d004000000001976a9147be05c03fe69f8da79ef8c3bab21fd4c3819523988acbb450304000000001976a914d744e9daaf32b7f76f4d44c94fa4d6ff428fe89c88ac767e3904000000001976a91461acdc99dc928d31099f1adc76db005f6d9a5d7788ac9146fb10000000001976a914341d91c5a7615b335afbf7d43e9aae2cb12a2a1088ac7318dd04000000001976a9141305cfca4cff38ed9e061c25290c75caffa272a588ac4b016204000000001976a914dd4c51b0788e0bc6be6ea9fe23ab117d87d76c9a88acf3ea6404000000001976a914908ebe2f857c5ceab1050ff96e52f0da5232980088ac94f12c05000000001976a914e6e55e2991742ece883b5dc084efc46552e9378588ac0d225404000000001976a914dad596aa198f76150caf3c43ae0c922bf76e011788acebae7504000000001976a914c8e9eac50f100e2782ed0ca2315c061acd229e1888acc6e8b804000000001976a914ad9d837dd996b6f09408c48fb021f47113893e1288acbcc31e04000000001976a91400f7f42c37f55dc4f75e5145afae80665c62e6a588ace4901f04000000001976a9142dbd943fa4480987fba00463944234550a6d94d188ac0fac8405000000001976a91444e9b068c16db122aed770108345e4a60924298d88ac3bba900b000000001976a9140d37f0f7edeae22e7ab7524db57b3885c0a1844f88ac90187604000000001976a914acd9ff6e5bb5ad0e31ba93a691332fcd5aea183a88ac9ccd1904000000001976a914b5a55074e8618f9d4f9e9c86083eff096f5011cf88ac23e94f04000000001976a914e02e3b7e136d12b23bd07a0954897a8c686edae588ac3a962704000000001976a9144b8dd99194ae1887826edce17d0998875c89c81b88ac3642af04000000001976a914e2daa0e1e810d90afdc4f67feb22b42b6911fcc988ac84209b04000000001976a9141d71a68b2bfc1af6842744794a460a532e9112ed88ac66050b04000000001976a9141f055a6a3c9dbf8857c366a6cd43a34c1f46ab7e88acf9dd2704000000001976a914d5f6ed687203993d4ac05401add7fdece18b8d8488ace74c2c1a000000001976a9142aa969bf3fdbe587bf6c14e606c4d2b14665c93d88ac24db4b04000000001976a91449e45c4f2ff522d6e026a6d8337de6cb87cd960888ac66ee1804000000001976a9145318d3a7bd7d5be0650a8e7d16db458a6b0fd1ae88ac96e10405000000001976a914647484ab81427b30333eeaae714ba08a0980a83288aca2b2ad05000000001976a9143213e4e884d1dfad34a563271894ed76667abcea88ac0000000001000000031091bb34c19754b689685bd5f84078b437653d08377d2d03d256b34f80eb219e010000008a4730440220476c26cdcecccf46fdd96fb87814661cb1044e103d1bcd5be339b9cbaceca47902201b2caafe5b36913ef475d4d33b9d62aa3316ece6f1ac3371a506906e61afd4510141048c632401521a105db6b62db0a2c393181b5538f2c56d461057611252baebc9c7794378c252c45b7393fc93ea3b8bc6d6db1c9b5506744d51d1ddd7a9acd27d81ffffffffcc72f7a8acf77c1b1313d4ba229bff7db4f40e0a1b525a9b8a4dbc8ecc80b070000000008b483045022100997b74ef85d3be61a96a4d8ecebfeb588979364276f54fa77571ff8fb7906e4202204390f9935695194362fbc221b00436b4811d01f645715105f9d081ad65977e2b014104fd579aa4983cece29365038ddbaf479af86bdf25afdcae67bbe8b30c268aecdac6cd8f923ef3f23ab694783f9243522f67886a46a43499e3fb3ed49623869d6fffffffff8ecdae566b8e8d709a32054175ce61fc2a9323e60023f62b23c342a7186beeea000000008b48304502200f95d4cd51bb5b867563700979ea23bf69921b1a0262ff5de3d7330bb992a713022100de58fa5822e7e62c8e6e4edbdece7913cb21f5a7b5a39d85fa9291874ce81a710141045f7f6eed56863bf527b8fd30cbe78038db311370da6c7054beced527d60f47a65d6f1ce62278ba496f5da4a3d738eac0e2cb0f4ac38b113cbeabef59b685b16cffffffff0280c51b11010000000576a90088ac4cbdd397a90000001976a914d1121a58483d135c953e0f4e2cc6d126b5c6b06388ac00000000010000000379c1e9a1351b8c48fca27acff391512cb175978c8168adb40bfcf53c05dcb294000000008b483045022006a45b800a55ed2049c5d4ab7a56c2ff5855b665e9ae317d630fcc7e6fc8fe290221008952ed27b36591f55fcf42f486d1f9c958946c30721c1c0cb74972e681bd044e0141041f2f2a1e0b07aea8121b0af50fda58aa5893c5484c3d7389b2be08d02e986b07673c1207bb44e4c8f53ed51068ddec008105f1aa1c93c182875174561c08bd28ffffffff95a53b939c4bb466d77eec9bdb5a2ceed11682150e57b70a523d6f0c875998ff010000008a473044022005ea3540a376e1e912a86ffaff77fca577b790bf9e6c01f1ddca0ee1f592f03b02202ac4b54ab374df394c339c9a56e3730511b2c9a8c43213cfa66e46540e1d06ab0141045a46b12291ce91177728fe72e2b5fcf889ec9af92e5b6c5e2d018fec3be33b9cfefa818ec5f054086d88529dd6d70545b0aaa2a189b2657ae95e7f14397529bfffffffff001372c7988ea3117066373bf6fe4e065eeddab23113017852deee196e83d990000000008b4830450221008107b6c0d39012ed287546e170bac03ca5b523f768a6caddcdd42a05b762f4e802206156dcae129cf29306644dc06a7cfb361ec4261b27c3b0dd05e11be8b1ce643a0141047c0e1c51568226267607fbecddaf8418ab9c5945f59c15101c513338e481f838e4d3aea166fd0c2577b6324e623524562f0b7f00861e25cc4eba4012f3b7dd08ffffffff0200e40b54020000000576a90088ac2af12b94010000001976a914c494dc55663b229dfff5cfa4494422da1cfb733f88ac000000000100000002cebb018b42ff01857999f937a0a9f806c5140c99ebf4350b3f063de54f242d67000000008c493046022100bd4495bc8894b6dc69b61e4490ce6fb037d901180ceb77c52a1cade1e39b6b4b022100db71fb5e4ac1b0ce41d640017a86f25b5a0c55373076851b249e1742afc6d8850141049ff3c0a24258c2ea592bd7470ec90808e5b2276c87e42d2dc634a50b92af6deaa759df2a69e887549f1e29f3ee13767e000e9631fc50935b091fbfede3155770ffffffff2e807ac0a79be9f4fa4424413b489361a2b3854915f74fcb9891489455df8e7a000000008c493046022100b35ada65dbcfad525f2ef72b8d184920d259a820e52ab42107d85e452830f6da02210098a6cf88216650ea1ca782b3be407cd7a41d9cce31b62b4d25b413cee75675820141040c3e3629999aa48634ed51d1b67567b0275579eb70cb308de99d7a4842903fd4240c269835c0a7cd6ed6d4d0fa563536727360a5b1b58884f8123221bd747e04ffffffff0200222048020000000576a90088ac95b46b8e140000001976a914ad42ec3fb939e7fb32e4181c627b31f4ca51935088ac000000000100000003f9b0aefb747977759cb38eaf49070941bb50725c58131a51dd2ea382856bd493010000008a47304402201bb7594d0dbcd3c3d0dda3b62056000ca8d4bfc888e3b2ccfed63267ee313e8c0220728e003c152e743525ee46421cb84ed03734536c6ddfb6edf22605ba937f2a1b0141049437dc89b2f312d6bf45c4a8f724a03f38f005ef797ab109c590ae8f2b741bfaf335198813e3ac20f41aa61a44aaa41c2d6d1351166192a93e9a2517e300cd11ffffffff907e6fb71e188950c217ecc1354e60978485466616e4f2fc0b54066dd6f74a7f010000008b483045022100f2bd60edde0336efb4469648a929483f189324838d12e03302974c0f9f3a38550220236ac00c39c3a5d3953374880fc761bc5e5887d092e6a57cb4c52cfcbf531eca014104984a410b276249519104c78fbf3c254a5d2ef54e4d5f50ee4e97e8bd921864632b6bb0aba8b15e574e9bef31452fc5d2ef86754f02e9f652e70411707cd89e39fffffffff6c7455d978b99e8545ef6cd9e2810cb254c8b044da43839e44f1af25c0abd89000000008b483045022011800e7110565b4ef1ad74b681c290197fda98d64f92d5dde87a806e3a9b9089022100e1d97e5b2a7849197c5d6b88e57b58a619965cdd66f2a9945775b445b474c17601410434eb3054af434d2b516f3bf7a29a765ee4a22911c4708f829d766ee635e62c6f4d77150c83d3f0251cfbe6a4b8c2b507c3b0a3dcaef1daea571728b1a3bd0c8dffffffff02a07be810000000001976a9144640c0f3b01655377f10a4ec6c71bc4f9acb037388ac00c817a8040000000576a90088ac0000000001000000029c2939ce49092ebe03663784ff1159ea60a51cd00e140cfb30a6ba10d48b10e1000000008b483045022100b0a0013746586915dbfcd81160433c50abb23ea5f14843ded879de314268de480220363d30a18ddf66fe4922384c3b03c591d26585ef6b886c530ce5d6caa0b2a40701410469cd4c69f639e978dcc3798fecfaff76a00ef713d4855646b2b358bdb5a6cdd1292e684917fa1c3deba06d35c3d64723b656ec4df914121e7a3e3b89b7c720c7ffffffff0b524c36b7a69737820ab2b8baeb26a98920d8e0a5fcabb3db9a61eacfb6efb0000000008a473044022049b3a8e3daa04e634605bcdd4a5e159ca06250e33872455b902e319a2fbdedf4022011c7be9f3673b72fd5181dba3e7808b48051d2ff0fc46c70344d55631aa21454014104dc93841231106c4a749644408d55b0c7e0f886da3a854308dfd4ad626847785175fb19b31d9c220b2541767e792bb7f7e3d0608b0660649452636ff25d9e9584ffffffff025b5d004b060000001976a91416f50e5bd3609b11b4b29c945d02d68b307fd2c288ac00e40b54020000000576a90088ac00000000010000000219efe8e1194227624d2bb6f318c6ec8de8b7bbba0c81ae80c17edcdfc267471f000000008b483045022100ef15d2067b6a1a649afe62b966c7b0b52eccd25bff6092c6f6855653fbfc8d6f0220329b7e39b7081318ef7d6b1a48735a3d00f69ddbaa6b220158138da139eb74e501410490cb3e0cf1e82fd74da06588c2b8a227ce6cf3ffb586cb223a15466ce993ec9a831c05d428768859f7fed3f07ea99f57bcc6b7273e353d34062a61f44ed749c1ffffffff95d6e74b2c05ecefd10db5a9ae889cc7cfbc614d1fdc27dd2a5bcd8a6447c25b000000008b483045022100958f9940845b4c149fbf00942634eef1767f2cf6d786ab71394c6e563db5e64502206f4cfffb10513b59b02ee77f62f6e1481b70d78ccfd5ffe95ee97fa5ba93b2bd0141047826de0d9803045401b9eacdec3dd12ccc24db786d613997ab33c3e0cad2952c736b7360684cd8c5ca3698531108f9c1d121be690567b369d49a1c3400d96614ffffffff026043a97e0d0000001976a9143c6eddbc7b4dfba3870364c5f72b5adfec96a1d688ac00d159920b0000000576a90088ac000000000100000004b97076000b63b0af66b597376ba2f647ba15a03be001c5f84b60d804b9675416000000008a47304402207d2ab9d5febcac0c6863e57a97b363a51c21c836a5ade2efcbd88884da697784022075838f05471489b9b83d6349a91f259c8de85168412d267b5ab37484fe4b689c0141044428c31cb917b10df68b9228eec86d9236a700b30bd6db34d7c3a227bb403f0cdd02418a99936da84c6e111c054ca8f556ba149d2f3c78a539a8bdeb04214e62ffffffffd3162e2acc1e161e6bea079d45f38e71e91c0cf87858c4dc3d4001133733ebc4010000008a47304402205ced90985ef2db226e9e6cc10806334fe50dc6b9f3f759e6079600900968d7c302201ea8635b24cc7e7226ae3891d3968792753d346bbe29e68632b99b9acdf7382e014104db1c689aaee39d3294eeace8ecb58a1af09401b2e5732beb8c0e52cdcd82f38e22d404a521dd1f7bd238350aa2e2f7365619eae49d8f8411e323720f1db9984fffffffff50d2d9c5c95bf7aed197884e60d33847ce9d34c69f1fa4a28fe3fd5356cdbf14000000008b483045022100b68835fa1177de4aa514e55232c34a88ad9081acdaf30797565fbbf49f722dba02206ed2ed427afcea05c37fcc6e1f11e676a820215fa65fce16a5481316fbbcf577014104df16cb9d9c33e0783b2f6256727edb62451279f6067cc4f7d7d6017f23982152fab4bdc9efd830e995df472e05fc45f3bab3568c2bd8f5165d46be2d372eb313ffffffffe379d34887eba5e646ba43709ee78f480ed714a3b2a1fd072aaba6b1756858a7000000008c493046022100b430fe5fadcbd57a16ca8068a94f888a8fa4ac5bd8fcc30e3708dc15dab703f4022100b3ed2ef9bc04e068d8404b7fd8ff3284e88082da8436b543acec9404a75786a001410498cd5baf356c719a385b5437a569d17cb5f9705d1972d9657208f174b07eb7dd34bde6917d00a73e9e21b2667862cd2e0f6c3efe7bbb7cb1535aa702c2c56335ffffffff027a404c79020000001976a91400929f558848fdc75f28c8d54a60168f1f58e95288ac00e40b54020000000576a90088ac0000000001000000045286ba6b751fd73b489ff568c4703a218c1b84cb4f050c6868647281c8a0bb5a010000008b483045022100d9c62fa2400947bbef0cf3a12fb624cf7077cc7012c8779d60caab5e729c592a02203549b760feb804ff75b7debee0f488877267b1898f328e5ed0f41b359c8e4bfb014104406db037ac5abf7706be70bfcb8bfc96a06f4d2e9ff32d558626326c4292bd7fe009ab11b78fb77e954ff1f1d9e75e0b813b9294c935ec346f878ebf63f6e648ffffffffd979bac67c4770b7d426fa9e86e7f28e9b2fb122c54aa3dc1c1a5efbcebbe71c000000008b483045022100df404dee5c82f8a5f427f265b2a8bf8281287bb0a903ef7c9a420ad19b6240d702204ef82b8b1fb6d96af1b77eef45cfb41f30f8b972874e476b5babb1ee849aedd3014104dbdb7215d1d188a1d4b0cc753893ead675ff357d12a651bff4f5a085bc25ca3841f6685621be85fc5bffbb96d0ac7b22d7faa3b0a2e0dac791228fc21168986affffffffaf0f08cf052709bfc240a5c9797316cd992e029fbffafe8d12583ba7b5031f15000000008a47304402205e2b963834ab670a0ed1ef54dbe97bd059968a9a7000dd74c80e32da05c273560220498567c78d4257adb126b1c86c6f296f0ffbaeb45f9234c5fe0611570001b2d00141048b354f8985192f0cbc2240807f74696da5e1b26e3d4175a9c5ce29f476462337db3cd259926d106a77d89f806342ee14bee8dbda497992e0584cf71dacef881affffffff5033093d8fe628a954d4a349ba553d8aa423de6de279327a5d9d5cb43c3758ca000000008a47304402206c428dbf810e0fb77747f7ff9d60bf6657e7e3edb290b9a8de8c12982ecb08ce02207ad61ae4d2470b3d8759d99bb4bd116a90e771ab76fbfdc791be0f7a5cf5f328014104552b2eb41559e7ce64c513f8a606fcbd8e39b0f8320ea93b92d2d483c86d8e5f860bf652bee62e744b10671dbcc89acfe8df3f7b368162cf8c77a42e25f8b7a1ffffffff0200e40b54020000000576a90088ac27edda06030000001976a9140ad47afb47219ba4efadb4d33db6e49a71f724af88ac000000000100000002640bc279dc41264c678e24416062291991d247cb593c21193da12ac7e9ab0e00010000008a47304402204c00e5109dbe1644413d92ac8408e0c02d888850e97c1bd74aa80f88c609bdb6022047d3f208244a3111c612e6af13052efd4b543c4d112429d2f838d35ce859dcc2014104c99034ab76c908b87a22f89b8e260447a066349c754a4eaa4e2b4d663e4a92f6a1e6540a82c49fcdc03aff1f9937ffa6d4aaa97802276438bd9703f3eb732ce1fffffffffd269441066b1683fe398de40356c4958d3474d817d036581302dcd0412c4174000000008a47304402201f7a187130a083a4d941561e0c39d61287fe1580d874874d7a62aaa62a85f98202203a14a99b2ba48c870073234c56a3b055506116ca5b8d0af9a1dd7997b55bce010141048548bb73eb0ea484f7f55236522f4a681cbf75b91848eab975aad8af35791629bdd706fa26e57296c5accd02c91b1b051acc850bc1e7c037268b128a52bc4b5fffffffff0280f243d5000000000576a90088ac0488242d020000001976a9144914281232f335f5ca684da9145aff9fed2b1bd388ac000000000100000004a70a2acec4564cbf68ba54de025943cd34db518ef7ccaa1adb9f079166d1d838010000008b483045022100a2b5c5f76c8565f2dd24e13af4c56578dc1755b74bcc2fad27a61d5e9a76e9c102203c7e279e07d10b3c3b0c9c87d29faa0304bedf3e9444b23e58ce445f15a052db014104660cce1ed3c99e1e6c2af088be0fb107c5dabcecd4eedf45f076dbe7ab5c0aa01506fdee2c2a6520f193d40a7c53d680f36ed17629bdd408bbc0be793a9026b8ffffffff50fabcb9f2a42335ceae67d9a97e852f1ffe19356b086fb1a1ee9873edd50eac000000008c493046022100d1dfe92b9f122cb5163b35f4a80895b106751d3600e330d3612c5b78ffa84a39022100939d0af24b057f08958030a13d46e2d56842b8e147dc7ffa7c0cc19e3d3527c601410465b84f162d2be2a7fcaf467c610594e7209f96bb3e2fac8e57bdd9f02f8c4b5f3434e656f8cd0d7876cf671690e939e31bf3d5b4ce70590862dedd35eb0db903ffffffffca944f07a8fbbdf81b4c575129e97485a3d3c160f271d07a426728f3c4da08e6010000008a47304402206c0022db38159e32f48bbfef5c781077cd147d9885d7f20a1af73297309a22b60220417b89f11ddbedf407e4c2ec745f0ee99bdac213c752fe77099b37ac5651e675014104016719c960c4312afd82d81818599a79c8e52c96bd631ae12ed7fd474f656696e4365bf60f5433691944f2456f49750e9fc42b067a4fa31c109f2171072af728ffffffff4adeb8301ef9093c4325e15c6c822bd34288ab8b8909754673af9d4b67bdc234000000008a47304402200515281ee114adcb28c3c5e6dc0d15d6ce0a69dcbb1457308f69ca109cf64e9c02200d48b04f2a44202ce0843e6b80a4e3c78eaa2a7f300259a1116c86d0550c887a014104cfa10d3f5636b42042d44d10193e9c56ad5b37adb4af47844f8b361e42748f33cc8540ac015696ecf4d687f86ae17260a393d40a5c4ed2f343d6af87a9c71b08ffffffff0200e40b54020000000576a90088aca04ad999000000001976a91415c2f66d85282d3136f044874899df12c6af401688ac00000000010000000548bbc19f5dc889b303a5097032b6235d0f293c3517437a22ab868fc5b030ccbc000000008c49304602210083cfebea9add1b691c17bbaec289ca4ee4b0e5052b95b85321350d8eaf17e7ff022100daa02af66c312540cde6f0d78d7156fbec17cdd0b1fc9f737319eded387197aa014104a9efbbc65f1fc99a89b899a2ca0f6ea7a709dfe1386aa1ee22ddef0fcff32cfd776d79fc67f937604cfcf39505de4b79ba753205f5d17537bae4b54e26248d0fffffffff0b050a9a612d4007e369d02370faf7dd9c99f4025136f9ba3e3c05f3026b6155010000008b483045022100df6607bb6285833a13aa20e96c05693b3078b7371956b8239aed1ce15af1ba8502204c2384dcda08bb68e5dcf0d65fea4aa2933fc1b7fb827541648cc03e4886248d014104debd5fe6e6bc796ac51e8d8e6853f6bce567f2d56e4c6086e14f7d7cedcd7999302980e775b5334deb21f29a2794e54979db14dc239f9237b217619297c1152bffffffffcd34d66eabddd3067fb4a2aefbd097e6c2a636d50fecf59f2676e58ecbb5f092000000008b4830450220784a6af74465584014dd815c3075918137b96f30edfa84d05accab644b61238a0221008f3bd6860303a3ade81cf66823c933083e18d38573da4f28a11f9bec2e3f6c560141044ccbe1d2a628b313340ff0418e8568f904cdc4eb6d4893ad8da70c4165dcb838122061aa47e2ab852cd782fbb42d21bcb8ba243e78ff4344f8bd4faee092054affffffff42d957671ff25e06200376ecf7e764b75305405a397235d85663bcb187390fc7000000008a47304402205363f75cb7c1a01bfe10e70467c7438a266793ec10d1347aa5a54fd60e82966102206b7fbb1b974dc3d99c74b7496627746562e609cefda4b646c654eb4dd34eef4c014104eaf728ca0222ed95104b3bb7497011e275b83b76a01c3dbb20dd6fcc12b3ead2722c3a19a02bb3f113433477c4cc6756fc126c8382e85666d8574af34e0275abffffffff6c3fad717b65f66f35f7b6f37f37d1b5e5487594a5c26ec2c15a4958aa266920000000008b4830450221009abbc460d10aeefc526c5f1b2a03e9d30522918bb870b5aa2d52614109b7ef910220439a27b134bd6dd967ea8e6afca46f8f7b7344120b66fc20c7fbb6ddbf561530014104b187b9770f4be1aa8e7ffede833b4e9cd3955ec8ec435ed53ab5c0b047df419913c6d7ece8586222ba4fd4f3c19a582214c3412dd3f31b1479d806548242e285ffffffff0267ed0290080000000576a90088acd8ec1115000000001976a9145c6c7f33ff387aa5e76a909ee1eae77a982f8fce88ac0000000001000000020d7dc1fbab67fdc7ad07a32dc264a6a89e1b4f90bd2a68e250d50f6f56422a4a010000008b4830450220109fa1e9015ca11ffb48c1c3396c16aabdcfa45f5161c45dd01be0d00296989102210087e4591ec96b49ce0aab06e59034b066e7dcfcdfddef978860ef55a6fd305465014104e22055ed36a6825cb9ef98896e21c697b9a1d0c233220ce5ff1e79a5dc8231e84fa8e3e18ad0a999fc30121a3effa6576ca5bf8f542f2dbe8d6b1271e103050dffffffff1e9bd8d6617a678a73e3e1ef1649f75bebf09cc1a25855ddeaf7b7e332e5245b000000008b4830450221009017d6a5c2e3d07d84e1c30b36cc5288cde374a7ebf5ff00851120e591e7dbd1022058c673a7ccecb1b93f837ccdac83246d949ad65cb7774b32372923b49a66b5be014104c0838f5fa82a5eddd38ba77f3025f0c5b1d7fe044254052ef2fb2d5ddb590d0e706d336b3c4c7564747412023f146dc1084583e7f0d20f09ce2f0522705b6976ffffffff02c507156a000000001976a914b4ddb8b532ac4bf8eb049f17e3b0439f0bb6cda688ac80ba0a58030000000576a90088ac000000000100000002023f9e8ec6ba2557aa132a321fb59a7431446be1f1ac47459f9cf5f3c0ec839f000000008a47304402207af3e5fa4af66df9d65c1d9990e1840ed7d93aeeb52aec715bc96834ce59413902207ef98c22214ccb4672959c5388764520bf79d64b70efd86a52c313dddb3017c60141042501bf738c99668d59e2eac327c5fa1d2df878b70c632e07b4870c56675db8f70474476069d6c0870ec6656507ced71ecbc4cdfb0c54e14d645de2cb20fb967cffffffff328d45c8c2db1e93b80f72583b64d1afa2cba961f3fe0e681ae3a1d1ea817842000000008a47304402207ef22192d74963e4b18a14fecfde3dcae0bdb98405711f92a3f07a4e71bf3d0902202ffa231600f2de0370d89dd12a00d864362efdbb7d662b5a6df3caf053fccf8b014104aec30ec70029b07ac2c520e5aca16bbe177cafa876bfa59f4a28dcb5741e6207add42b73d528c53c599ad54827106077e018d52a70e38432cc29d0425edbfccaffffffff022124df22010000001976a914c36eb340bede7392d20a1ead0e22223c3614e51288ac00e40b54020000000576a90088ac000000000100000002b3433bd439ad4de84486497ae0f8792d2af6c93a6beaf3498c449c34d30b06a5010000008b48304502207ef51939f938e73ea87281f8e2ca4d9dee529750e187602eb585fcd7f7e804b30221009ad3b8519fe91b45bbe509c9a45a709fb90bd61c0d4fff1a49d1be8459bbe32001410465000d8f80f2a2de2e670e1a895ba12d64508504e2fe0fcb3c81c107a376810e8296353fa345d5f923fe03ed27b493c853fff931e878adec54d861f8fe211673ffffffffb78ba1f379ce69c3425514260cbac8d58abc324af57190da0151552f3e0e06a9000000008a47304402201bf73c5f8cca86bb008adebd69ca0c3954682f7fccf4a42a5bb74a88ae11e3910220312301f26a99f25f42896fb7e69c8e05ce0095bdc3d9cfa5ecfd5b3c7e9bfcc6014104476cf62f4d084e142dd386dbf58c5cf7ede50381fe9dc204082efb91e139f38502838a6d7816e83d9f1b9f42e940fbd6d10a0f64d53a143e54a610b080de7d89ffffffff0205b61ea1000000001976a914db9d4a800a4ffd2da65b38eca5d86c6d7d08127f88ac00e40b54020000000576a90088ac000000000100000002b6c4ed0ad8af3df9657a86620091f77168ad971dc9cb5b450f4f9ea674111a5b010000008c493046022100852a86d2f99db69939c1c9ce1508ae815eddedb72d8b718e51e31700021da8b6022100dfa8089991e932c7f4e50b3f4ba758f5fca45fb0e325a13441b5479d578f37ae0141044288c93686409888ab458da8a13bcc3ac42fbc7d705037330737481ad4e1138d2282d72bdd005cfc73e0e4dfcf9e34f9bfe015fecd47eeef438341c91a3ad7e1ffffffff0395cca9da44033256732afefff0baf8b18e4773b9d9175e49a7f5ce046f60b8000000008b4830450220577bf83dd84dfe9367586050f6b8e58410e0184d1d5e0978ab84bbb5cc1e669e022100a4d57e20a0523b8841bcda6d80f239469e1ead73ddd0075e500ecbbf10a9c4b1014104ea85268a10b2e98808318e492fb8c8dd8ce71bc76bdbf224673116d17cd272b0cd1f501e9f6cfafecce6d577156bac4ff2ed1b61e060162f1df2574f83044fd9ffffffff02511ff15c010000001976a91481dcf5d6b068f73a950abcedd85ef3f6a08f644e88ac000d4ad5000000000576a90088ac000000000100000002365db58511ead0a21395a5ace6b6807879aced9d4af329a635c4539ff5c0a59a010000008b483045022100f9dbed75fe4383887f705ff21f2e4c1387f05146390866310de1a93dd5fe29bc02204fe61799e2c45758645498f35853f19e09370af67301ff5013390ea4ff5062bc014104b757f915135e703fb46090f0b43b561a9ad03e58709d740854fcf57a0926a9d8a88fe650ff62333ba22a6ddb6c47ded04576f0bc95240693ea14eb2336bb23fcffffffff71637c58482f5d9752a1e6d4fb641e29406d27e0603c1d90a5b233d4a423adf0000000008c493046022100b5a80c6c27fcc0ff871d8afb7ef69b0b9a12b2af1933e26860adeac18a15c4f1022100f76f91213af3f81824e7272f8e13c2d2af80e8ab6e3627cc8e61e7c94eae13cb014104ccd734cb966202957feb2d99665fde8dc10058c846eee9600c472f04de62f8c43e29675b49cab939964893b5561d4ce3a7b843724f824eaef371f8106b971648ffffffff02227dd4bf000000001976a914941cd547e06648a37b65113b2c964f7614013d7c88ac00752b7d000000000576a90088ac000000000100000002820f995cc5610abc2bb46cde44e09d3999c040e7c55c018d51d6101d97e3c7fc000000008b483045022100ed90c5758af1016b55c7edf5c6d5ed51ee58240bec20b37426484ce6861b25a502204243b29c1844b2d7d7be7e86baf66b6190ad82157dd6e255a158e3306f694b4d0141040dfdd0d4f52b0d06d8456382fafc5e9cfecc9d8b22ccba8f81ac547b07bcf9b0b325354403ec29676958179ee88238c69148e604ecfc1a3a1db988034bed9d8cffffffff5c72c48dc9a96256b89bba1faeccfba2299c84541ddc9993d8629170186473a5000000008b483045022100906a575cb19b671a37ceb99bab48ab9ad16f41e6acd457bc815d87009aeefcc302207119d2eb966c182ca89b2e2f7758eceee643cfecc7957ae66b99abc22b9fb4a5014104b36bf897bc8e3ddaf994d35fb9318b96ea39c83d79fa321e734d9e0ccf098acfe084a85da72b21acc06fbd3fd8c0d1135200de59435a9513e7a070d4e437c4aaffffffff0288d71375000000001976a914956ec1b15bf7bbf51579322acfa2de0181a92b4888ac00e40b54020000000576a90088ac000000000100000002e3dcb3745b2268694e09dfef8322e760148bc20a572b4ba05114154937db3e7a000000008a47304402200732e1793ef171cfa80e6e1e31162667544bead7b41c5f07a1efeb202e7d6d4a022064077c7255c3d7e6f486b98cf73385d1be479a7249cbe7a9064c0a5765f02d9b014104eb6b057a58e583a1102674fbd261c6e5d12e38c0614ef09d075ced6758250cecfb650be3f72cc372db7fa3b1687ed3d2cd3031a3d9b5eaa277237aa399084d7effffffff77bc71b71f63a44f4a6237f4465ba1c5047fc6eea03d6dfd2ca99839c97b1904000000008b483045022006dd69e583d3e6375328acc7008ca8cb524a627b642f167893f38e84ae778d670221009e042c8c7c5a87a99b1d9666bbc9bcb533bf2dab193fff812cd30daeb3bc091d014104af4ff96561c45324bb0e5acbc9969bae759c06805df2b4f158198c80f550799bf82e00469a597d85cad4bda76f96aa75db681153e70968270df4f6530845963affffffff0200216e83010000000576a90088acf605e870010000001976a9149fa92478eb2a698b1c43b93d1cac579e5867f33488ac0000000001000000026d2b9285ee9e842b9bd7679c968fba4f50d4f4e4f3fc3ab09b67c9a33181b356000000008b48304502201fcfefc85852e5d62d84d7b291e06d610f6e7a8cee2cccf45fcb6ac45da7bed50221009522abef511ff6e55bef3fe4d42a0392fb1158e4ee11d199e96df7fb092e5b0f01410439c7e7dfa93f1f2df27c6477da515959d20e5adeb97bbad791c319b798f8471a51e3ba6e9ecd655d052600f74acd2dd4e4c550e112f3390cff7ac5f9b9e7d28bffffffffd715f45860232b410559a3637666e15b8e0a5d638b8e3bce62e2a85867a1c80b000000008b483045022100e4f93784ed21edcf992b10800562e889c232b9c91b6be7477a0f5d8096a24f7002207f73f01e85f2dfefe42e7eec5b367d8f1e3f6770c473e6ec4aecf419b969ef4d01410432b78bcf0094b1d85ef4e0432615cda857e2db16f36b6d36bebb3b4af694eafeaadf44cf5f366c2e280e3976121867fbbde9bd52433db6ad90e1badda28d7ca9ffffffff02403d49ed000000000576a90088ac49a00b05020000001976a91419addef47d03493eec8498478a12816bf60b0b3388ac00000000010000000272ab6897ceb3ed03b6a2b1ef780132eefd30b868a2675645f9e000190cc97847000000008a473044022005c02efb73f739e86129b4f83c1d1328dfd699d004a3d751d9ae4727649e008402205d6895bdec555fe2e873b141c2231a03a005a4e77acb54f53508ff64349308d80141045104b3ab84e7ef4fbe927a80fce88238d959b55fdb24060d186b7d8236d4622d91c3f1637fb72180997a66490b2b3619cbe66a1ac8b29d4b424bd428c5027d19ffffffff7eda41fd44dad3cf3b348a77551d1def84453c7d9ed6b5891a8478e4df4d95e5000000008b4830450220699bb2298c200c827a427a0b93367db0b709a1b0015661911f84d4f6a4c8fe40022100b8c145a4bef4e6e634e9e812bfbbac0dd6ff40f68feaf2459908d44fc7852c1201410445a0247f0ebf30cc032863169a20c1769be5059c5b2c1f396b0e20885700f06b9d4f57da5560531506b78e687f48185cab7481e0347762a4934929b1fe294734ffffffff02d864fd4a020000000576a90088acb762a15d000000001976a91467cd6ae9649e5d80c4e3892d31e1bf3cc241e88d88ac0000000001000000029e2d25174c440290f96c5c85f75a286926b28d724dadcc09389b520ae229d0d5000000008b483045022008fd31a7c955a1aa1d39849ff0ea7c2d286f6557184bb1b6d1bb91a4e0f742e4022100942e632ad357efbc63b24e761c3ee14dce98cb21c4acf5fdcbbb7dc95919053a0141049cced48977ff3e8886e9de9f148871f0338d3393eecfa003bc383a3eda1bf0bcc284205bfb9aeac58426370e4f2027d3bb7284d09b79539b1e2242cdca33ee1effffffff2c92c1722d2ed167e523943d601d1bbaf946da44cd4bdfdcdf9f3ff71a216a29000000008c493046022100a68ea7160f424662aa0e2e10d664250b890bd77d3425220df0677ed354e7bd7c022100814abf85fe352b6962f65046c43458a58060ce823bec18bd73ffeae9b859300b014104338e8d5b7c053dfbec2d081ce4149100b625a11eada6875973bcbb9a0e4a6cbd8b3b3aa2483cbd5fb1c0106c223f391f0872b1a9e87ef514e60cab51b0e3f112ffffffff02008589dc000000000576a90088acef220c91000000001976a914a37e0e3bdf5b14dd31656a4d10dc03ed7ac2387a88ac0000000001000000053ed213109224b1dd1670c7c0ab58d4e9624bc35e3e5387c6cfbdc865727f6ed5010000008b483045022100fc69d035bee079389ac6fdb384d9a849fc219ae7ac9af27a0aab505e9c52780c022050801b3971a363716e10182e43f56d9a8cc796b41764e2d78c5cd291cb84783e014104bc602e6f995fa1665aecc6a47a37a4cc709db7b2a74ef5dc77af12fc2e630265741f3ea1900b1263978eeeddc77f07c849b617715a39d28295b00bab728eb0acffffffffa07db90df20868c32368fe93c94f8728dd4c067336e4ed1cb17ae25d909715f1010000008a473044022038edeaec1ef24a2221656e80b4b91437f9e15fc1a33c1d53c7c08843a486761a022075571c3fbc99321f7427d3e38adc2e6a6eb9693edbcbbe8d3722ca876d271543014104cc25f901c5393e5e1bc31e65867dce8530da7f7447b291a15856fd00bb97e7425303c9d6141fdea77bd3e714c4e14fc4881f8f929d9d1f3b69ef2589580c7048ffffffff6a3c51fc1627448a20147eecca0cdc3eacab2d9553bceec440c589ce7559a560010000008c4930460221009e25da2150990195aa4c82f4be3975e4dfb1807fb61b3ce38b985bc0aa962cc00221008072b935899ba5221e8953a4f6f5d000bc073fa39a5268a37ceb74b57c41632e014104889e87dc69456de7341b3f4b7be13cfd0f12a25293dd5bc5f277898b4302247df8a3d6d62b4eda4dd85006d05c1e4333632bdb9885156ff5cbb0950be1d7efd3ffffffff118296ff18757293b834b527d9079c7cc910758b95179e17dd1072520b9903db000000008c493046022100880937c0eb29bb02001180bb9acd4a63a09df77a8cfaa202d9fe4ed8e45ddc21022100aadfe80a5268be7e151a3aa8215fb2cfe6e2cce903a014909dbd1141cbe05d9b0141049cced48977ff3e8886e9de9f148871f0338d3393eecfa003bc383a3eda1bf0bcc284205bfb9aeac58426370e4f2027d3bb7284d09b79539b1e2242cdca33ee1effffffff8ef7419e33621c8dac43f74ec5dec2463c8893cf449afc740ebb19bdba0deb75000000008b483045022055f534b8ecb4dbf832aeb566aa4e95dd0371c76a2eeed2edde441bcec5862402022100b243282a2003028f988267c75228306fd6dcb68ad1db4c0e7bd04acb3891743c014104dd2f0f609d31439fdcfe64f20c133e95156527aaee35ad7e2ec7d148256e41ab00eabe612e40419fa7552601e68cae3d7b6f8d519bd7444175fcac8cbb6d807fffffffff02935c2815010000001976a914f2e63314c350094550c703fcdcd4850ad37d831088ac00e40b54020000000576a90088ac0000000001000000028f1c4c3b555ad15599cbdfc217ca83b25ebc63a7a47583eecc9e849cdb292b74000000008c49304602210098a473ff7b98d7acc3ce3acda19f4e45f330e04ca4d31a02b51ae69adc7b2b000221009c46c53ed0fc1b8a257328f9b6d8f6e02ed6c44a94b2a0a4c4728027558d6c8301410440008f0ad075c74dec817f2e4bebeacf89f0666b3c5c35bba197a7cce335da684ee2a9087d66b0501934cc3a92dc5f68660af35367dac4e5aac1aaa1f840eabdffffffffc4c286341e5608e2ded8cabe7f29b02a8ccc427c87e51f2288dfa291783be2cb000000008b4830450221009139501c766c7d74d0d8a1f34efedca971f53358bd79263204a2eee841578a8502201a105f464759a52ae65e2ac3bccb563bb3cbc093ae13e328b1411e87edb63596014104b19a8b0dbe177f7e019e0cb95f12ad3ea6923805a8daba88ec5f28d777ab2eb7376d7e00a726d3b5056dbe301a5c3e01748a83210a458421daaeb77c9975c5a4ffffffff0221dc0124000000001976a914f74c13d007a14fbd5b9ec91dfd1cfb963d0881f088acc01de690000000000576a90088ac0000000001000000014d0da1a301f0497839db0288f1c5a6fb7e4f00ee030775840d3d8dccd752eaab010000008b4830450221009eaf4aed229e80d11a1f3a82ea27657639493c980e591fa770b8af36829bac910220777ab5514c6645d885a74fdfd5daaeddb674c5f2f807474fdd552d8fc556e717014104d60963c00623ebfded78e022bb40b870c4a9d2382d69881c1db327fc2d39baa56fe0c6ce07a18da6ce9b8d6ad4f0e48ef07582fcc7c7cbd0816804e6d9fd1155ffffffff02e0930400000000001976a914fbe5e214b23fc7a0bc3e764b043dca89c20b1f3c88ac60c20523000000001976a9141537e5e949dd0f552bd7e9f4ee3cf7b223851d6d88ac00000000010000000166c7d23b44e7833216150ebac52063af59a9edd978336a6119f71ad8e404ab10000000008b4830450221008c284af16d3ee85470b726c1e0632b12a3bd9b0af51cbbdcae6ae52b34a8aa3f022030dc762c7b2c68e41d5facf79b059259eb257b5d2b01ff7c0c66e125c9ef6e980141048e114fd19de8b537b82712930b1bd64c796d0abcba48c92053481a824361570b4b2fa5039adaed27aee785f6f68699803a4d1b04a63372d415a91c63303e2388ffffffff02fffe2b06000000001976a9147fc4dab271d443d3aa399a5f96707b54ea0aabbf88ac18170000000000001976a914c4c69a79f4e56bf6e17308f6e5e7ff4dc046ac0f88ac0000000001000000014c380ce44c37cb3d99d65e9cdcf0519a32f3e693c7e7b22ad97cf2dbec6aaed1000000008b483045022063a331ff7aeebbd06912e5ec4cdec6e89bb376b1a55523a9eb73eff9ff8bd91f022100a3a6c92593832057772d8b890fb7e6c0f3f21851ba8c3cdf2b8b30e1ebda97f3014104dad007c26b13ffa69e44ea32e7dce7781f95552e636cdd04024a55e5d0a9fed07f6b2fc2b9f8e689afb2d0aed790944bc390d38560bfb7178c16a73ff396f2adffffffff029d18a500000000001976a914b75a4b00d4df1e8358cc79f3d8d000d4806f46ee88ac9fc70600000000001976a914a31cc35833040aa1f8b09d69dcf0ae7359d6f4b788ac00000000");
		b1.parseTransactions ();
		assertTrue (b1.getHash ().equals ("0000000000000449ee5b94ba7a051caffff5c23d6a03335f6e20e3985b5ffa61"));
		Blk b2 = Blk.fromWireDump (b1.toWireDump ());
		b2.computeHash ();
		assertTrue (b1.getHash ().equals (b2.getHash ()));
	}

	@Test
	public void testTransaction () throws ValidationException
	{
		Tx t1 =
				Tx.fromWireDump ("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff1b02725236326635303332353334383266000000062f736c7573682f000000000100f2052a010000001976a914c685bbd80810f7bb1cb7faa2f0841d331fea481488ac00000000");
		assertTrue (t1.getHash ().equals ("2371beae0bf4f8fe0fa846973d179ad44839b1f4907e2adda43accbcb64184ae"));
		Tx t2 = Tx.fromWireDump (t1.toWireDump ());
		assertTrue (t1.getHash ().equals (t2.getHash ()));
	}
}
