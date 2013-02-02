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
package com.bitsofproof.supernode.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Exchanger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class ClientBusAdaptor implements BCSAPI
{
	private static final Logger log = LoggerFactory.getLogger (ClientBusAdaptor.class);
	private Connection connection;
	private Session session;

	private String brokerURL;
	private String user;
	private String password;
	private String clientId;

	private final List<TransactionListener> transactionListener = Collections.synchronizedList (new ArrayList<TransactionListener> ());
	private final List<TrunkListener> trunkListener = Collections.synchronizedList (new ArrayList<TrunkListener> ());
	private final List<TemplateListener> blockTemplateListener = Collections.synchronizedList (new ArrayList<TemplateListener> ());
	private final Map<String, ArrayList<TransactionListener>> addressListener = Collections
			.synchronizedMap (new HashMap<String, ArrayList<TransactionListener>> ());

	private MessageProducer transactionProducer;
	private MessageProducer blockProducer;
	private MessageProducer blockRequestProducer;
	private MessageProducer transactionRequestProducer;
	private MessageProducer accountRequestProducer;

	public void setClientId (String clientId)
	{
		this.clientId = clientId;
	}

	public void setBrokerURL (String brokerUrl)
	{
		this.brokerURL = brokerUrl;
	}

	public void setUser (String user)
	{
		this.user = user;
	}

	public void setPassword (String password)
	{
		this.password = password;
	}

	private void addMessageListener (String topic, MessageListener listener) throws JMSException
	{
		Destination destination = session.createTopic (topic);
		MessageConsumer consumer = session.createConsumer (destination);
		consumer.setMessageListener (listener);
	}

	public void init ()
	{
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory (user, password, brokerURL);
		try
		{
			connection = connectionFactory.createConnection ();
			connection.setClientID (clientId);
			connection.start ();
			session = connection.createSession (false, Session.AUTO_ACKNOWLEDGE);
			addMessageListener ("transaction", new MessageListener ()
			{
				@Override
				public void onMessage (Message arg0)
				{
					BytesMessage o = (BytesMessage) arg0;
					for ( TransactionListener l : transactionListener )
					{
						try
						{
							byte[] body = new byte[(int) o.getBodyLength ()];
							o.readBytes (body);
							l.process (Transaction.fromProtobuf (BCSAPIMessage.Transaction.parseFrom (body)));
						}
						catch ( Exception e )
						{
							log.error ("Transaction message error", e);
						}
					}
				}
			});
			addMessageListener ("trunk", new MessageListener ()
			{
				@Override
				public void onMessage (Message arg0)
				{
					try
					{
						BytesMessage m = (BytesMessage) arg0;
						byte[] body = new byte[(int) m.getBodyLength ()];
						m.readBytes (body);
						TrunkUpdateMessage tu = TrunkUpdateMessage.fromProtobuf (BCSAPIMessage.TrunkUpdate.parseFrom (body));
						for ( TrunkListener l : trunkListener )
						{
							l.trunkUpdate (tu.getRemoved (), tu.getAdded ());
						}
					}
					catch ( Exception e )
					{
						log.error ("Block message error", e);
					}
				}
			});
			addMessageListener ("work", new MessageListener ()
			{
				@Override
				public void onMessage (Message arg0)
				{
					BytesMessage o = (BytesMessage) arg0;
					for ( TemplateListener l : blockTemplateListener )
					{
						try
						{
							byte[] body = new byte[(int) o.getBodyLength ()];
							o.readBytes (body);
							l.workOn (Block.fromProtobuf (BCSAPIMessage.Block.parseFrom (body)));
						}
						catch ( Exception e )
						{
							log.error ("Block message error", e);
						}
					}
				}
			});
			transactionProducer = session.createProducer (session.createTopic ("newTransaction"));
			blockProducer = session.createProducer (session.createTopic ("newBlock"));
			blockRequestProducer = session.createProducer (session.createTopic ("blockRequest"));
			transactionRequestProducer = session.createProducer (session.createTopic ("transactionRequest"));
			accountRequestProducer = session.createProducer (session.createTopic ("accountRequest"));
		}
		catch ( JMSException e )
		{
			log.error ("Can not create JMS connection", e);
		}

	}

	public void destroy ()
	{
		try
		{
			session.close ();
			connection.close ();
		}
		catch ( JMSException e )
		{
		}
	}

	@Override
	public void registerTransactionListener (TransactionListener listener)
	{
		transactionListener.add (listener);
	}

	@Override
	public void registerTrunkListener (TrunkListener listener)
	{
		trunkListener.add (listener);
	}

	@Override
	public void registerBlockTemplateListener (TemplateListener listener)
	{
		blockTemplateListener.add (listener);
	}

	private byte[] synchronousRequest (MessageProducer producer, Message m)
	{
		byte[] result = null;
		final Exchanger<byte[]> exchanger = new Exchanger<byte[]> ();

		TemporaryQueue answerQueue = null;
		MessageConsumer consumer = null;
		try
		{
			answerQueue = session.createTemporaryQueue ();
			m.setJMSReplyTo (answerQueue);
			consumer = session.createConsumer (answerQueue);
			consumer.setMessageListener (new MessageListener ()
			{
				@Override
				public void onMessage (Message message)
				{
					BytesMessage m = (BytesMessage) message;
					byte[] body;
					try
					{
						if ( m.getBodyLength () == 0 )
						{
							try
							{
								exchanger.exchange (null);
							}
							catch ( InterruptedException e )
							{
							}
						}
						else
						{
							body = new byte[(int) m.getBodyLength ()];
							m.readBytes (body);
							try
							{
								exchanger.exchange (body);
							}
							catch ( InterruptedException e )
							{
							}
						}
					}
					catch ( JMSException e )
					{
						log.trace ("Can not parse reply", e);
					}
				}
			});
			producer.send (m);
			try
			{
				result = exchanger.exchange (null);
			}
			catch ( InterruptedException e )
			{
			}
		}
		catch ( JMSException e )
		{
			log.error ("Can not send request", e);
		}
		finally
		{
			try
			{
				if ( consumer != null )
				{
					consumer.close ();
				}
				if ( answerQueue != null )
				{
					answerQueue.delete ();
				}
			}
			catch ( JMSException e )
			{
			}
		}
		return result;
	}

	@Override
	public Transaction getTransaction (String hash)
	{
		try
		{
			BytesMessage m = session.createBytesMessage ();
			BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder ();
			builder.setBcsapiversion (1);
			builder.addHash (ByteString.copyFrom (new Hash (hash).toByteArray ()));
			m.writeBytes (builder.build ().toByteArray ());
			byte[] response = synchronousRequest (transactionRequestProducer, m);
			if ( response != null )
			{
				Transaction t = Transaction.fromProtobuf (BCSAPIMessage.Transaction.parseFrom (response));
				t.computeHash ();
				return t;
			}
		}
		catch ( Exception e )
		{
			log.error ("Can not get transaction", e);
		}
		return null;
	}

	@Override
	public Block getBlock (String hash)
	{
		try
		{
			BytesMessage m = session.createBytesMessage ();
			BCSAPIMessage.Hash.Builder builder = BCSAPIMessage.Hash.newBuilder ();
			builder.setBcsapiversion (1);
			builder.addHash (ByteString.copyFrom (new Hash (hash).toByteArray ()));
			m.writeBytes (builder.build ().toByteArray ());
			byte[] response = synchronousRequest (blockRequestProducer, m);
			if ( response != null )
			{
				Block b = Block.fromProtobuf (BCSAPIMessage.Block.parseFrom (response));
				b.computeHash ();
				return b;
			}
		}
		catch ( Exception e )
		{
			log.error ("Can not get block", e);
		}
		return null;
	}

	@Override
	public AccountStatement registerAccountListener (List<String> addresses, long from, TransactionListener listener)
	{
		try
		{
			if ( listener != null )
			{
				for ( final String address : addresses )
				{
					ArrayList<TransactionListener> al = addressListener.get (address);
					if ( al == null )
					{
						al = new ArrayList<TransactionListener> ();
						addressListener.put (address, al);

						Destination blockDestination = session.createTopic ("address" + address);
						MessageConsumer blockConsumer = session.createConsumer (blockDestination);
						blockConsumer.setMessageListener (new MessageListener ()
						{
							@Override
							public void onMessage (Message arg0)
							{
								BytesMessage o = (BytesMessage) arg0;
								for ( TransactionListener l : addressListener.get (address) )
								{
									try
									{
										byte[] body = new byte[(int) o.getBodyLength ()];
										o.readBytes (body);
										l.process (Transaction.fromProtobuf (BCSAPIMessage.Transaction.parseFrom (body)));
									}
									catch ( Exception e )
									{
										log.error ("Transaction message error", e);
									}
								}
							}
						});
					}
					al.add (listener);
				}
			}

			BytesMessage m = session.createBytesMessage ();
			BCSAPIMessage.AccountRequest.Builder ab = BCSAPIMessage.AccountRequest.newBuilder ();
			ab.setBcsapiversion (1);
			for ( String a : addresses )
			{
				ab.addAddress (a);
			}
			ab.setFrom ((int) from);
			m.writeBytes (ab.build ().toByteArray ());
			byte[] response = synchronousRequest (accountRequestProducer, m);
			if ( response != null )
			{
				return AccountStatement.fromProtobuf (BCSAPIMessage.AccountStatement.parseFrom (response));
			}
		}
		catch ( Exception e )
		{
			log.error ("Can not register account", e);
		}
		return null;
	}

	@Override
	public void sendTransaction (Transaction transaction)
	{
		try
		{
			BytesMessage m = session.createBytesMessage ();
			m.writeBytes (transaction.toProtobuf ().toByteArray ());
			transactionProducer.send (m);
		}
		catch ( Exception e )
		{
			log.error ("Can not send transaction", e);
		}
	}

	@Override
	public void sendBlock (Block block)
	{
		try
		{
			BytesMessage m = session.createBytesMessage ();
			m.writeBytes (block.toProtobuf ().toByteArray ());
			blockProducer.send (m);
		}
		catch ( JMSException e )
		{
			log.error ("Can not send transaction", e);
		}
	}
}
