/**
 * <h1>TiffTags.java</h1>
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version; or, at your choice, under the terms of the
 * Mozilla Public License, v. 2.0. SPDX GPL-3.0+ or MPL-2.0+.
 * </p>
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License and the Mozilla Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public License and the Mozilla Public License
 * along with this program. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a> and at <a
 * href="http://mozilla.org/MPL/2.0">http://mozilla.org/MPL/2.0</a> .
 * </p>
 * <p>
 * NB: for the © statement, include Easy Innova SL or other company/Person contributing the code.
 * </p>
 * <p>
 * © 2015 Easy Innova, SL
 * </p>
 *
 * @author Antonio Manuel Lopez Arjona
 * @version 1.0
 * @since 18/5/2015
 *
 */

package com.easyinnova.iptc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

/**
 * The Class TiffTags.
 */
public class IptcTags {

	/** The singleton instance. */
	private static IptcTags instance = null;

	/** The tag map. */
	public static HashMap<Integer, Tag> tagMap = new HashMap<Integer, Tag>();

	/** The tag names. */
	protected static HashMap<java.lang.String, Tag> tagKeys = new HashMap<java.lang.String, Tag>();

	/** The tag types. */
	public static HashMap<Integer, java.lang.String> tagTypes = new HashMap<Integer, java.lang.String>();

	/**
	 * Instantiates a new tiff tags.
	 */
	protected IptcTags() {
		File folder = new File("./config/iptc/");
		Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy").create();

		for (final File fileEntry : folder.listFiles()) {
			try {
				BufferedReader br = new BufferedReader(  
						new FileReader(fileEntry.toPath().toString()));

				Tag tag = gson.fromJson(br, Tag.class); 

				tagMap.put(tag.getDecimal(), tag);
				tagKeys.put(tag.getKey(), tag);
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}  
		}

		tagTypes.put(1, "SHORT");
		tagTypes.put(2, "STRING");
		tagTypes.put(3, "DATE");
		tagTypes.put(4, "TIME");
		tagTypes.put(5, "UNDEFINED");
	}

	/**
	 * Gets the tiff tags.
	 *
	 * @return the singleton instance
	 */
	public static synchronized IptcTags getIptcTags() {
		if (instance == null) {
			instance = new IptcTags();
		}
		return instance;
	}

	/**
	 * Gets tag information.
	 *
	 * @param identifier Tag id
	 * @return the tag or null if the identifier does not exist
	 */
	public static Tag getTag(int identifier) {
		Tag t = null;
		if (instance == null)
			getIptcTags();
		if (tagMap.containsKey(identifier))
			t = tagMap.get(identifier);
		return t;
	}

	/**
	 * Gets the tag id.
	 *
	 * @param name the name
	 * @return the tag id
	 */
	public static int getTagId(String name) {
		int id = -1;
		if (instance == null)
			getIptcTags();
		if (tagKeys.containsKey(name))
			id = tagKeys.get(name).getDecimal();
		return id;
	}

	/**
	 * Checks for tag.
	 *
	 * @param id the id
	 * @return true, if successful
	 */
	public static boolean hasTag(int id) {
		if (instance == null)
			getIptcTags();
		return tagMap.containsKey(id);
	}
}
