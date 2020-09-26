package com.jsb.bot.embed;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedAuthor;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedFooter;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedTitle;

public class WebhookEmbedBuilder {
	
	private final static String ZERO_WIDTH_SPACE = "\u200E";
	
	private final List<WebhookEmbed.EmbedField> fields;
	
	private final StringBuilder description;
	
	private OffsetDateTime timestamp;
	private Integer color;
	
	private String thumbnailUrl;
	private String imageUrl;
	
	private WebhookEmbed.EmbedFooter footer;
	private WebhookEmbed.EmbedTitle title;
	private WebhookEmbed.EmbedAuthor author;
	
	public WebhookEmbedBuilder() {
		this.fields = new ArrayList<>();
		this.description = new StringBuilder();
	}
	
	public void clear() {
		this.fields.clear();
		this.timestamp = null;
		this.color = null;
		this.description.setLength(0);
		this.thumbnailUrl = null;
		this.imageUrl = null;
		this.footer = null;
		this.title = null;
		this.author = null;
	}
	
	public WebhookEmbedBuilder setTimestamp(TemporalAccessor timestamp) {
		if(timestamp instanceof Instant) {
			this.timestamp = OffsetDateTime.ofInstant((Instant) timestamp, ZoneId.of("UTC"));
		}else{
			this.timestamp = timestamp == null ? null : OffsetDateTime.from(timestamp);
		}
		
		return this;
	}
	
	public WebhookEmbedBuilder setColor(Integer color) {
		this.color = color;
		
		return this;
	}
	
	public WebhookEmbedBuilder setColor(Color color) {
		this.color = color != null ? color.getRGB() : null;
		
		return this;
	}
	
	public WebhookEmbedBuilder setDescription(CharSequence description) {
        this.description.setLength(0);
        
        if(description != null && description.length() >= 1) {
        	this.appendDescription(description);
        }
		
		return this;
	}
	
	public WebhookEmbedBuilder appendDescription(CharSequence description) {
		this.description.append(description);
		
		return this;
	}
	
	public StringBuilder getDescriptionBuilder() {
		return this.description;
	}
	
	public WebhookEmbedBuilder setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
		
		return this;
	}
	
	public WebhookEmbedBuilder setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
		
		return this;
	}
	
	public WebhookEmbedBuilder setFooter(WebhookEmbed.EmbedFooter footer) {
		this.footer = footer;
		
		return this;
	}
	
	public WebhookEmbedBuilder setFooter(String footer, String icon) {
		return this.setFooter(new EmbedFooter(footer, icon));
	}
	
	public WebhookEmbedBuilder setFooter(String footer) {
		return this.setFooter(footer, null);
	}
	
	public WebhookEmbedBuilder setTitle(WebhookEmbed.EmbedTitle title) {
		this.title = title;
		
		return this;
	}
	
	public WebhookEmbedBuilder setTitle(String text, String url) {
		return this.setTitle(new EmbedTitle(text, url));
	}
	
	public WebhookEmbedBuilder setTitle(String text) {
		return this.setTitle(text, null);
	}
	
	public WebhookEmbedBuilder setAuthor(WebhookEmbed.EmbedAuthor author) {
		this.author = author;
		
		return this;
	}
	
	public WebhookEmbedBuilder setAuthor(String name, String iconUrl, String url) {
		return this.setAuthor(new EmbedAuthor(name, iconUrl, url));
	}
	
	public WebhookEmbedBuilder setAuthor(String name, String iconUrl) {
		return this.setAuthor(name, iconUrl, null);
	}
	
	public WebhookEmbedBuilder setAuthor(String name) {
		return this.setAuthor(name, null, null);
	}
	
	public WebhookEmbedBuilder addField(WebhookEmbed.EmbedField field) {
		if(this.fields.size() == WebhookEmbed.MAX_FIELDS) {
			throw new IllegalStateException("Cannot add more than 25 fields");
		}
		
		this.fields.add(Objects.requireNonNull(field));
		
		return this;
	}
	
	public WebhookEmbedBuilder addField(boolean inline, String name, String value) {
		return this.addField(new EmbedField(inline, name, value));
	}
	
	public WebhookEmbedBuilder addField(String name, String value, boolean inline) {
		return this.addField(inline, name, value);
	}
	
	public WebhookEmbedBuilder addEmptyField(boolean inline) {
		return this.addField(inline, WebhookEmbedBuilder.ZERO_WIDTH_SPACE, WebhookEmbedBuilder.ZERO_WIDTH_SPACE);
	}
	
	public boolean isEmpty() {
		return this.isEmpty(this.description.toString())
			&& this.isEmpty(this.imageUrl)
			&& this.isEmpty(this.thumbnailUrl)
			&& this.isFieldsEmpty()
			&& this.isAuthorEmpty()
			&& this.isTitleEmpty()
			&& this.isFooterEmpty()
			&& this.timestamp == null;
	}

	private boolean isEmpty(String string) {
		return string == null || string.trim().isEmpty();
	}

	private boolean isTitleEmpty() {
		return this.title == null || this.isEmpty(this.title.getText());
	}

	private boolean isFooterEmpty() {
		return this.footer == null || this.isEmpty(this.footer.getText());
	}

	private boolean isAuthorEmpty() {
		return this.author == null || this.isEmpty(this.author.getName());
	}

	private boolean isFieldsEmpty() {
		if(this.fields.isEmpty()) {
			return true;
		}
		
		return this.fields.stream().allMatch(field -> this.isEmpty(field.getName()) && this.isEmpty(field.getValue()));
	}
	
	public int length() {
        int length = 0;

        if(!this.isTitleEmpty()) {
            length += this.title.getText().length();
        }
        
        if(!this.isEmpty(this.description.toString())) {
            length += this.description.length();
        }
        
        if(!this.isAuthorEmpty()) {
            length += this.author.getName().length();
		}
        
        if(!this.isFooterEmpty()) {
            length += this.footer.getText().length();
        }
        
        if(!this.isFieldsEmpty()) {
            for(EmbedField field : this.fields) {
                length += field.getName().length() + field.getValue().length();
            }
        }

        return length;
    }
	
	public WebhookEmbed build() {
		if(this.isEmpty()) {
			throw new IllegalStateException("Cannot build an empty embed");
		}
		
		return new WebhookEmbed(
			this.timestamp, this.color,
			this.description.toString(), this.thumbnailUrl, this.imageUrl,
			this.footer, this.title, this.author,
			new ArrayList<>(this.fields)
		);
	}
}