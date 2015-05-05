package org.traccar.geocode.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class AddressParts {
	@XmlElement(required = false)
	protected String street;
	@XmlElement(required = false)
	protected String road;
	@XmlElement(required = false)
	protected String pedestrian;
	@XmlElement(required = false)
	protected String city;
	@XmlElement(required = false)
	protected String town;
	@XmlElement(required = false)
	protected String house_number;

	public String getStreet() {
		return street;
	}

	public String getTown() {
		return town;
	}

	public String getRoad() {
		return road;
	}

	public String getPedestrian() {
		return pedestrian;
	}

	public String getCity() {
		return city;
	}

	public String getHouse_number() {
		return house_number;
	}

}
