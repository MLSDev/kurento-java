/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.rabbitmq;

import org.kurento.client.factory.KmfMediaApiProperties;
import org.kurento.commons.Address;
import org.kurento.rabbitmq.server.RabbitMqConnectorManager;

/**
 * Media Server for MultipleClientsAndServersTest.
 * 
 * @see {@link MultipleClientsAndServersTest}
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class MediaServer {

	private RabbitMqConnectorManager mediaServerBroker;
	private int num;

	public MediaServer(int num) {
		this.num = num;
	}

	public void start() {

		Address thriftKmfAddress = KmfMediaApiProperties.getThriftKmfAddress();
		thriftKmfAddress.setPort(thriftKmfAddress.getPort() + num);

		mediaServerBroker = new RabbitMqConnectorManager(
				KmfMediaApiProperties.getThriftKmsAddress(), thriftKmfAddress,
				KmfMediaApiProperties.getRabbitMqAddress());

	}

	public void destroy() {
		mediaServerBroker.destroy();
	}

}