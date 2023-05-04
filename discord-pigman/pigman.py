import asyncio
import json
import time
import os
import getpass
import subprocess
import shutil
import string
import random
import socket
import logging
import sys

from typing import Optional

import discord
from redbot.core import Config, checks, commands
from redbot.core.bot import Red
from redbot.core.commands import Cog
from redbot.core.utils.chat_formatting import pagify
from redbot.core.utils.predicates import MessagePredicate

from .buffer import dprint, buffempty, dsend
from .webhook import webhook_send

logger = logging.getLogger("red")


def dirformat(obj):
    dirlist = []
    for name in dir(obj):
        attr_type = str(type(getattr(obj, name)))
        attr_type = attr_type.split("'")[1]

        dirlist.append(f'`{name}  -  {attr_type}`')

    return '\n'.join(dirlist)


def codeformat(text):
    return f'`{text}`'

def identifier(length):
    return ''.join([ random.choice(string.digits + string.ascii_lowercase)
                                                    for x in range(length) ])

def parse_args_to_addresses_usernames(args):
    addresses, usernames = [], []
    for arg in args:
        if is_valid_ip_address(arg):
            addresses.append(arg)
        else:
            usernames.append(arg)
    return addresses, usernames


# TODO: 555.555.555.555 is invalid but we could assume user wanted to get ip,
# and that is not his username
def is_valid_ip_address(string):
    ip = string.split('.')
    if len(ip) != 4:
        return False
    for num in ip:
        if not num.isnumeric():
            return False
        if not 0 <= int(num) <= 255:
            return False
    return True


async def readlined(reader):
    return (await reader.readline()).decode()[:-1]

async def paper_command(command, timeout=5):
    fut = paper_command_for_wrapper(command)
    try:
        return await asyncio.wait_for(fut, timeout=timeout)
    except asyncio.TimeoutError:
        logger.info("TIMEOUT")
        return False


# TODO: server is down situation
async def paper_command_for_wrapper(command):
    if command[-1] != '\n':
        command += '\n'
    reader, writer = await asyncio.open_connection('localhost', 7551)
    writer.write(command.encode())
    await writer.drain()
    line = await readlined(reader) 
    success, comment = line.split(maxsplit=1)
    writer.close()
    await writer.wait_closed()
    return success == 'success:'


class pigman(Cog):
    """
    pigman utils
    """

    def __init__(self, red: Red):
        super().__init__()
        self.bot = red
        home = self.home = os.environ['HOME']

        # TODO: adding yourself to whitelist gives you minecraft role

        # TODO:
        # how minecraft role and channel are called, 
        # whats id of them and ability to change name
        # (or name doesn't matter since we have ID?)
        # and setting them up or creating when first use of cog
        self.config = Config.get_conf(self, identifier=5372189531)
        self.config.register_global(whitelist={}, usernames={})
        # whitelist - {user_id: [['username', ], ['1.1.1.1', ]]}
        self.config.register_guild(minecraft_channel=None, logs_channel=None, minecraft_role=None)

        # TODO: allow for modifying it
        self.http_dir = '/srv/www/page-name/pages/verify'
        self.logfile = '/var/log/page-name/access.log'
        self.pigman_location = os.path.dirname(os.path.realpath(__file__))
        self.whitelist_rsync = False

        if self.bot.is_ready():  # this doesn't happen during restart REEEEEEEEEEEEEEEEEEE
            asyncio.create_task(self.load())


    async def load(self):
        logger.info("loaded pigman")
        logger.info("========================================")
        # you can't hardcode channels and roles here, you have to add it into config
        # every server owner probably should be able to register server to pigman
        # and then pigman should be able to manage all of those servers
        # but managing multiple servers seems really hard to implement
        # to set it up to work with multiple minecraft servers and multiple discord server,
        # lots of thinking and configuration, and planning, and designing would be neccessary, *probably*
        # Maybe it seems like that because I haven't thought about how to implement this yet

        self.dev_guild = dev_guild = self.bot.get_guild(962664255189041183)
        self.debug_channel = dev_guild.get_channel(1077547793507438663)

        self.dev_guild_channels = { channel.id:channel for channel in await dev_guild.fetch_channels() }

        logs_channel_id = await self.config.guild(dev_guild).logs_channel()
        self.logs_channel = self.dev_guild_channels.get(logs_channel_id)
        minecraft_channel_id = await self.config.guild(dev_guild).minecraft_channel()
        self.minecraft_channel = self.dev_guild_channels.get(minecraft_channel_id)

        self.minecraft_role = None

        # we should really iterate over already set up servers here
        # and send no-config comunicate when someone uses command to setup another guild or something
        logger.info(f"{minecraft_channel_id} {logs_channel_id}")
        if self.logs_channel == None or self.minecraft_channel == None:
            # todo: send dm to a server owner?
            logger.info("you have to setup logs channel and minecraft channel properly in order to continue")
            return
        await self.logs_channel.send('loading...')
        self.chat_bridge_task = asyncio.create_task(self._chat_bridge())

        webhooks = { w.name:w for w in await self.minecraft_channel.webhooks() }
        if 'general' not in webhooks:
            webhook = await self.minecraft_channel.create_webhook(name='general')
        else:
            webhook = webhooks['general']
        self.general_webhook = webhook


    # Do we really want this dumb code here?
    # TODO: remove webhooks accounts registered here together with removing username?
    # guild only
    async def _chat_bridge(self):
        # TODO: ConnectionRefusedError
        while True:
            try:
                try:
                    reader, writer = await asyncio.open_connection('localhost', 7551)
                    logger.info("connected to localhost 7551")
                except ConnectionRefusedError:
                    asyncio.sleep(5)
                    continue
            except asyncio.CancelledError:
                loger.info('Cancelled!!')
                return

            writer.write(b'chat\n')
            await writer.drain()
            line = await readlined(reader)
            await self.logs_channel.send(line)
            try:
                # TODO: shutdown this properly
                dc_forward_task = asyncio.create_task(self._discord_forwarder(writer))
                await self._minecraft_forwarder(reader)
            except asyncio.CancelledError:
                logger.info('cancelled - shutting down connection')
            except Exception as e:
                logger.info('you have error in your forwarder')
                logger.info(e)
            finally:
                writer.close()
                await writer.wait_closed()


    async def _discord_forwarder(self, writer):
        pred = MessagePredicate.same_context(channel=self.minecraft_channel)
        while True:
            message = await self.bot.wait_for("message", check=pred)
            if message.author.bot:
                continue
            writer.write(f'[discord] <{message.author.display_name}> {message.content}\n'.encode())
            await writer.drain()


    async def _minecraft_forwarder(self, reader):
        while True:
            line = await readlined(reader)
            if line[0] == '/':
                asyncio.create_task(self._minecraft_command(line.split('\t')))
            else:
                asyncio.create_task(self._forward_minecraft_message(line))


    async def _forward_minecraft_message(self, line):
        username, message = line.split(maxsplit=1)
        user = self.bot.get_user(await self._get_user_id(username))
        if user == None:
            await self.general_webhook.send(message, username=username)
            return
        # username = f'{user.display_name} ({username})'  XXX: do we want this?
        await webhook_send(self.dev_guild, self.minecraft_channel, user, message=message, username=username)


    async def _minecraft_command(self, args):
        if args[0] == '/cords':
            username, x, y, z, message = args[1:]
            cords = f'`X:{x}, Y:{y}, Z:{z}`: {message}' if message != 'None' else f'`X:{x}, Y:{y}, Z:{z}`'
            user = self.bot.get_user(await self._get_user_id(username))
            if user == None:
                await self.general_webhook.send(cords, username=username)
                return
            await webhook_send(self.dev_guild, self.minecraft_channel, user, message=cords, username=username)
            return
        if args[0] == '/event':
            if args[1] == 'join':
                username = args[2]
                await self.minecraft_channel.send(f'*user **{username}** connected to the server*')
            if args[1] == 'quit':
                username = args[2]
                await self.minecraft_channel.send(f'*user **{username}** disconnected from the server*')


    # whitelist - {user_id: [['username', ], ['1.1.1.1', ]]}
    async def _get_usernames_and_addresses(self, user_id):
        return ( await self.config.whitelist() ).get(str(user_id), ([], []) )


    async def _get_user_id(self, nickname):
        return ( await self.config.usernames() ).get(nickname)


    async def _add_user_username(self, username, user_id):
        logger.info(f'adding {username}:{user_id} of type {type(username)}:{type(user_id)}')
        async with self.config.usernames() as usernames:
            usernames[username] = user_id


    async def _add_user_whitelist(self, user_id, usernames_to_add, addresses_to_add):
        async with self.config.whitelist() as whitelist:
            usernames, addresses = whitelist.get(str(user_id), ([], []) )
            addresses_to_add = [ x for x in addresses_to_add if x not in addresses ]
            usernames_to_add = [ x for x in usernames_to_add if x not in usernames ]
            usernames += usernames_to_add
            addresses += addresses_to_add
            whitelist[str(user_id)] = usernames, addresses
        return usernames_to_add, addresses_to_add


    async def _del_username(self, username):
        async with self.config.usernames() as usernames:
            usernames.pop(username)


    async def _del_whitelist(self, user_id, usernames_to_del, addresses_to_del):
        async with self.config.whitelist() as whitelist:
            usernames, addresses = whitelist.get(str(user_id), ([], []) )
            usernames_removed = [ x for x in usernames_to_del if x in usernames ]
            addresses_removed = [ x for x in addresses_to_del if x in addresses ]
            usernames = [ x for x in usernames if x not in usernames_removed ]
            addresses = [ x for x in addresses if x not in addresses_removed ]
            whitelist[str(user_id)] = usernames, addresses
        return usernames_removed, addresses_removed


    async def _generate_whitelist_file(self, filename):
        whitelist = await self.config.whitelist()
        with open(filename, 'w') as f:
            for usernames, addresses in whitelist.values():
                for username in usernames:
                    f.write(username + ' ' + ' '.join(addresses) + '\n')
        if self.whitelist_rsync:  # TODO: file on different server
            pass


    # TODO: make this async and put timeout here
    domain_addr = 'temp.com'
    async def _http_get_ip(self, member):
        TIMEOUT = 15
        giga_timer = 0
        user_ip = None

        file_id = identifier(8)
        src_file = f'{self.pigman_location}/index.html'
        dst_file = f'{self.http_dir}/{file_id}.html'
        shutil.copyfile(src_file, dst_file)
        with open(self.logfile, 'r') as f:
            while f.readline():
                pass
            await member.send(f'https://{domain_addr}/verify/{file_id}')
            while giga_timer < TIMEOUT:
                line = f.readline()
                if not line:
                    await asyncio.sleep(0.1)
                    giga_timer += 0.1
                    continue
                # if this isn't enough to identify we can wait for 'get pigman.png' too
                if file_id in line and 'Discordbot' not in line:  
                    user_ip = line.split()[0]
                    break
        os.remove(dst_file)
        return user_ip


    @commands.group(name='ip')
    async def ip(self, ctx):
        """Whitelist management"""
        pass

    # todo: adding usernames only on server ?
    # block using link generation in this command twice at once
    # what username could broke our java whitelist plugin?
    @ip.command(name='add')
    async def add(self, ctx, generate_link: Optional[bool], *args):
        """Add your ip and nickname to the whitelist

        ip add generates link for you if you don't specify ip as argument
        unless no is used as first argument
        usage:
        &ip add Username1
        &ip add username2 username3
        &ip add Username1 9.9.9.9
        &ip add 9.8.9.9 12.4.5.6 username2
        &ip add no username2"""

        addresses, usernames = parse_args_to_addresses_usernames(args)
        for username in usernames.copy():
            user_id = await self._get_user_id(username)
            if user_id != None:
                # XXX: block adding too much usernames (of config to add max)
                await ctx.author.send(f'username `{username}` is already registered') 
                usernames = [ x for x in usernames if x != username ]
                continue
            await self._add_user_username(username, ctx.author.id)

        # do link generation if argument not overwritten and address was not specified
        if generate_link == None and len(addresses) == 0 or generate_link:
            # TODO: check if we're not at dm already and first usage
            await ctx.send("In order to register your IP to whitelist, you have to open link I've sent you on DM")
            gen_ip = await self._http_get_ip(ctx.author) 
            if gen_ip == None:
                await ctx.send('ip verification error: timeout')
            else:
                addresses.append(gen_ip)

        usernames, addresses = await self._add_user_whitelist(ctx.author.id, usernames, addresses)
        if usernames:
            await ctx.author.send(f"added usernames: `{' '.join(usernames)}`")
        if addresses:
            await ctx.author.send(f"added addresses: `{' '.join(addresses)}`")
        if not usernames and not addresses:
            await ctx.author.send("nothing got added, "
                                  "data you wanted to register "
                                  "might already be registered")
        await self._generate_whitelist_file('/srv/papermc/ip_whitelist.ipw')
        if not await paper_command("reload"):
            await ctx.send("error; TODO: log this somewhere or send to myself")


    # TODO: only at dm?
    @ip.command(name='del')
    async def delete(self, ctx, *args):
        """Delete your ip and nickname from the whitelist"""
        addresses, usernames = parse_args_to_addresses_usernames(args)

        # TODO: check context and call user dumb if addresses not empty and not at dm

        for username in usernames:
            user_id = await self._get_user_id(username)
            if user_id == None:
                await ctx.author.send(f'{username} username is not registered')
                continue
            if user_id != ctx.author.id:
                await ctx.author.send(f"you're not the person who registered {username} username!")
                continue
            await self._del_username(username)

        usernames, addresses = await self._del_whitelist(ctx.author.id, usernames, addresses)
        if usernames:
            await ctx.author.send(f"removed usernames: `{' '.join(usernames)}`")
        if addresses:
            await ctx.author.send(f"removed addresses: `{' '.join(addresses)}`")
        if not usernames and not addresses:
            await ctx.author.send(f"nothing got removed, you're sure you used valid data?")
        await self._generate_whitelist_file('/srv/papermc/ip_whitelist.ipw')
        if not await paper_command("reload"):
            await ctx.send("error; TODO: log this somewhere or send to myself")


    @ip.command(name='list')
    async def list(self, ctx):
        """List the content of a whitelist list"""
        usernames, addresses = await self._get_usernames_and_addresses(ctx.author.id)
        await ctx.author.send(f"your usernames: `{' '.join(usernames) if usernames else ' EMPTY '}`")
        await ctx.author.send(f"your IPs: `{' '.join(addresses) if addresses else ' EMPTY '}`")


    @commands.admin()
    @commands.group(name='d')
    async def dev(self, ctx):
        """Misc commands useful for development"""
        pass


    @dev.command(name='set')
    async def d_set(self, ctx, channel_type, channel: discord.TextChannel):
        """available types are minecraft and logs"""
        if channel_type == 'minecraft':
            await self.config.guild(ctx.guild).minecraft_channel.set(channel.id)
            await ctx.send('changed minecraft channel to ' + channel.mention)
        elif channel_type == 'logs':
            await self.config.guild(ctx.guild).logs_channel.set(channel.id)
            await ctx.send('changed logs channel to ' + channel.mention)
        else:
            await ctx.send('not such type')


    @dev.command(name='buffempty')
    async def buffempty(self, ctx):
        await dsend(ctx.channel)


    @dev.command(name='thing')
    async def thing(self, ctx):
        """does the thing"""
        dprint(dirformat(ctx.channel))
        await dsend(ctx.channel)

    # TODO: check wait_for_red, wait_until_red_ready, initialize functions

    # if I remember correctly, logger doesnt work anymore at this point
    # but cog_unload happens cause there's /tmp/unload created
    def cog_unload(self):
        with open('/tmp/unload', 'w') as f:
            f.write('aaa')
        logger.log('unloading..')
        self.bot.loop.create_task(self.session.close())
        self.chat_bridge_task.cancel()
