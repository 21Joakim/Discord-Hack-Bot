//to be replaced later using an api
let logJSON = {"data":{"types":[{"type":"MEMBER_JOIN"},{"type":"MEMBER_LEAVE"},{"type":"MEMBER_BAN"},{"type":"MEMBER_UNBAN"},{"type":"MEMBER_NICKNAME_CHANGE"},{"type":"MEMBER_ROLE_ADD"},{"type":"MEMBER_ROLE_REMOVE"},{"type":"ROLE_CREATE"},{"type":"ROLE_DELETE"},{"type":"ROLE_UPDATE_COLOR"},{"type":"ROLE_UPDATE_HOISTED"},{"type":"ROLE_UPDATE_MENTIONABLE"},{"type":"ROLE_UPDATE_NAME"},{"type":"ROLE_UPDATE_PERMISSIONS"},{"type":"EMOTE_CREATE"},{"type":"EMOTE_DELETE"},{"type":"EMOTE_UPDATE_NAME"},{"type":"EMOTE_UPDATE_ROLES"},{"type":"GUILD_UPDATE_AFK_CHANNEL"},{"type":"GUILD_UPDATE_AFK_TIMEOUT"},{"type":"GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL"},{"type":"GUILD_UPDATE_ICON"},{"type":"GUILD_UPDATE_MFA_LEVEL"},{"type":"GUILD_UPDATE_NAME"},{"type":"GUILD_UPDATE_NOTIFICATION_LEVEL"},{"type":"GUILD_UPDATE_OWNER"},{"type":"GUILD_UPDATE_REGION"},{"type":"GUILD_UPDATE_SPLASH"},{"type":"GUILD_UPDATE_SYSTEM_CHANNEL"},{"type":"GUILD_UPDATE_VERIFICATION_LEVEL"},{"type":"TEXT_CHANNEL_CREATE"},{"type":"TEXT_CHANNEL_DELETE"},{"type":"TEXT_CHANNEL_UPDATE_NAME"},{"type":"TEXT_CHANNEL_UPDATE_NSFW"},{"type":"TEXT_CHANNEL_UPDATE_PARENT"},{"type":"TEXT_CHANNEL_UPDATE_SLOWMODE"},{"type":"TEXT_CHANNEL_UPDATE_TOPIC"},{"type":"VOICE_CHANNEL_CREATE"},{"type":"VOICE_CHANNEL_DELETE"},{"type":"VOICE_CHANNEL_UPDATE_BITRATE"},{"type":"VOICE_CHANNEL_UPDATE_NAME"},{"type":"VOICE_CHANNEL_UPDATE_PARENT"},{"type":"VOICE_CHANNEL_UPDATE_USER_LIMIT"},{"type":"STORE_CHANNEL_CREATE"},{"type":"STORE_CHANNEL_DELETE"},{"type":"STORE_CHANNEL_UPDATE_NAME"},{"type":"CATEGORY_CHANNEL_CREATE"},{"type":"CATEGORY_CHANNEL_DELETE"},{"type":"CATEGORY_CHANNEL_UPDATE_NAME"},{"type":"VOICE_DEAFEN"},{"type":"VOICE_MUTE"},{"type":"VOICE_JOIN"},{"type":"VOICE_LEAVE"},{"type":"VOICE_MOVE"},{"type":"VOICE_SUPRESS"},{"type":"MESSAGE_"}],"loggers":[{"channel":{"name":"logger-1","id":593105103729328100},"enabled":true,"events":["MEMBER_NICKNAME_CHANGE"]},{"channel":{"name":"logger-2","id":593105129058861000},"enabled":false,"events":["MEMBER_JOIN","MEMBER_LEAVE","MEMBER_BAN","MEMBER_UNBAN","MEMBER_ROLE_ADD","MEMBER_ROLE_REMOVE","ROLE_CREATE","ROLE_DELETE","ROLE_UPDATE_COLOR","ROLE_UPDATE_HOISTED","ROLE_UPDATE_MENTIONABLE","ROLE_UPDATE_NAME","ROLE_UPDATE_PERMISSIONS","EMOTE_CREATE","EMOTE_DELETE","EMOTE_UPDATE_NAME","EMOTE_UPDATE_ROLES","GUILD_UPDATE_AFK_CHANNEL","GUILD_UPDATE_AFK_TIMEOUT","GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL","GUILD_UPDATE_ICON","GUILD_UPDATE_MFA_LEVEL","GUILD_UPDATE_NAME","GUILD_UPDATE_NOTIFICATION_LEVEL","GUILD_UPDATE_OWNER","GUILD_UPDATE_REGION","GUILD_UPDATE_SPLASH","GUILD_UPDATE_SYSTEM_CHANNEL","GUILD_UPDATE_VERIFICATION_LEVEL","TEXT_CHANNEL_CREATE","TEXT_CHANNEL_DELETE","TEXT_CHANNEL_UPDATE_NAME","TEXT_CHANNEL_UPDATE_NSFW","TEXT_CHANNEL_UPDATE_PARENT","TEXT_CHANNEL_UPDATE_SLOWMODE","TEXT_CHANNEL_UPDATE_TOPIC","VOICE_CHANNEL_CREATE","VOICE_CHANNEL_DELETE","VOICE_CHANNEL_UPDATE_BITRATE","VOICE_CHANNEL_UPDATE_NAME","VOICE_CHANNEL_UPDATE_PARENT","VOICE_CHANNEL_UPDATE_USER_LIMIT","STORE_CHANNEL_CREATE","STORE_CHANNEL_DELETE","STORE_CHANNEL_UPDATE_NAME","CATEGORY_CHANNEL_CREATE","CATEGORY_CHANNEL_DELETE","CATEGORY_CHANNEL_UPDATE_NAME","VOICE_DEAFEN","VOICE_MUTE","VOICE_JOIN","VOICE_LEAVE","VOICE_MOVE","VOICE_SUPRESS"]}]},"success":true};

createLoggers();
function createLoggers() {
  let text = {
    title: "My Loggers",
    paragraph: "Here you will be able to manage how the logging system works on your server.<br><br>Currently, the system offers having more than 1 channel from which the bot can log to. A few things to note is that you are only limited to <strong>5</strong> channels to log to and the same event cannot be used in a different channel."
  }
  
  let __manageContent = document.getElementById("manageContent");
  let __catholder = createDivAndClass("div", ["categoryHolder"], "logging");

  let __title = createElmWtext("h2", text.title, ["categoryTitle"])
  __title.style = "margin-top:0;"
  
  let __p = createElmWtext("p", text.paragraph, ["sectionDescription"]); 
  __p.style = "margin-bottom:40px;"

  if(!logJSON.data.loggers.length == 0) {
    let __placeholders = createPlaceholders(logJSON.data.loggers);
    appendChildren(__catholder, [__title, __p, __placeholders]);
  }else{
    appendChildren(__catholder, [__title, __p]);
  }

  __manageContent.appendChild(__catholder);
}

function createPlaceholders(x) {
  let __mplaceHolder = createDivAndClass("div", ["placeholder-main"]);
  let arr = [];
  for(let i = 0; i < x.length; i++) {
    let __placeholder = createDivAndClass("div", ["placeholder"]);
    let __placeholderTitle = createElmWtext("div", `channel<br><span>#${x[i].channel.name}</span>`, ["placeholderTitle"]);
    let __placeholderTitle2 = createElmWtext("div", `events<br><span>${getEvents(x[i].events)}</span>`, ["placeholderTitle"])
    let __placeholderBtn = createDivAndClass("div", ["placeholderBtn"]);
    let __btn = createElmWtext("button", "Edit", ["btn", "small", "white"]);

    __placeholderTitle2.style = "line-height: 20px;"

    let y = {}
    if(x[i].enabled) {
      y.clr = "red"
      y.txt = "Disable"
    }else{
      y.clr = "green"
      y.txt = "Enable"
    }


    let __btn2 = createElmWtext("button", y.txt, ["btn", "small", y.clr])
    __btn2.style = "margin-right:10px"

    appendChildren(__placeholderBtn, [__btn2, __btn]);
    appendChildren(__placeholder, [__placeholderTitle, __placeholderTitle2, __placeholderBtn]);
    arr.push(__placeholder)
  }
  appendChildren(__mplaceHolder, arr);
  return __mplaceHolder;
}

function getEvents(x) {
  let txt = '';
  if(x.length == 0) {
    txt = "None";
  }else{
    for(let i = 0; i < x.length; i++) {
      txt += `<code>${x[i]}</code> `
    }
  }
  return txt;
}