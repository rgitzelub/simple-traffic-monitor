
*June 16, 2017*

Seems it's easy to forget what I've done if I put this down for awhile.

When I stopped last, I was having trouble with the app not stopping.  Turns out all the actors were still running, so duh.  But the `Terminator` doesn't seem to work, I had to shut down the actor system myself (this is just in `CountRandom`).

I tried to get logging working... frustratingly the Akka docs didn't seem to work, and I got complaints that the SLF jar wasn't there.  Eventually some Googling made it work. You need an Akka SLF4J lib, which is not a surprise, but's not in the Akka docs.

Okay, so it seems to be counting correctly, and I can build a tree of output, but... that's not what will be interesting.  I want the actors to tell me when there's a problem.

But how do that?  Every actor needs some way to communicate there's a problem.... ug, that's a lot of constructors to modify, and it seems messy. But it initially works.

But in this revision there's a lot of duplication, it feels like, and tweaking the numbers is awkward. Think I'll need to wrap the actor up, and not have it sent in directly.
