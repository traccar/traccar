package org.traccar.geocode.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "reversegeocode")
public class ReverseGeocode {
	@XmlElement(required = false)
	protected AddressParts addressparts;

	public AddressParts getAddressparts() {
		return addressparts;
	}
	
	
}
