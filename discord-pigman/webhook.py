import discord

webhooks_dict = dict()

# This is simple system for sending messages from webhooks
# which do look like user account. 

# webhooks_dict is:
# {
#
#   guild_id: {
#      channel_id: {
#        user_id:  webhook,
#        user_id2: webhook2,
#      }
#   },
#   guild2_id: ...
#
# }

# TODO:
# * big problem with this is: what if more than one plugin want to use our webhooks?
# * always keep max_webhooks-1 webhooks on server and remove least used webhook
#   account, so we need webhook send tracking
# * direct messages checks

def webhooks_loaded(guild, channel):
    guild_dict = webhooks_dict.get(guild.id)
    if guild_dict == None:
        return False
    # you can create channel after first loading so we need to check this
    if guild_dict.get(channel.id) == None:
        return False
    return True


async def webhook_send(guild, channel, user,
                       message=None, file=None, embed=None,
                       username=None, wait=True):
    if username == None:
        username = user.display_name
    if not webhooks_loaded(guild, channel):
        await load_webhooks(guild)

    webhook = webhooks_dict.get(guild.id).get(channel.id).get(str(user.id))
    if webhook == None:
        webhook = await webhook_create(guild, channel, user)
    try:
        message = await webhook.send(content=message, file=file, embed=embed,
                                     username=username, wait=wait)
    except discord.errors.NotFound:  # if for some reason webhook was removed
        webhook = await webhook_create(guild, channel, user)
        message = await webhook.send(content=message, file=file, embed=embed,
                                     username=username, wait=wait)

    return message


# TODO: webhook limit in current channel
async def webhook_create(guild, channel, user):
    avatar = await user.avatar_url_as().read()  # returns bytes object
    webhook = await channel.create_webhook(name=user.id, avatar=avatar)
    webhooks_dict[guild.id][channel.id][str(user.id)] = webhook
    return webhook


# TODO: guild and bot member for permission check
# maybe TODO: make it faster with tasks
async def load_webhooks(guild):
    webhooks_dict[guild.id] = dict()
    for channel in guild.text_channels:
        webhooks_dict[guild.id][channel.id] = dict()
        if not channel.permissions_for(guild.me).manage_webhooks:  # can we use anything else for this one?
            continue

        webhooks = await channel.webhooks()
        for webhook in webhooks:
            webhooks_dict[guild.id][channel.id][webhook.name] = webhook

