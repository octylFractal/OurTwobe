Data layout in Firestore:

- /profiles/{uid}
    - UserProfile data
- /guilds/{gid}
    - Guild data (here GuildData), mostly in sub-collections:
    - /channels/{cid}
        - List of voice channels in the guild (VoiceChannelData)
        - /users/{uid}
            - Set of users in the voice channel
    - /users/{uid}
        - Set of users in the guild
            - /queues/{random-id}
            - User's queue of songs (QueueEntry list)

Extra types:
- Sets are essentially maps, the key points to a document `{"exists": true, "id": "the_id"}` and nothing else.
