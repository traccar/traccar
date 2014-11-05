/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.geocode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.traccar.geocode.jaxb.AddressParts;
import org.traccar.geocode.jaxb.ReverseGeocode;
import org.traccar.helper.Log;
import org.w3c.dom.Document;

public class NominatimReverseGeocoder implements ReverseGeocoder {

	private final String url;

	public NominatimReverseGeocoder(String url) {
		this.url = url + "?format=xml&lat=%f&lon=%f&zoom=18&addressdetails=1";
	}

	@Override
	public String getAddress(double latitude, double longitude) {

		try {
			String urlstr = String.format(Locale.US, url, latitude, longitude);
//			System.out.println("URL:" + urlstr);
			URLConnection conn = new URL(urlstr).openConnection();

//			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//			String line = null;
//			while ((line = br.readLine()) != null) {
//				System.out.println(line);
//			}

			JAXBContext context = JAXBContext.newInstance(ReverseGeocode.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			ReverseGeocode geocode = (ReverseGeocode) unmarshaller.unmarshal(conn.getInputStream());
			StringBuilder sb = new StringBuilder();
			AddressParts ap = geocode.getAddressparts();
			if (ap != null) {
				if (ap.getCity() != null) {
					sb.append(ap.getCity());
				} else if (ap.getTown() != null) {
					sb.append(ap.getTown());
				}

				sb.append(" ");
				if (ap.getRoad() != null) {
					sb.append(ap.getRoad());
				} else if (ap.getPedestrian() != null) {
					sb.append(ap.getPedestrian());
				} else if (ap.getStreet() != null) {
					sb.append(ap.getStreet());
				}

				sb.append(" ");
				if (ap.getHouse_number() != null) {
					sb.append(ap.getHouse_number());
				}

			}
			return sb.toString();

		} catch (Exception error) {
			Log.warning(error);
		}

		return null;
	}

}
