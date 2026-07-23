# PlayerCrasherPlus

> for that one player who won't stop flying, won't stop reaching, won't stop being a little freak about it

*In the great tradition of the Bastard Operator From Hell: the user is not always right, but they are always powerless.*

Kicking is for cowards. Kicking says "please reconnect in 3 seconds and keep hacking, champ." Kicking is a suggestion. This is not a suggestion.

`/crash <player> [sizeMB]` reaches directly into that player's network connection and hands their client a present. The present is garbage. A LOT of garbage. Their client opens it expecting a normal little packet (couple KB, nothing to see here, very normal, very legal) and instead gets buried alive.

## how

We don't ask Minecraft nicely to send a big packet, because Minecraft will not do that, because Minecraft has opinions about packet sizes and so do the cheat clients built to survive them. So we lie.

We hand-write the frame ourselves: a tiny, boring, totally-a-normal-packet-I-swear length header, then just... keep writing bytes. Way more bytes than we said we would. The client reads the lie, slices off what it thinks is "the packet," and the rest of the flood is still sitting there in the pipe wearing a trench coat, about to become several thousand more "packets" that were never supposed to exist. Decode desync o'clock. Their parser doesn't recover from that. It wasn't built to.

We then skip past Minecraft's own packet machinery entirely (encoder, compressor, the length-stamping guy who would've ruined the lie) and post it straight to the raw socket via Netty's `unsafe()`. Nobody in the pipeline gets a vote.

If the target somehow, against all odds, shrugs it off (respect, but no) - we wait a second, check if they're still breathing, and if so, boot them the old-fashioned way and tell you it took two tries. No cheater escapes both barrels.

## config.yml

Yes it's configurable. Yes, you can make the fake header even more insultingly small. Yes, you can change the default blast radius. Self-documenting, because future-you deserves comments, not archaeology.

## disclaimer (the fun kind)

This is a blunt instrument for your own server, against people you already caught cheating, because a normal kick felt insufficient to the crime. It is not a DDoS tool, not for other people's servers, not for randoms, not for main character syndrome. Point it at cheaters. Enjoy responsibly. Or irresponsibly. We're not your parents.

built with love, deployed with malice.

official excuse if anyone asks what happened to their connection: "the packet was too heavy for the network, we're not sure why."
