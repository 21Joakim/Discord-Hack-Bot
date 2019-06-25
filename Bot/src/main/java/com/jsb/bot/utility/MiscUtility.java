package com.jsb.bot.utility;

import java.util.List;

public class MiscUtility {

	public static <Type> String join(List<Type> list, String joinBy) {
		StringBuilder string = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Type object = list.get(i);
			
			string.append(object.toString());
			if (i != list.size() - 1) {
				string.append(joinBy);
			}
		}
		
		return string.toString();
	}
	
}
