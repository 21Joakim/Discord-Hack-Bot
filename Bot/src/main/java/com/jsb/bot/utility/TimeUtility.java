package com.jsb.bot.utility;

import java.util.List;

public class TimeUtility {

	private static final List<String> dayAliases = List.of("day", "d", "days");
	private static final List<String> hourAliases = List.of("hour", "h", "hours");
	private static final List<String> minuteAliases = List.of("minute", "m", "minutes", "min", "mins");
	private static final List<String> secondAliases = List.of("second", "s", "seconds", "sec", "secs");
	
	private static long getCorrectSeconds(long number, String suffix) {
		long seconds = 0L;
		if (dayAliases.contains(suffix)) {
			seconds += number * 86400;
		} else if (hourAliases.contains(suffix)) {
			seconds += number * 3600;
		} else if (minuteAliases.contains(suffix)) {
			seconds += number * 60;
		} else if (secondAliases.contains(suffix)) {
			seconds += number;
		} else {
			throw new IllegalArgumentException("Incorrect time format, a good example would be `1d 5h 20m 36s`");
		}
		
		return seconds;
	}
	
	public static long timeStringToSeconds(String time) {
		String[] timeSplit = time.split(" ");
		Long lastNumber = null;
		long seconds = 0L;
		for (String word : timeSplit) {
			if (MiscUtility.isNumber(word)) {
				if (lastNumber == null) {
					lastNumber = Long.parseLong(word);
				} else {
					throw new IllegalArgumentException("Incorrect time format, a good example would be `1d 5h 20m 36s`");
				}
			} else if (MiscUtility.isWord(word)) {
				word = word.toLowerCase();
				if (lastNumber == null) {
					throw new IllegalArgumentException("Incorrect time format, a good example would be `1d 5h 20m 36s`");
				} else {
					seconds += TimeUtility.getCorrectSeconds(lastNumber, word);
					
					lastNumber = null;
				}
			} else {
				word = word.toLowerCase();
				char[] characters = word.toCharArray();
				int index = 0;
				for (int i = 0; i < characters.length; i++) {
					char character = characters[i];
					if (!Character.isDigit(character)) {
						index = i;
						break;
					}
				}
				
				if (index == 0) {
					throw new IllegalArgumentException("Incorrect time format, a good example would be `1d 5h 20m 36s`");
				} else {
					long number = Long.parseLong(word.substring(0, index));
					String timeSuffix = word.substring(index);
					seconds += TimeUtility.getCorrectSeconds(number, timeSuffix);
				}
			}
		}
		
		if (seconds <= 0) {
			throw new IllegalArgumentException("Your time cannot be negative");
		}
		
		return seconds;
	}
	
}