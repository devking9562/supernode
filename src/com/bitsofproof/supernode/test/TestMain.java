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
package com.bitsofproof.supernode.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bitsofproof.supernode.main.Application;

public class TestMain
{
	private static final Logger log = LoggerFactory.getLogger (TestMain.class);

	public static void main (String[] args)
	{
		try
		{
			log.trace ("Spring context setup");
			ApplicationContext context = new ClassPathXmlApplicationContext ("testassembly.xml");
			Application application = context.getBean (Application.class);
			application.start (context, args);
		}
		catch ( Exception e )
		{
			log.error ("Application", e);
		}
	}
}
