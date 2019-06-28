# Commands

## Prune

### Description

Command to prune messages, ability to prune by different types of messages will also be available. The command that have filters would be able to be used with template filters, like `template:invites` or `t:invites`.

### Meta

Required bot permission: `MANAGE_MESSAGE`

Required author permission: `MANAGE_MESSAGE`

### Variations

Maybe add the ability to prune for multiple users at once, unsure of how to add this. Also unsure about the `--regex` option, I am not sure if options are very user friendly and they should probably just be used as a last resort!

- `prune <user> [filter]` Remove all messages sent by the specified user, with a optional filter and option `--regex` to use regex filter
- `prune <filter>` Remove all messages matching the filter, with an option `--regex` to use regex filter
- `prune images` Removes all messages that have images
- `prune embeds` Removes all messages that have embeds
- `prune bots` Removes all messages that were sent by bots, with a `bc` alias

## Warn

### Description

The warn command would be used to warn users about their wrong doings, this would send a log in a `mog-logs` channel if it has been set. If the user has x amount of warnings a specified action may be set.

### Meta

Required author permissions to use `warn`: `MANAGE_MESSAGE`

Required author permissions to configure: `MANAGE_SERVER` (May need elevated permissions for specific configurations?)

### Configuration

- Setting action for `x` amount of warnings
  - `kick`
  - `ban`
  - `mute` 
  - `give role`
- When setting the action you should also be able to set which roles the action applies to
- When setting the action you should also be able to set how many warnings a specific role needs for that action, if you want to increase it for specific roles

### Data format

May need a better name for `noAppliesTo`. The reason there is both `appliesTo` and `noAppliesTo` is so you can make it not apply to some roles without having to specifically say to apply to all other roles. We may also choose to allow it for specific users and for that we may 

- Have it in the same array and check for both users and roles
- Change the format of it to specify the type of what it applies to
- Have a separate array for it

```JSON
{
    "actions": [
        {
            "action": "KICK",
            "warnings": 3,
            "appliesTo": [
                "{role.id}"
            ],
            "noAppliesTo": [
                "{role.id}"
            ],
            "exceptions": [
                {
                    "id": "{role.id}",
                    "warnings": 5
                }
            ]
        }
    ]
}
```

## Template

### Description

Templates would be used to create pre-defined reasons for moderation actions or values for things like filters. Templates would be specified using `template:{template_name}` or `t:{template_name}` for short.

To use the template for commands, for instance, for the `ban` command you could do something like this `ban @Joakim template:spam & template:tos` which would result in `Reason: Spamming & Violating Discord's ToS`

### Meta

Required author permissions: `MANAGE_SERVER`

### Variations

- `template reason add <template name> <template>`
- `template reason remove <template>`
- `template prune add <template name> <template>`
- `template prune remove <template>`

## Mute

### Description

Mute would be used to mute a specific user, with an optional reason. This would send a log in a `mog-logs` channel if it has been set.

### Meta

Required author permissions to use `mute`: `MANAGE_MESSAGES`

### Configuration

- Being able to use a custom role for mute, if you were to have multiple bots it may be annoying to have multiple different mute roles
- Set the action for mute evasion
  - Ban on leave
  - Ban on join (if time hasn't expired)
  - Re-mute on join (if time hasn't expired)
  - Warn on join (if time hasn't expired)

### Data format

Should we allow for custom action depending on the role, similar to the warn actions?

```JSON
{
    "role": "{role.id}",
    "action": {
        "type": "WARN",
        "value": 3,
        "message": "Mute evasion"
    }
}
```

or

```JSON
{
    "role": "{role.id}",
    "action": {
        "type": "WARN",
        "value": 3,
        "message": "Mute evasion",
        "appliesTo": [
            "{role.id}"
        ],
        "noAppliesTo": [
            "{role.id}"
        ],
        "exceptions": [
            {
                "id": "{role.id}",
                "type": "KICK"
            }
        ]
    }
}
```

## Logger

### Description

The logger commands would be used to customise the logger, which logs specific events in the server to a specific channel.

### Meta

Required author permissions: `MANAGE_SERVER`

Required bot permissions: `MANAGE_WEBHOOKS`

### Configuration

- Ability to change the channel it logs in
- Ability to specify what logs should be logged
- Ability to have specific logs go in specific channels

### Events

- Member
  - `MEMBER_KICK`
  - `MEMBER_BAN`
  - `MEMBER_NICK_CHANGE`
  - `MEMBER_JOIN`
  - `MEMBER_LEAVE`
- Message
  - `MESSAGE_DELETE`
  - `MESSAGE_EDIT`
- Channel
  - `CHANNEL_NAME_CHANGE`
  - `CHANNEL_CREATE`
  - `CHANNEL_DELETE`
  - `CHANNEL_PERMISSION_CHANGE` (Unsure if we can do this because JDA gay)
  - `CHANNEL_INVITE_CREATED`
  - `CHANNEL_WEBHOOK_CREATED`
- Role
  - `ROLE_NAME_CHANGE`
  - `ROLE_CREATE`
  - `ROLE_DELETE`
  - `ROLE_PERMISSION_CHANGE`
- Server
  - `SERVER_ICON_CHANGE`
  - `SERVER_NAME_CHANGE`
  - `SERVER_VOICE_REGION_CHANGE`
  - `SERVER_VERIFCATION_CHANGE`
  - `SERVER_EXPLICIT_FILTER_CHANGE`
  - `SERVER_WIDGET_CHANGE`
- Emote
  - `EMOTE_CREATE`
  - `EMOTE_DELETE`

### Data format

The `mode` property would determine whether or not events specified in `events` should be enabled or disabled, if `mode` is equal to `false` then all events are enabled and anything specified in `events` is disabled and if `mode` is equal to `true` then everything would be disabled and anything specified in `events` would be enabled

```JSON
{
    "enabled": true,
    "loggers": [
        {
            "enabled": true,
            "channel": "{channel.id}",
            "webhookId": "{webhook.id}",
            "webhookToken": "{webhook.token}",
            "mode": true,
            "events": [
                "MEMBER_LEFT",
                {
                    "type": "MESSAGE_DELETE",
                    "channels": [
                        "{channel.id}"
                    ]
                }
            ]
        }
    ]
}
```

## Filter

### Description

Filters would be used to filter specific messages by regex.

### Meta

Required author permissions: `MANAGE_MESSAGES` or `MANAGE_SERVER`

Required bot permissions: `MANAGE_MESSAGES`