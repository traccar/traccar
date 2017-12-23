// Copyright (c) Philipp Wagner. All rights reserved.

// Licensed under the MIT license. See LICENSE file in the project root for full license information.


package org.traccar.fcm.core.requests.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.requests.builders.NotificationPayloadBuilder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPayload {
    @JsonProperty("title")
    private String title;
    @JsonProperty("body")
    private String body;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("sound")
    private String sound;
    @JsonProperty("badge")
    private String badge;
    @JsonProperty("tag")
    private String tag;
    @JsonProperty("color")
    private String color;
    @JsonProperty("clickAction")
    private String clickAction;
    @JsonProperty("bodyLocKey")
    private String bodyLocKey;
    @JsonProperty("bodyLocKeyArgs")
    private List<String> bodyLocKeyArgs;
    @JsonProperty("titleLocKey")
    private String titleLocKey;
    @JsonProperty("titleLocKeyArgs")
    private List<String> titleLocKeyArgs;
    @JsonProperty("androidChannelId")
    private String androidChannelId;

    public static NotificationPayloadBuilder builder() {
        return new NotificationPayloadBuilder();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getClickAction() {
        return clickAction;
    }

    public void setClickAction(String clickAction) {
        this.clickAction = clickAction;
    }

    public String getBodyLocKey() {
        return bodyLocKey;
    }

    public void setBodyLocKey(String bodyLocKey) {
        this.bodyLocKey = bodyLocKey;
    }

    public List<String> getBodyLocKeyArgs() {
        return bodyLocKeyArgs;
    }

    public void setBodyLocKeyArgs(List<String> bodyLocKeyArgs) {
        this.bodyLocKeyArgs = bodyLocKeyArgs;
    }

    public String getTitleLocKey() {
        return titleLocKey;
    }

    public void setTitleLocKey(String titleLocKey) {
        this.titleLocKey = titleLocKey;
    }

    public List<String> getTitleLocKeyArgs() {
        return titleLocKeyArgs;
    }

    public void setTitleLocKeyArgs(List<String> titleLocKeyArgs) {
        this.titleLocKeyArgs = titleLocKeyArgs;
    }

    public String getAndroidChannelId() {
        return androidChannelId;
    }

    public void setAndroidChannelId(String androidChannelId) {
        this.androidChannelId = androidChannelId;
    }
}