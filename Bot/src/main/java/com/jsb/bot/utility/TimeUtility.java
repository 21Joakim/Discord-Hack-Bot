package com.jsb.bot.utility;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class TimeUtility {
	
	private TimeUtility() {}

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
					
					seconds += TimeUtility.getCorrectSeconds(number, word.substring(index));
				}
			}
		}
		
		if (seconds <= 0) {
			throw new IllegalArgumentException("Your time cannot be negative");
		}
		
		return seconds;
	}
	
	public static String secondsToTimeString(long time) {
		ChronoUnit[] units = {ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.WEEKS, ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS};
		StringBuilder timeString = new StringBuilder();
		if (time > 0) {
			for (int i = 0; i < units.length; i++) {
				ChronoUnit unit = units[i];
				
				long value = time / unit.getDuration().toSeconds();
				time = time % unit.getDuration().toSeconds();
				
				if (value > 0) {
					String name = unit.toString().toLowerCase();
					timeString.append(value + " " + name.substring(0, (value == 1 ? name.length() - 1 : name.length())) + " ");
				}
			}
			
			return timeString.toString().trim();
		} else {
			return time + " " + units[units.length - 1].toString().toLowerCase();
		}
	}
}