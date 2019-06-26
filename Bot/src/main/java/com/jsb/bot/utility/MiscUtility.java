package com.jsb.bot.utility;

import java.util.Collection;
import java.util.List;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;

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
	
	public static int getWebhookEmbedLength(WebhookEmbed embed) {
		int length = 0;
		
		String title = embed.getTitle() != null ? embed.getTitle().getText().trim() : null;
		if (title != null) {
			length += title.length();
		}
		
		String description = embed.getDescription() != null ? embed.getDescription().trim() : null;
		if (description != null) {
			length += description.length();
		}
		
		String author = embed.getAuthor() != null ? embed.getAuthor().getName().trim() : null;
		if (author != null) {
			length += author.length();
		}
		
		String footer = embed.getFooter() != null ? embed.getFooter().getText().trim() : null;
		if (footer != null) {
			length += footer.length();
		}
		
		if (embed.getFields() != null) {
			for (EmbedField field : embed.getFields()) {
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
}
