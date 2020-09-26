package com.jsb.bot.utility;

import java.util.Collection;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;

public class MiscUtility {
	
	private MiscUtility() {}

	public static <Type> String join(Collection<Type> list, String joinBy) {
		StringBuilder string = new StringBuilder();
		
		int index = 0;
		for(Type object : list) {
			string.append(object.toString());
			
			if(index != list.size() - 1) {
				string.append(joinBy);
			}
			
			index++;
		}
		
		return string.toString();
	}
	
	public static int getWebhookEmbedLength(WebhookEmbed embed) {
		int length = 0;
		
		String title = embed.getTitle() != null ? embed.getTitle().getText().trim() : null;
		if(title != null) {
			length += title.length();
		}
		
		String description = embed.getDescription() != null ? embed.getDescription().trim() : null;
		if(description != null) {
			length += description.length();
		}
		
		String author = embed.getAuthor() != null ? embed.getAuthor().getName().trim() : null;
		if(author != null) {
			length += author.length();
		}
		
		String footer = embed.getFooter() != null ? embed.getFooter().getText().trim() : null;
		if(footer != null) {
			length += footer.length();
		}
		
		if(embed.getFields() != null) {
			for(EmbedField field : embed.getFields()) {
				length += field.getName().trim().length() + field.getValue().trim().length();
			}
		}
		
		return length;
	}
	
	public static int getWebhookEmbedLength(Collection<WebhookEmbed> embeds) {
		return embeds.stream()
			.mapToInt(embed -> MiscUtility.getWebhookEmbedLength(embed))
			.sum();
	}
	
	public static boolean isNumber(String string) {
	    char[] characterArray = string.toCharArray();
	    for(int i = 0; i < characterArray.length; i++) {
	        char character = characterArray[i];
	        
	        if(i == 0 && (character == '-' || character == '+')) {
	            continue;
	        }
	        
	        if(!Character.isDigit(character)) {
	            return false;
	        }
	    }
	    
	    return true;
	}
	
	public static boolean isWord(String string) {
	    char[] characterArray = string.toCharArray();
	    for(int i = 0; i < characterArray.length; i++) {
	        if(!Character.isLetter(characterArray[i])) {
	            return false;
	        }
	    }
	    
	    return true;
	}
}