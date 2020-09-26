## What is this?
A Discord moderation bot made for the Discord Hack Week 2019

## Invite
The prefix for this bot is `?`

~~https://discordapp.com/oauth2/authorize?scope=bot&permissions=8&client_id=592768736315047946~~
The bot still exists if you really wanted to invite it but it has been offline basically since the hack week concluded

## Website
~~https://moderation.jockie.tk~~
The website has also been offline for an equally as long time and I don't even have the domain anymore

# Bot

## Features

* Dashboard

* Most advanced logger out there
  * Have up to 5 loggers each in a different channel
  * Each logger can have their own events, but only a maximum of 1 of each type of event across all loggers to prevent abuse and spam
  * Disable/enable the events you want
  * Currently missing two events, `message edit` and `message delete`

* Mod logs
  * Ability to delete modlogs
  * Ability to change the reason for each log
  * Disable specific logs in the modlog channel
  
* Warning system
  * Actions when they reach `x` amount of warnings
  * Optional option to have the actions re-apply for every new warning until they reach a new action

* Template system
  * Have preset reasons so you can quickly specify a reason or just be lazy, like so `kick @Joakim t:spam` or `kick @Joakim template:spam`, you can also add additional content or even other templates `kick @Joakim Being a meanie, t:spam and t:tos` 

* Extensive prune commands
  * `prune images` to prune any images in the past 100 messages
  * `prune embeds` to prune any embeds in the past 100 messages
  * `prune user` to prune any messages from a specific user in the past 100 messages
  * `prune regex` to prune any messages that match the provided regex in the past 100 messages
  * `prune bots` to prune any bot messages in the past 100 messages
  
* Mute command with mute evasion actions
  * Available actions
    * `ban on leave` - If a user is muted and they leave they will be banned
    * `ban on join` - If a user leaves and then joins the server while being muted they will be banned, provided the mute hasn't ran out in that time
    * `warn on join` - If a user leaves and then joins the server while being muted they will be warned a set value, provided the mute hasn't ran out in that time
    * `remute on join` - If a user leaves and then joins the server while being muted they will be muted again, provided the mute hasn't ran out in that time
    * `kick on join` - If a user leaves and then joins the server while being muted they will be kicked, provided the mute hasn't ran out in that time

* Bot permissions
  * Give a user or role Discord permissions for the bot, this can be used to limit the permissions they have while still giving them the ability to moderate
  * Give a user or role full permissions to a command without needing to have the required Discord permissions for it

## Dependencies

* Java 9
* MongoDB

## Credits
[Bumbleboss](https://github.com/Bumbleboss) - Website
[Shea](https://github.com/Shea4) - Bot
[Joakim](https://github.com/21Joakim) - Bot
